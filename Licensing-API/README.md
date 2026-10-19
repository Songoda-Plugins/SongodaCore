# Songoda Licensing-API

A single-source, shadeable Java 17+ license client for Bukkit/Paper plugins, independent of SongodaCore.
Consumers control startup, retries between checks, and player limits. The module includes no listeners or lifecycle hooks.

## Maven

```xml
<repositories>
    <repository>
        <id>songoda-public</id>
        <url>https://repo.songoda-reborn.com/repository/maven-public/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.songoda</groupId>
        <artifactId>Licensing-API</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

Shade and relocate `com.songoda.license` into your plugin's own package. The marketplace discovers the
`%%__LICENSE__%%`, `%%__USER_ID__%%`, and `%%__PRODUCT_ID__%%` literals after relocation.
They must remain intact until the download is personalized. Do not replace them in your source.
The fields are assigned in a static initializer to avoid Java constant inlining.

You can also copy `SongodaLicense.java` into your project. It needs no runtime JSON/JWT dependency or key resource.
The Paper dependency is provided; the plugin adapter uses the Bukkit description API.

## Nonblocking startup

Call from your normal `onEnable()`, and start licensed features only after success:

```java
SongodaLicense.verifyAsync(this, "your-product-uuid").whenComplete((result, error) ->
        getServer().getScheduler().runTask(this, () -> {
            if (error != null || !result.isValid() || result.isTrial()) {
                getLogger().severe(error == null ? result.message() : "License verification failed");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            // Start licensed features here.
        }));
```

This example deliberately declines trials. To support them, enforce `result.effectiveMaxPlayers()` yourself.
Zero is unlimited only on a valid result. The API never grants an uninjected build a local trial.

`verifyAsync(this)` uses the injected product UUID in personalized downloads. For local builds, pass the product UUID
shown beside the development credential. A caller-owned executor can be passed to `verifyAsync(plugin, productId, executor)`.
The synchronous `verify`, `isValid`, and `check` methods perform blocking I/O and must not run on the server thread.

Recheck licenses periodically, for example once per minute asynchronously. Do not keep using a grant after
`tokenExpiresAt()` or `licenseExpiresAt()`. Apply Bukkit changes on the server thread.
A `CompletableFuture` completion may run off-thread; do not initialize Bukkit listeners or features there directly.

## Creator development licenses

1. Sign in to Songoda, open a plugin you manage, and select its **Licensing** tab.
2. Create your own development credential for 1–90 days.
3. Put the displayed product UUID and returned creator credential UUID, in that order on separate lines, in
   `plugins/YourPlugin/license.key` for a local, uninjected build:

   ```text
   <product UUID>
   <creator credential UUID>
   ```

   Existing one-line `license.key` files that contain only the credential remain supported when the product UUID is
   supplied to verification or through the `songoda.license.product-id` system property.

For compatibility, local builds also continue to read `product.key` when `license.key` is not present.

Credentials are restricted to the account and plugin that issued them, with up to three active server IPs.
Only users with current plugin management access can create/read/revoke their own credential.
The server rechecks creator access when issuing verification tokens. Replacement invalidates the old UUID;
revocation stops new verification immediately. Cached development grants last at most five minutes.

Keep development credentials out of source control and distributed JARs. They are bearer credentials: someone
who obtains a real credential can use it for that plugin until expiry/revocation, subject to activation limits.
Paid downloads use their injected license UUID, opaque user UUID, and immutable product UUID. Usernames, email addresses,
and sequential database IDs are not embedded. Local files cannot override the injected identity,
and development grants are rejected for those injected builds.

The old global `SONGODA_DEV_LICENSE` mechanism and `SONGODA_PUBLIC_KEY.pem` overrides are not supported.

## Trust, caching, and outages

Only ES256 tokens with the expected issuer, audience, key ID, protocol version, identity, expiry, and limits are accepted.
Unknown license types, wildcard products, missing required claims, duplicate JSON fields, and invalid signatures fail closed.
There is no 24-hour expiration grace period.

Verification and public keys default to `https://songoda-reborn.com` and use normal certificate validation with
redirects disabled. Operators can override the base origin for both endpoints with a JVM system property:

```text
-Dsongoda.license.server-url=https://license.example.com
```

The value must be an HTTPS origin without credentials, path, query, or fragment. The override changes where the
client sends license identity and fetches the verification key, but it does not change which signing key is trusted.
The returned `keyId` and encoded public key must exactly match the production key compiled into this API release.
Local PEM files, environment variables, and classpath resources cannot alter this pin. An alternate endpoint that
does not present the pinned key is rejected, even if it returns an otherwise validly signed token.

The first check after a JVM restart needs an endpoint that serves the pinned key. The trusted key is cached in memory
and refreshed every five minutes; a previously authenticated key remains usable during a network outage. No unsigned
key cache is trusted from disk.
This assumes an untampered client and JVM trust configuration; server owners can still modify plugin bytecode or the JVM.

The disk cache contains only the signed token. Refresh is based on JWT `exp`, separate from license expiry:
one hour before paid-token expiry, or one minute before development-token expiry.
A network failure can fall back only to a token that is still valid when the request finishes.
Cache writes are atomic where supported, and a cache persistence failure does not invalidate a verified license.
Interrupted HTTP requests preserve interruption and stop retrying.

## Server rollout and key rotation

Deploy the updated SongodaWeb backend before releasing plugins using this client.
It supplies `GET /api/license/key` and protocol-v3 responses from `POST /api/license/verify`.
Existing legacy tokens are intentionally rejected and replaced by online verification.

The backend generates fresh private signing keys server-side, persists them in the server database,
and excludes the legacy development key resources from its artifact.
An active legacy bundled key is automatically retired when signing/key lookup is used.
The current API release pins key ID `2a4ca413fbccab79`. Key rotation is not transparent: publish a new API release
containing the next trusted key before changing the server's signing key, then allow dependent plugins to update
before the server starts issuing tokens with that key. Old API releases reject unknown keys. Back up and restrict
access to the signing-key database. Never ship private keys with a plugin or the API.

## Build and publishing

This module is independently versioned and can be built without the SongodaCore parent reactor:

```text
mvn clean verify
mvn deploy
```

Deployment targets the Songoda Maven repository configured in the POM and requires matching credentials in Maven
settings. A successful local build does not publish the artifact.
