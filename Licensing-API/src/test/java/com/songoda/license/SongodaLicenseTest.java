package com.songoda.license;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SongodaLicenseTest {
    private static final String LICENSE = "1469707f-65d5-461c-b5a6-f4cb8c6a44d1";
    private static final String USER_ID = "6b80ea2a-f0a6-44ae-a10a-c96b5b3017cc";
    private static final String PRODUCT_ID = "dff3b170-11a5-41c6-90a4-c89bc394b7c7";
    private KeyPair pair;
    private SongodaLicense.TrustedKey key;
    @TempDir Path folder;

    @BeforeEach
    void keys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        pair = generator.generateKeyPair();
        key = new SongodaLicense.TrustedKey("test-key", (ECPublicKey) pair.getPublic(), Instant.now());
    }

    @Test
    void verifiesSignedIdentityAndUsesTokenExpirySeparately() throws Exception {
        SongodaLicense.Result result = verify(sign(claims()));
        assertTrue(result.isValid());
        assertEquals(SongodaLicense.Status.LICENSED, result.status());
        assertTrue(result.licenseExpiresAt().isAfter(result.tokenExpiresAt()));
        assertFalse(SongodaLicense.isCurrent(result, result.tokenExpiresAt()));
        assertTrue(SongodaLicense.isCurrent(result, result.tokenExpiresAt().minusSeconds(1)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ver", "iss", "aud", "iat", "exp", "license_id", "product_id", "user_id", "license_type", "max_players", "max_ips"})
    void rejectsMissingRequiredClaims(String field) throws Exception {
        Map<String, String> claims = claims();
        claims.remove(field);
        assertFalse(verify(sign(claims)).isValid(), field);
    }

    @Test
    void rejectsOtherUsersProductsAndLicenses() throws Exception {
        String token = sign(claims());
        assertFalse(SongodaLicense.verifyToken(token, UUID.randomUUID().toString(), LICENSE, USER_ID, true, key, false).isValid());
        assertFalse(SongodaLicense.verifyToken(token, PRODUCT_ID, UUID.randomUUID().toString(), USER_ID, true, key, false).isValid());
        assertFalse(SongodaLicense.verifyToken(token, PRODUCT_ID, LICENSE, UUID.randomUUID().toString(), true, key, false).isValid());
        Map<String, String> claims = claims();
        claims.put("product_id", "\"*\"");
        assertFalse(verify(sign(claims)).isValid());
    }

    @Test
    void refusesLocallySignedAndTamperedTokens() throws Exception {
        String token = sign(claims());
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        SongodaLicense.TrustedKey unrelated = new SongodaLicense.TrustedKey("test-key", (ECPublicKey) generator.generateKeyPair().getPublic(), Instant.now());
        assertFalse(SongodaLicense.verifyToken(token, PRODUCT_ID, LICENSE, USER_ID, true, unrelated, false).isValid());
        String[] parts = token.split("\\.");
        parts[1] = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"license_type\":\"DEVELOPMENT\"}".getBytes(StandardCharsets.UTF_8));
        assertFalse(verify(String.join(".", parts)).isValid());
        assertFalse(verify(token + ".extra").isValid());
        assertFalse(verify(sign(claims(), "{\"alg\":\"none\",\"typ\":\"JWT\",\"kid\":\"test-key\"}")).isValid());
    }

    @Test
    void developmentIsBoundedAndCannotReplaceAPaidDownload() throws Exception {
        Map<String, String> claims = claims();
        claims.put("license_type", "\"DEVELOPMENT\"");
        claims.put("exp", Long.toString(Instant.now().plusSeconds(240).getEpochSecond()));
        String token = sign(claims);
        assertTrue(verify(token).isDevelopment());
        assertFalse(SongodaLicense.verifyToken(token, PRODUCT_ID, LICENSE, USER_ID, false, key, false).isValid());
        claims.put("exp", Long.toString(Instant.now().plusSeconds(3600).getEpochSecond()));
        assertFalse(verify(sign(claims)).isValid());
        claims.put("license_type", "\"UNKNOWN\"");
        assertFalse(verify(sign(claims)).isValid());
    }

    @Test
    void expirationHasNoTwentyFourHourGrace() throws Exception {
        Map<String, String> claims = claims();
        claims.put("iat", Long.toString(Instant.now().minusSeconds(600).getEpochSecond()));
        claims.put("exp", Long.toString(Instant.now().minusSeconds(1).getEpochSecond()));
        assertEquals(SongodaLicense.Status.EXPIRED, verify(sign(claims)).status());
        claims = claims();
        claims.put("license_expires_at", Long.toString(Instant.now().minusSeconds(1).getEpochSecond()));
        assertEquals(SongodaLicense.Status.EXPIRED, verify(sign(claims)).status());
    }

    @Test
    void signedTrialUsesItsActualLimit() throws Exception {
        Map<String, String> claims = claims();
        claims.put("license_type", "\"TRIAL\"");
        claims.put("max_players", "2");
        assertEquals(2, verify(sign(claims)).effectiveMaxPlayers());
        claims.put("max_players", "0");
        assertFalse(verify(sign(claims)).isValid());
        claims.put("max_players", "4294967296");
        assertFalse(verify(sign(claims)).isValid());
    }

    @Test
    void missingCredentialsNeverGrantLocalTrials() throws Exception {
        assertEquals(SongodaLicense.Status.INVALID, SongodaLicense.check(folder.toFile(), PRODUCT_ID, "1").status());
        Files.writeString(folder.resolve("product.key"), "DEVELOPMENT_LICENSE");
        assertEquals(SongodaLicense.Status.INVALID, SongodaLicense.check(folder.toFile(), PRODUCT_ID, "1").status());
    }

    @Test
    void cacheFailurePreservesVerifiedGrantAndDoesNotLeaveTemporaryFiles() throws Exception {
        String token = sign(claims());
        SongodaLicense.Result verified = verify(token);
        Path blocker = Files.writeString(folder.resolve("not-a-directory"), "data");
        SongodaLicense.Result result = SongodaLicense.cacheResult(blocker.resolve(".license"), token, verified);
        assertTrue(result.isValid());
        assertTrue(result.message().contains("cache unavailable"));
        assertEquals("data", Files.readString(blocker));
        assertTrue(SongodaLicense.cacheResult(folder.resolve(".license"), token, verified).isValid());
        assertEquals(token, Files.readString(folder.resolve(".license")));
        try (var files = Files.list(folder)) {
            assertEquals(2, files.count());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"a\":null,\"a\":1}", "{\"a\":1,}", "{\"a\":01}", "{\"a\":\"\\x\"}", "{\"a\":[1,]}", "{\"a\":null} garbage", "{\"a\":\"unterminated}"})
    void rejectsMalformedOrAmbiguousJson(String json) {
        assertThrows(IllegalArgumentException.class, () -> SongodaLicense.Json.object(json));
    }

    @Test
    void parsesEscapesNullAndAudienceArrayWithoutSearchingOtherFields() {
        Map<String, Object> json = SongodaLicense.Json.object("{\"aud\":null,\"next\":\"songoda-core\",\"a\":[\"x\"],\"text\":\"hello\\n\\u0022\"}");
        assertEquals(null, json.get("aud"));
        assertEquals("hello\n\"", json.get("text"));
        String value = "line\n\t\"\\";
        assertEquals(value, SongodaLicense.Json.object("{\"text\":" + SongodaLicense.Json.quote(value) + "}").get("text"));
    }

    private SongodaLicense.Result verify(String token) {
        return SongodaLicense.verifyToken(token, PRODUCT_ID, LICENSE, USER_ID, true, key, false);
    }

    private Map<String, String> claims() {
        Map<String, String> claims = new LinkedHashMap<>();
        claims.put("ver", "3");
        claims.put("iss", "\"songoda-auth\"");
        claims.put("aud", "[\"songoda-core\"]");
        claims.put("iat", Long.toString(Instant.now().minusSeconds(1).getEpochSecond()));
        claims.put("exp", Long.toString(Instant.now().plusSeconds(3600).getEpochSecond()));
        claims.put("license_expires_at", Long.toString(Instant.now().plusSeconds(86400).getEpochSecond()));
        claims.put("license_id", "\"" + LICENSE + "\"");
        claims.put("user_id", "\"" + USER_ID + "\"");
        claims.put("product_id", "\"" + PRODUCT_ID + "\"");
        claims.put("license_type", "\"PURCHASE\"");
        claims.put("max_players", "0");
        claims.put("max_ips", "3");
        return claims;
    }

    private String sign(Map<String, String> claims) throws Exception {
        return sign(claims, "{\"alg\":\"ES256\",\"typ\":\"JWT\",\"kid\":\"test-key\"}");
    }

    private String sign(Map<String, String> claims, String header) throws Exception {
        String payload = "{" + String.join(",", claims.entrySet().stream().map(entry -> "\"" + entry.getKey() + "\":" + entry.getValue()).toList()) + "}";
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String body = encoder.encodeToString(header.getBytes(StandardCharsets.UTF_8)) + "." + encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        Signature signer = Signature.getInstance("SHA256withECDSAinP1363Format");
        signer.initSign(pair.getPrivate());
        signer.update(body.getBytes(StandardCharsets.US_ASCII));
        return body + "." + encoder.encodeToString(signer.sign());
    }
}
