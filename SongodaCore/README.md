# SongodaCore

The modern shared foundation for Songoda plugins.

SongodaCore provides the `SongodaPlugin` base class and implements its shared runtime directly under
`com.songoda.core`. It depends directly on [VInject](https://github.com/vortexdevelopment-net/VInject) for dependency injection and includes:

- VInject dependency injection;
- annotated commands and tab completion;
- listener and reload-hook registration;
- YAML configuration and serializers;
- database and repository support;
- GUI, Adventure, platform, and compatibility utilities.

## Legacy H2 database conversion

When a plugin uses H2 and SongodaCore finds an existing file database that the current H2 driver cannot open, it sends
the closed `.mv.db` file to Songoda's hosted converter at `https://songoda-reborn.com/api/database-convert/v1/h2`.
The converter returns a current-format H2 file. SongodaCore validates it locally, saves the original file as a
`.pre-v2-<timestamp>.bak` backup, and only then replaces the database. The conversion request uses HTTPS and carries
the configured H2 username and password in request headers. No H2 1.4 library is bundled with consuming plugins.

## Plugin update backups

Before a plugin's update migrations run, SongodaCore compares the plugin version and JAR checksum with the last
version that completed startup. A replaced SNAPSHOT JAR triggers a backup even when its version string is unchanged.
When the version or JAR changed, Core writes and verifies a ZIP containing the complete existing plugin data folder under
`plugins/.songoda-backups/`. The archive is named with the plugin name, old and new versions, and a UTC timestamp.
Core keeps the original files available while plugin migrations run and records the new version only after startup
succeeds. If archive creation fails, plugin startup stops before migrations can modify its data.

Licensing is opt-in. Free plugins do not need the licensing client; paid plugins can call
`verifySongodaLicense()` from `verifyLicense()` to validate a Songoda grant and stop startup when verification fails.
SongodaCore rejects trial grants by default. A plugin that explicitly accepts trials can call
`verifySongodaLicense(true)`. SongodaCore does not apply player or IP limits.

## Requirements

- Java 17+
- Maven 3.8+
- Paper-compatible server (Paper, Purpur, Leaf, Pufferfish, Folia, or a supported Paper-API hybrid) running Minecraft 1.18.2+

## Build

```bash
mvn clean verify
```

The project publishes `com.songoda:SongodaCore` as a self-contained runtime JAR containing the core classes and VInject.
Every final Songoda plugin only needs to relocate
`com.songoda.core` to `com.songoda.<plugin-name>.core`.

SongodaCore cannot know the package of the plugin that will consume it. Its runtime uses the native Paper API directly,
and platform services are located dynamically after relocation. For a plugin whose main package is
`com.songoda.epicfarming`, the relocation target must be
`com.songoda.epicfarming.core`.

## Consuming SongodaCore

Add the dependency:

```xml

<dependency>
    <groupId>com.songoda</groupId>
    <artifactId>SongodaCore</artifactId>
    <version>5.0.0-SNAPSHOT</version>
</dependency>
```

The final plugin should shade the dependency and relocate the SongodaCore package to `com.songoda.<plugin-name>.core`.
For example, a plugin whose main package is
`com.songoda.epicfarming` can use this complete shade configuration:

```xml

<plugin>
    <groupId>com.songoda</groupId>
    <artifactId>EpicFarming</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <dependencies>
        <dependency>
            <groupId>com.songoda</groupId>
            <artifactId>SongodaCore</artifactId>
            <version>5.0.0-SNAPSHOT</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.6.0</version>
                <dependencies>
                    <dependency>
                        <groupId>net.vortexdevelopment</groupId>
                        <artifactId>MavenYamlTransformer</artifactId>
                        <version>1.0.1</version>
                    </dependency>
                </dependencies>
                <configuration>
                    <!-- Keep SongodaCore's reflectively loaded platform services in the final plugin JAR. -->
                    <minimizeJar>false</minimizeJar>
                    <relocations>
                        <relocation>
                            <pattern>com.songoda.core</pattern>
                            <shadedPattern>com.songoda.epicfarming.core</shadedPattern>
                        </relocation>
                    </relocations>
                    <transformers>
                        <transformer implementation="net.vortexdevelopment.MavenYamlTransformer">
                            <paths>
                                <path>plugin.yml</path>
                                <path>paper-plugin.yml</path>
                            </paths>
                        </transformer>
                    </transformers>
                </configuration>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</plugin>
```

## Paid plugin licensing

The licensing client is a provided compile dependency of SongodaCore, so free plugins do not package or require it.
Paid plugins must add the standalone client as a dependency and shade/relocate it into their own JAR:

```xml
<dependency>
    <groupId>com.songoda</groupId>
    <artifactId>Licensing-API</artifactId>
    <version>1.0.0</version>
</dependency>
```

Then opt in through the existing lifecycle hook:

```java

@Override
protected void verifyLicense() throws PluginVerificationException {
    verifySongodaLicense();
}
```

Use `verifySongodaLicense(true)` only when the plugin intentionally accepts trial grants. The marketplace discovers
the client after shading by scanning its placeholder fields. It injects the published license identity, which takes
precedence over local settings. For local development builds, pass the product UUID to
`verifySongodaLicense(productId)` or set `-Dsongoda.license.product-id=<product UUID>`, then place the creator
development credential at `plugins/<PluginName>/license.key`. The client checks the local cache and requests a
refreshed grant as needed; `product.key` remains supported as a legacy fallback.
Because the client is not included in SongodaCore's runtime JAR, a paid plugin must package it. Free plugins can
continue to depend on SongodaCore without adding the client.

The important part is the consumer-specific SongodaCore relocation:

```xml

<relocation>
    <pattern>com.songoda.core</pattern>
    <shadedPattern>com.songoda.epicfarming.core</shadedPattern>
</relocation>
```

Use the Maven YAML transformer in the final plugin so the embedded SongodaCore descriptor fragment is merged
into the plugin's own `plugin.yml`. Do not leave SongodaCore in one shared package such as `com.songoda.core`, or the
shared package can cause collisions between multiple Songoda plugins. `SongodaPlugin.getSongodaCorePackage()`
resolves the relocated runtime package automatically.

## Plugin skeleton

```java
package com.songoda.exampleplugin;

import com.songoda.core.SongodaPlugin;
import net.vortexdevelopment.vinject.annotation.component.Root;
import org.jetbrains.annotations.Nullable;

@Root(
        packageName = "com.songoda.exampleplugin",
        createInstance = false
)
public final class ExamplePlugin extends SongodaPlugin {

    @Override
    public void onPluginLoad() {
        initDatabase();
    }

    @Override
    protected @Nullable Integer getBstatsPluginId() {
        return 12345;
    }
}
```

`initDatabase(...)` must be called from `onPluginLoad()`. Every concrete plugin class needs its own `@Root` annotation
because VInject uses it as the scan anchor.

## Commands and components

Songoda plugins use VInject annotations and SongodaCore's command and registration annotations:

```java

@Command("example")
public final class ExampleCommand {

    @Inject
    private ExampleService service;

    @BaseCommand
    public void execute(@Sender CommandSender sender) {
        service.execute(sender);
    }
}
```

Keep public Songoda contracts in separate API modules when a plugin has an API surface. Keep SongodaCore, Bukkit,
commands, listeners, configuration, and GUI implementations in plugin modules.

## Project layout

```text
SongodaCore/
├── pom.xml
├── README.md
├── .gitignore
└── src/
    └── main/
        ├── java/com/songoda/core/ (bootstrap, commands, compatibility, GUI, hooks, text, and VInject integration)
        └── resources/ (descriptor, database defaults, language defaults, and VInject templates)
```
