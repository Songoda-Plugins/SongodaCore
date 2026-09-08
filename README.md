# SongodaCore

**The modern shared foundation for Songoda plugins.**

<p align="center">
  <a href="https://songoda-reborn.com">Website</a> ·
  <a href="https://discord.gg/vtuJUfebrd">Join the Discord</a> ·
  <a href="SongodaCore/README.md">SongodaCore documentation</a> ·
  <a href="Licensing-API/README.md">Licensing-API documentation</a>
</p>

SongodaCore provides the Songoda-facing `SongodaPlugin` base class and a shared runtime built on
[VortexCore](https://github.com/vortexdevelopment-net/VortexCore). The repository also contains the standalone
`Licensing-API` client for plugins that need Songoda licensing.

## Modules

| Module | Description |
| --- | --- |
| [`SongodaCore`](SongodaCore/README.md) | VortexCore-based runtime and `SongodaPlugin` base class. |
| [`Licensing-API`](Licensing-API/README.md) | Standalone Bukkit/Paper license client that can be shaded into a plugin. |

Read the [SongodaCore documentation](SongodaCore/README.md) for requirements, build instructions, consumption, shading,
relocation, and plugin integration. Read the [Licensing-API documentation](Licensing-API/README.md) for verification,
development licenses, caching, and rollout guidance.

## Build

Build all modules with:

```bash
mvn clean install
```

The parent configures the Songoda Maven repository for deployment. Use `mvn deploy` with credentials for the
`songoda-public` server entry in Maven `settings.xml`.

For support, announcements, and community discussion, [join the Songoda Discord](https://discord.gg/vtuJUfebrd).
