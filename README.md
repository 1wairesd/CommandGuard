# CommandGuard

> Cross-platform command blocker and tab-complete manager for Minecraft networks.

[![Modrinth](https://img.shields.io/modrinth/v/commandguard?label=modrinth&color=00AF5C)](https://modrinth.com/plugin/commandguard)
[![License](https://img.shields.io/github/license/1wairesd/CommandGuard)](LICENSE)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://adoptium.net)

## Platforms

| Platform | Versions |
|----------|----------|
| Paper / Spigot | 1.13+ |
| Velocity | 3.1+ |
| Waterfall | 1.18+ |

## Features

- Tab-complete filtering — players only see commands they're allowed to use
- Command blocking with custom actions (message, title, sound, kick, potion effect...)
- Whitelist mode — allow only specific commands, block everything else
- Permission groups with priority, inheritance and merging
- Per-server groups — different rules per backend server (proxy only)
- Cross-server sync via plugin messaging (TabSync)
- Spy-client protection at the packet level
- Multi-language support — `en_EN`, `ru_RU`, add your own
- Configurable prefix in the lang file

## Installation

1. Download the jar for your platform (`-spigot`, `-velocity`, or `-waterfall`)
2. Drop it into `plugins/`
3. Start the server — config files are generated automatically
4. Edit `plugins/CommandGuard/config.yml`
5. Run `/cg reload` (backend) or `/cgv reload` (proxy)

## Building

```
./gradlew build
```

Outputs three jars in `build/libs/`:
- `CommandGuard-*-spigot.jar`
- `CommandGuard-*-velocity.jar`
- `CommandGuard-*-waterfall.jar`

## Documentation

[1wairesd.github.io/1wairesdIndustriesWiki](https://1wairesd.github.io/1wairesdIndustriesWiki/docs/CommandGuard/commandguard-intro)

## License

MIT — based on [EasyCommandBlocker](https://github.com/Ajneb97/EasyCommandBlocker) by Ajneb97.
