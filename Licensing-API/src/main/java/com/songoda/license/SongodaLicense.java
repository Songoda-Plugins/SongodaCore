package com.songoda.license;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Standalone, shadeable Songoda license client. No lifecycle or trial policy is installed.
 * Synchronous calls block; use verifyAsync from plugin startup and schedule Bukkit work
 * back onto the server thread. Trust keys are obtained only from Songoda over HTTPS.
 */
public final class SongodaLicense {
    /**
     * Marketplace-injected license UUID. Never overridden by local files.
     */
    public static final String LICENSE;
    /**
     * Marketplace-injected opaque licensing user ID.
     */
    public static final String USER_ID;
    /**
     * Marketplace-injected opaque licensing product ID.
     */
    public static final String PRODUCT_ID;

    private static final URI VERIFY_URI = URI.create("https://songoda-reborn.com/api/license/verify");
    private static final URI KEY_URI = URI.create("https://songoda-reborn.com/api/license/key");
    private static final String CACHE_FILE = ".license";
    private static final int MAX_NETWORK_ATTEMPTS = 3;
    private static final int MAX_DOCUMENT_LENGTH = 65_536;
    private static final Duration KEY_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration REFRESH_THRESHOLD = Duration.ofHours(1);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    private static volatile TrustedKey trustedKey;

    static {
        LICENSE = "%%__LICENSE__%%";
        USER_ID = "%%__USER_ID__%%";
        PRODUCT_ID = "%%__PRODUCT_ID__%%";
    }

    private SongodaLicense() {
    }

    /**
     * Blocking verification. Injected marketplace identity is used in published downloads.
     */
    public static Result verify(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return verify(plugin, plugin.getPluginMeta().getName());
    }

    /**
     * Blocking verification with the product UUID shown for local development builds.
     */
    public static Result verify(Plugin plugin, String productId) {
        Objects.requireNonNull(plugin, "plugin");
        return check(plugin.getDataFolder(), productId, plugin.getPluginMeta().getVersion());
    }

    /**
     * Nonblocking verification. Completion callbacks do not run on the server thread.
     */
    public static CompletableFuture<Result> verifyAsync(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return verifyAsync(plugin, plugin.getPluginMeta().getName());
    }

    /**
     * Async verification with the product UUID shown for local development builds.
     */
    public static CompletableFuture<Result> verifyAsync(Plugin plugin, String productId) {
        Objects.requireNonNull(plugin, "plugin");
        File folder = plugin.getDataFolder();
        String version = plugin.getPluginMeta().getVersion();
        return CompletableFuture.supplyAsync(() -> check(folder, productId, version));
    }

    /**
     * Async verification on a caller-owned executor, with metadata captured on the calling thread.
     */
    public static CompletableFuture<Result> verifyAsync(Plugin plugin, String productId, Executor executor) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(executor, "executor");
        File folder = plugin.getDataFolder();
        String version = plugin.getPluginMeta().getVersion();
        return CompletableFuture.supplyAsync(() -> check(folder, productId, version), executor);
    }

    /**
     * Blocking convenience check. A valid result may be a trial; consumers must enforce its limits.
     */
    public static boolean isValid(Plugin plugin) {
        return verify(plugin).isValid();
    }

    /**
     * Blocking verification without a plugin instance. Injected identity takes precedence.
     * Local builds read a creator-issued credential from dataFolder/product.key.
     * No local key, environment variable, or JVM property changes the trust authority.
     */
    public static Result check(File dataFolder, String productId, String pluginVersion) {
        Objects.requireNonNull(dataFolder, "dataFolder");
        String product = requireUuid(isInjected(PRODUCT_ID) ? PRODUCT_ID : productId);
        String licenseId;
        String userId;
        try {
            licenseId = resolveLicenseId(dataFolder);
            userId = isInjected(USER_ID) ? requireUuid(USER_ID) : null;
            if (isInjected(LICENSE) && userId == null) {
                return failure(Status.INVALID, "Incomplete marketplace identity", licenseId, product);
            }
            requireUuid(licenseId);
        } catch (IOException | IllegalArgumentException exception) {
            return failure(Status.INVALID, "Missing or invalid license credential", null, product);
        }

        TrustedKey key;
        try {
            key = productionKey();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failure(Status.NETWORK_ERROR, "License verification interrupted", licenseId, product);
        } catch (Exception exception) {
            return failure(Status.NETWORK_ERROR, "Could not obtain Songoda verification key", licenseId, product);
        }

        Result offline = null;
        String cached = readCache(dataFolder.toPath().resolve(CACHE_FILE));
        if (cached != null) {
            offline = verifyToken(cached, product, licenseId, userId, !isInjected(LICENSE), key, true);
            if (offline.isValid()) {
                Duration threshold = offline.isDevelopment() ? Duration.ofSeconds(60) : REFRESH_THRESHOLD;
                if (offline.tokenExpiresAt().isAfter(Instant.now().plus(threshold))) {
                    return offline;
                }
            }
        }
        Result online = verifyOnlineWithRetries(licenseId, product, pluginVersion, dataFolder, userId, key);
        if (online.isNetworkError() && offline != null && offline.isValid() && isCurrent(offline, Instant.now())) {
            return offline;
        }
        if (!online.isValid() && !online.isNetworkError()) {
            try {
                Files.deleteIfExists(dataFolder.toPath().resolve(CACHE_FILE));
            } catch (IOException ignored) {
                // The rejected result remains authoritative for this call.
            }
        }
        return online;
    }

    private static String resolveLicenseId(File folder) throws IOException {
        if (isInjected(LICENSE)) {
            return LICENSE;
        }
        Path file = folder.toPath().resolve("product.key");
        if (!Files.isRegularFile(file) || Files.size(file) > 256) {
            throw new IOException("Missing license credential");
        }
        return Files.readString(file, StandardCharsets.UTF_8).trim();
    }

    private static String requireUuid(String value) {
        if (value == null || !UUID.fromString(value).toString().equals(value)) {
            throw new IllegalArgumentException("Invalid UUID identity");
        }
        return value;
    }

    private static boolean isInjected(String value) {
        return value != null && !value.isBlank() && !value.startsWith("%%__");
    }

    private static Result verifyOnlineWithRetries(String licenseId, String product, String version, File folder, String userId, TrustedKey key) {
        Result last = failure(Status.NETWORK_ERROR, "License server unavailable", licenseId, product);
        for (int attempt = 0; attempt < MAX_NETWORK_ATTEMPTS; attempt++) {
            if (Thread.currentThread().isInterrupted()) {
                return failure(Status.NETWORK_ERROR, "License verification interrupted", licenseId, product);
            }
            last = verifyOnline(licenseId, product, version, folder, userId, key);
            if (!last.isNetworkError() || Thread.currentThread().isInterrupted()) {
                return last;
            }
            if (attempt + 1 < MAX_NETWORK_ATTEMPTS) {
                try {
                    Thread.sleep(1_000L << attempt);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return failure(Status.NETWORK_ERROR, "License verification interrupted", licenseId, product);
                }
            }
        }
        return last;
    }

    private static Result verifyOnline(String licenseId, String product, String version, File folder, String userId, TrustedKey key) {
        try {
            String body = "{\"license_id\":" + Json.quote(licenseId) + ",\"product_id\":" + Json.quote(product) + ",\"plugin_version\":" + Json.quote(version == null ? "" : version) + "}";
            HttpRequest request = HttpRequest.newBuilder(VERIFY_URI).timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json").header("User-Agent", "SongodaLicense/2.0.0").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status >= 400 && status < 500 && status != 408 && status != 429) {
                return failure(Status.REJECTED, "Songoda rejected license verification (HTTP " + status + ")", licenseId, product);
            }
            if (status != 200) {
                return failure(Status.NETWORK_ERROR, "License server returned HTTP " + status, licenseId, product);
            }
            String token = string(Json.object(response.body()), "token");
            if (!key.id().equals(string(jwtPart(token, 0), "kid"))) {
                key = refreshProductionKey();
            }
            Result result = verifyToken(token, product, licenseId, userId, !isInjected(LICENSE), key, false);
            return result.isValid() ? cacheResult(folder.toPath().resolve(CACHE_FILE), token, result) : result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failure(Status.NETWORK_ERROR, "License verification interrupted", licenseId, product);
        } catch (IOException exception) {
            return failure(Status.NETWORK_ERROR, "Could not contact Songoda", licenseId, product);
        } catch (Exception exception) {
            return failure(Status.INVALID, "Invalid Songoda verification response", licenseId, product);
        }
    }

    private static synchronized TrustedKey productionKey() throws Exception {
        if (trustedKey != null && trustedKey.fetchedAt().plus(KEY_CACHE_TTL).isAfter(Instant.now())) {
            return trustedKey;
        }
        try {
            return refreshProductionKey();
        } catch (IOException exception) {
            if (trustedKey != null) {
                // A key previously authenticated over HTTPS remains usable during an outage.
                return trustedKey;
            }
            throw exception;
        }
    }

    private static synchronized TrustedKey refreshProductionKey() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(KEY_URI).timeout(Duration.ofSeconds(15)).GET().build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("Verification key unavailable");
        }
        Map<String, Object> json = Json.object(response.body());
        String pem = string(json, "publicKeyPem");
        String encoded = pem.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
        ECPublicKey key = (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(encoded)));
        if (key.getParams().getOrder().bitLength() != 256) {
            throw new IllegalArgumentException("Expected P-256 key");
        }
        trustedKey = new TrustedKey(string(json, "keyId"), key, Instant.now());
        return trustedKey;
    }

    static Result verifyToken(String token, String product, String licenseId, String userId, boolean allowDevelopment, TrustedKey key, boolean fromCache) {
        try {
            Map<String, Object> header = jwtPart(token, 0);
            if (!"ES256".equals(string(header, "alg")) || !"JWT".equals(string(header, "typ")) || !key.id().equals(string(header, "kid")) || header.containsKey("crit")) {
                throw new IllegalArgumentException("Invalid JWT header");
            }
            String[] parts = token.split("\\.", -1);
            byte[] signature = Base64.getUrlDecoder().decode(parts[2]);
            if (signature.length != 64) {
                throw new IllegalArgumentException("Invalid ES256 signature length");
            }
            Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
            verifier.initVerify(key.key());
            verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            if (!verifier.verify(signature)) {
                throw new IllegalArgumentException("Invalid signature");
            }
            Map<String, Object> claims = jwtPart(token, 1);
            Object audience = claims.get("aud");
            boolean expectedAudience = "songoda-core".equals(audience) || List.of("songoda-core").equals(audience);
            if (number(claims, "ver") != 3 || !"songoda-auth".equals(string(claims, "iss")) || !expectedAudience) {
                throw new IllegalArgumentException("Invalid token authority");
            }
            String actualLicense = string(claims, "license_id");
            requireUuid(actualLicense);
            String actualUser = requireUuid(string(claims, "user_id"));
            String actualProduct = requireUuid(string(claims, "product_id"));
            if (!licenseId.equals(actualLicense) || !product.equals(actualProduct) || (userId != null && !userId.equals(actualUser))) {
                throw new IllegalArgumentException("Token identity mismatch");
            }
            Instant now = Instant.now();
            Instant issued = Instant.ofEpochSecond(number(claims, "iat"));
            Instant expires = Instant.ofEpochSecond(number(claims, "exp"));
            Instant licenseExpires = claims.get("license_expires_at") == null ? null : Instant.ofEpochSecond(number(claims, "license_expires_at"));
            if (issued.isAfter(now.plusSeconds(60)) || !expires.isAfter(issued) || (claims.containsKey("nbf") && Instant.ofEpochSecond(number(claims, "nbf")).isAfter(now))) {
                throw new IllegalArgumentException("Invalid token dates");
            }
            if (!expires.isAfter(now) || (licenseExpires != null && !licenseExpires.isAfter(now))) {
                return failure(Status.EXPIRED, "License or verification token expired", licenseId, product);
            }
            Status status = switch (string(claims, "license_type")) {
                case "PURCHASE", "SUBSCRIPTION" -> Status.LICENSED;
                case "TRIAL" -> Status.TRIAL;
                case "DEVELOPMENT" -> {
                    if (!allowDevelopment || licenseExpires == null || expires.isAfter(issued.plusSeconds(300))) {
                        throw new IllegalArgumentException("Development license not permitted");
                    }
                    yield Status.DEVELOPMENT;
                }
                default -> throw new IllegalArgumentException("Unknown license type");
            };
            int maxPlayers = Math.toIntExact(number(claims, "max_players"));
            int maxIps = Math.toIntExact(number(claims, "max_ips"));
            if (maxPlayers < 0 || maxIps < 1 || maxIps > 32 || (status == Status.TRIAL && (maxPlayers < 1 || licenseExpires == null))) {
                throw new IllegalArgumentException("Invalid license limits");
            }
            return new Result(status, "Verified by Songoda", licenseId, product, maxPlayers, maxIps, licenseExpires, expires, fromCache);
        } catch (Exception exception) {
            return failure(Status.INVALID, "Invalid license token", licenseId, product);
        }
    }

    private static Map<String, Object> jwtPart(String token, int part) {
        if (token == null || token.length() > MAX_DOCUMENT_LENGTH) {
            throw new IllegalArgumentException("Invalid token size");
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || !token.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Malformed JWT");
        }
        try {
            String json = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(Base64.getUrlDecoder().decode(parts[part]))).toString();
            return Json.object(json);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Malformed JWT payload", exception);
        }
    }

    private static String string(Map<String, Object> json, String field) {
        if (json.get(field) instanceof String value && !value.isBlank()) {
            return value;
        }
        throw new IllegalArgumentException("Missing string: " + field);
    }

    private static long number(Map<String, Object> json, String field) {
        if (json.get(field) instanceof BigDecimal value) {
            return value.longValueExact();
        }
        throw new IllegalArgumentException("Missing number: " + field);
    }

    static boolean isCurrent(Result result, Instant now) {
        return result.tokenExpiresAt() != null && result.tokenExpiresAt().isAfter(now) && (result.licenseExpiresAt() == null || result.licenseExpiresAt().isAfter(now));
    }

    private static String readCache(Path file) {
        try {
            if (Files.isRegularFile(file) && Files.size(file) <= MAX_DOCUMENT_LENGTH) {
                return Files.readString(file, StandardCharsets.UTF_8).lines().findFirst().orElse(null);
            }
        } catch (IOException ignored) {
            // An unreadable cache must trigger online verification.
        }
        return null;
    }

    static Result cacheResult(Path file, String token, Result result) {
        Path temporary = null;
        try {
            Path parent = file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, ".license-", ".tmp");
            Files.writeString(temporary, token, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return result;
        } catch (IOException exception) {
            return new Result(result.status(), result.message() + " (local cache unavailable)", result.licenseId(), result.productId(), result.maxPlayers(), result.maxIps(), result.licenseExpiresAt(), result.tokenExpiresAt(), result.fromCache());
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Cache cleanup must not change a verified grant.
                }
            }
        }
    }

    private static Result failure(Status status, String message, String licenseId, String product) {
        return new Result(status, message, licenseId, product, 0, 0, null, null, false);
    }

    /**
     * Verification outcome. Only signed server grants are valid.
     */
    public enum Status {
        LICENSED, TRIAL, DEVELOPMENT, EXPIRED, INVALID, REJECTED, NETWORK_ERROR
    }

    /**
     * Immutable verified grant. Callers enforce player limits and recheck expiration.
     */
    public record Result(Status status, String message, String licenseId, String productId, int maxPlayers, int maxIps,
                         Instant licenseExpiresAt, Instant tokenExpiresAt, boolean fromCache) {
        public Result {
            Objects.requireNonNull(status, "status");
            message = message == null ? "" : message;
        }

        public boolean isValid() {
            return status == Status.LICENSED || status == Status.TRIAL || status == Status.DEVELOPMENT;
        }

        public boolean isTrial() {
            return status == Status.TRIAL;
        }

        public boolean isDevelopment() {
            return status == Status.DEVELOPMENT;
        }

        public boolean isNetworkError() {
            return status == Status.NETWORK_ERROR;
        }

        /**
         * Returns the signed player limit; zero means unlimited only for a valid result.
         */
        public int effectiveMaxPlayers() {
            return maxPlayers;
        }

        @Override
        public String toString() {
            return "SongodaLicense.Result{status=" + status + ", message='" + message + "'}";
        }
    }

    record TrustedKey(String id, ECPublicKey key, Instant fetchedAt) {
    }

    /**
     * Strict JSON reader kept here so the API remains copyable as one source file.
     */
    static final class Json {
        private final String source;
        private int position;

        private Json(String source) {
            if (source == null || source.length() > MAX_DOCUMENT_LENGTH) {
                throw new IllegalArgumentException("Invalid JSON size");
            }
            this.source = source;
        }

        static Map<String, Object> object(String source) {
            Json parser = new Json(source);
            Object value = parser.value(0);
            parser.whitespace();
            if (!(value instanceof Map<?, ?> map) || parser.position != source.length()) {
                throw new IllegalArgumentException("Expected JSON object");
            }
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put((String) key, item));
            return result;
        }

        private Object value(int depth) {
            whitespace();
            if (depth > 16 || position >= source.length()) {
                throw new IllegalArgumentException("Invalid JSON nesting");
            }
            return switch (source.charAt(position)) {
                case '{' -> objectValue(depth + 1);
                case '[' -> arrayValue(depth + 1);
                case '"' -> text();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> numeric();
            };
        }

        private Map<String, Object> objectValue(int depth) {
            expect('{');
            Map<String, Object> fields = new LinkedHashMap<>();
            if (take('}')) {
                return fields;
            }
            do {
                whitespace();
                String key = text();
                expect(':');
                if (fields.containsKey(key)) {
                    throw new IllegalArgumentException("Duplicate JSON field");
                }
                fields.put(key, value(depth));
            } while (take(','));
            expect('}');
            return fields;
        }

        private List<Object> arrayValue(int depth) {
            expect('[');
            List<Object> items = new ArrayList<>();
            if (take(']')) {
                return items;
            }
            do {
                items.add(value(depth));
            } while (take(','));
            expect(']');
            return items;
        }

        private String text() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (position < source.length()) {
                char next = source.charAt(position++);
                if (next == '"') {
                    return result.toString();
                }
                if (next < 32) {
                    throw new IllegalArgumentException("Unescaped JSON control character");
                }
                if (next == '\\') {
                    if (position >= source.length()) {
                        throw new IllegalArgumentException("Incomplete JSON escape");
                    }
                    next = source.charAt(position++);
                    next = switch (next) {
                        case '"', '\\', '/' -> next;
                        case 'b' -> '\b';
                        case 'f' -> '\f';
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        case 'u' -> unicode();
                        default -> throw new IllegalArgumentException("Invalid JSON escape");
                    };
                }
                result.append(next);
            }
            throw new IllegalArgumentException("Unterminated JSON string");
        }

        private char unicode() {
            if (position + 4 > source.length()) {
                throw new IllegalArgumentException("Incomplete Unicode escape");
            }
            String hex = source.substring(position, position + 4);
            if (!hex.matches("[0-9a-fA-F]{4}")) {
                throw new IllegalArgumentException("Invalid Unicode escape");
            }
            position += 4;
            return (char) Integer.parseInt(hex, 16);
        }

        private Object literal(String text, Object value) {
            if (!source.startsWith(text, position)) {
                throw new IllegalArgumentException("Invalid JSON literal");
            }
            position += text.length();
            return value;
        }

        private BigDecimal numeric() {
            int start = position;
            while (position < source.length() && "-+0123456789.eE".indexOf(source.charAt(position)) >= 0) {
                position++;
            }
            String number = source.substring(start, position);
            if (!number.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")) {
                throw new IllegalArgumentException("Invalid JSON number");
            }
            return new BigDecimal(number);
        }

        private boolean take(char expected) {
            whitespace();
            if (position < source.length() && source.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!take(expected)) {
                throw new IllegalArgumentException("Unexpected JSON character");
            }
        }

        private void whitespace() {
            while (position < source.length() && " \t\r\n".indexOf(source.charAt(position)) >= 0) {
                position++;
            }
        }

        static String quote(String value) {
            StringBuilder result = new StringBuilder("\"");
            for (char character : value.toCharArray()) {
                switch (character) {
                    case '"' -> result.append("\\\"");
                    case '\\' -> result.append("\\\\");
                    default -> {
                        if (character < 32) {
                            result.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                        } else {
                            result.append(character);
                        }
                    }
                }
            }
            return result.append('"').toString();
        }
    }
}
