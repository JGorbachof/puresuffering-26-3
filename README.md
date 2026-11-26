# Pure Suffering - Unofficial Port

Survive increasingly severe invasions as your Minecraft world ages. The mod includes Standard, Hyper and Nightmare invasions, configurable spawn limits and game rules, custom invasion datapacks, weather, sky effects and sound cues.

Original project: [TheAGamePlayer/The-Pure-Suffering-Mod](https://github.com/TheAGamePlayer/The-Pure-Suffering-Mod), version 1.6.8.5R, revision 89e0eaaf10b69cdcbe82a366a25e2b7a8038169d.

## Installation

Minecraft 26.3, NeoForge 26.3.0.118, Java 25. Install the runtime JAR from [Releases](https://github.com/JGorbachof/puresuffering-26-3/releases) on the client and server. No additional mod is required. Start a new 26.3 world; migration from 1.21.1 saves is unsupported. Back up worlds before testing.

## Alpha status

Headless tests passed for all three invasion difficulties, actual mob spawning, hypercharge, targeting, anti-grief, XP, kill-limit termination, active-session disk persistence, packet codecs and native day/night transitions. All 20 invasion definitions and 23 game rules load. Real multiplayer synchronization, client rendering and audio, full invasion variety and modpack compatibility remain unverified.

## Build, source and credits

Run `gradlew.bat build` on Windows or `./gradlew build` on Linux with Java 25. The exact complete corresponding source archive, including build scripts and Gradle wrapper, is attached to the release. Original GPL v3 license retained in [LICENSE](LICENSE). Original mod and repository assets by TheAGamePlayer; see [original README](README.upstream.md), [port notes](PORT_NOTES.md) and [verification](verification/README.md). AI-assisted port development.

[ModHub](https://jgorbachof.github.io/) | [Discord](https://discord.gg/3zPAp2vk6Q)
