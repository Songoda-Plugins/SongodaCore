# Songoda Licensing-API

A single-source, shadeable Java 17+ license client for Bukkit/Paper plugins, independent of SongodaCore and VortexCore.
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
        <version>5.0.0-SNAPSHOT</version>
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
3. Put the returned UUID in `plugins/YourPlugin/product.key` in a local, uninjected build.
4. Pass the displayed product UUID in verification.

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

Verification and public keys use fixed Songoda HTTPS endpoints with normal certificate validation and no redirects.
No environment variable, JVM property, local PEM, or classpath resource changes the API's verification authority.
The first check after a JVM restart needs Songoda online to authenticate its public key.
Trusted keys are cached in memory and refreshed every five minutes; a previously authenticated key remains usable
during a network outage. No unsigned key cache is trusted from disk.
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
Administrators rotate keys in the signing administration page; clients refresh on an unknown key ID.
Back up and restrict access to the signing-key database. Never ship private keys with a plugin or the API.

## Build and publishing

From the existing SongodaCore parent directory:

```text
mvn -pl Licensing-API -am clean verify
mvn -pl Licensing-API -am deploy
```

The parent POM must be available alongside the module. Deployment inherits the configured Songoda repository
and requires the matching Maven credentials. Verify the repository is a writable deployment destination
before publishing; this build does not deploy automatically.
