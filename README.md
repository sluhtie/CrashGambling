# CrashGambling

A Minecraft Spigot plugin for a crash gambling minigame with economy and hologram integration.

**Author:** sluhtie  
**Version:** 1.0.0  
**API:** Spigot 1.17+  
**Java:** 17

## Dependencies

- **Vault** – Economy API (e.g. for EssentialsX Economy)
- **FancyHolograms** – In-game holograms
- **Folia** – optionally supported (folia-supported: true)

## Building

```bash
./gradlew build
```

The built JAR will be in `build/libs/`.

## Installation

1. Place the CrashGambling JAR in your Spigot server’s `plugins/` folder.
2. Install and configure Vault and FancyHolograms.
3. Start or reload the server; the config is created at `plugins/CrashGambling/config.yml`.

## Commands

| Command | Description | Permission |
|--------|--------------|------------|
| `/crash bet <amount>` | Place a bet | `crash.use` |
| `/crash cashout` | Cash out before crash | `crash.use` |
| `/crash status` | Show game status | `crash.use` |
| `/crash reload` | Reload configuration | `crash.admin` |

## Permissions

- `crash.use` – Use the crash game (default: true)
- `crash.admin` – Admin commands such as reload (default: op)

## Project Structure

```
src/main/java/com/crashgambling/
├── CrashGambling.java      # Plugin main class
├── command/CrashCommand.java
├── data/PlayerBet.java
├── game/BetManager.java
├── game/GameManager.java
├── game/GameState.java
└── service/
    ├── ActionBarService.java
    └── HologramService.java
```

## License

Project-specific – see repository or author.
