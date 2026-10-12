![Ancestral Archetypes](https://raw.githubusercontent.com/borisshoes/AncestralArchetypes/main/non-mod-resources/Banner.png)

# Pick one of 30+ Mobs and gain unique abilities!

**Server-Side Origins-inspired mod for Fabric.**

[![Minecraft Version](https://cf.way2muchnoise.eu/versions/Minecraft_1331020_latest.svg?badge_style=for_the_badge)](https://www.curseforge.com/minecraft/mc-mods/ancestral-archetypes/files) [![Server-side](https://img.shields.io/badge/Environment-Server--side-C65135?style=for-the-badge)](https://www.curseforge.com/minecraft/mc-mods/ancestral-archetypes/files) [![Requires Fabric API](https://img.shields.io/badge/Requires-Fabric_API-DBD0B4?style=for-the-badge)](https://www.curseforge.com/minecraft/mc-mods/fabric-api)

[![Discord](https://img.shields.io/discord/1403978074550439966?style=for-the-badge&logo=discord&logoColor=white&label=Discord&color=5865F2)](https://discord.gg/dTKPeNEg6r) [![Source](https://img.shields.io/badge/Source-GitHub-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/borisshoes/AncestralArchetypes)

## Features
- **30 playable mobs in 11 Archetypes**, from Axolotl and Fox to Warden and Ender Dragon, each with its own strengths and drawbacks.
- **Around 100 abilities.** Fire volleys of fireballs, teleport like an Enderman, glide on wings, ride a soulbound Spirit Mount, burrow through the ground or brew free potions.
- **Meaningful weaknesses.** Aquarians dry out on land, Infernals freeze in the cold, Golems cannot regenerate naturally. Every pick is a trade-off.
- **Balanced for multiplayer.** Designed around a competitive environment, with team-aware abilities and a spyglass mechanic for scouting what other players are.
- **In-game GUIs.** Players browse and pick Archetypes from a menu, and can read every ability with its exact numbers.
- **Built for servers.** No client mods, a resource pack served by the server, 200+ config options that can be changed without a restart, and a permission node for every command.
- **Make it your own.** Admins can add or remove abilities on any Archetype with a command.
- **Full translation support** through language files.

## The Archetypes
| Archetype | Mobs |
|---|---|
| **Aquarians** | Axolotl, Guardian, Salmon |
| **Canids** | Fox, Wolf |
| **Centaurs** | Horse, Donkey, Camel |
| **Enderians** | Enderman, Shulker, Ender Dragon |
| **Felids** | Cat, Ocelot |
| **Gelatians** | Slime, Magma Cube, Sulfur Cube |
| **Golems** | Copper Golem, Tuff Golem, Iron Golem |
| **Infernals** | Blaze, Wither Skeleton, Strider |
| **Sentinels** | Creaking, Warden |
| **Swampers** | Witch, Cave Spider, Frog |
| **Windswepts** | Breeze, Parrot, Ghastling |

[![Archetype Summaries](https://raw.githubusercontent.com/borisshoes/AncestralArchetypes/main/non-mod-resources/Full%20Infographic%20Small.png)](https://raw.githubusercontent.com/borisshoes/AncestralArchetypes/main/non-mod-resources/Full%20Infographic.png)

Click the infographic for the full-size version. The [GitHub page](https://github.com/borisshoes/AncestralArchetypes#archetypes) lists every mob's abilities as text.

## Getting Started
1. **Pick an Archetype.** Run `/archetypes changeArchetype` to open the selection menu and choose your mob. New players can do this once by default, so read before you pick!
2. **Learn your abilities.** `/archetypes abilities` lists what your mob can do, with the exact values your server uses.
3. **Get your Ability Items.** Active abilities are used through items. `/archetypes items` gives them to you, and gives them back if you lose them.
4. **Scout other players.** Look at a player through a spyglass for a few seconds to reveal their Archetype.

Not sure what to pick? `/archetypes list` lets you browse every Archetype and its abilities without choosing one.

## Videos
| Mod Spotlight / Tutorial | 1.2.0 Changes Showcase | 1.3.0 Changes Showcase |
|:---:|:---:|:---:|
| [![Mod Tutorial](https://img.youtube.com/vi/QApJcu1rpD0/mqdefault.jpg)](https://www.youtube.com/watch?v=QApJcu1rpD0) | [![1.2.0 Changes Showcase](https://img.youtube.com/vi/ZWrzjhnXP8E/mqdefault.jpg)](https://www.youtube.com/watch?v=ZWrzjhnXP8E) | [![1.3.0 Changes Showcase](https://img.youtube.com/vi/8HL_ekXbSds/mqdefault.jpg)](https://www.youtube.com/watch?v=8HL_ekXbSds) |

## Server Setup
1. Install Fabric Loader and [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api) on your server, then put the Ancestral Archetypes jar in the `mods` folder. Every other library is bundled, and players do not need to install anything.
2. Launch the server once to generate the configs.
3. Open `config/polymer/auto-host.json` and change `"enabled": false` to `true`. The server now generates the mod's resource pack and sends it to players when they connect.
4. (Optional) In the same file you can make the resource pack required, or set the `"message"` that players see when they are asked to download it.

The mod works without the resource pack, but Ability Items will show as vanilla items instead of their own textures.

## Commands
* `/archetypes changeArchetype` Opens a menu to choose your Archetype.
* `/archetypes list` Opens a menu to browse all Archetypes.
* `/archetypes abilities [<archetype>]` Shows the abilities of your Archetype, or the one given, with their config values.
* `/archetypes items` Gives you the Ability Items for your Archetype (1 minute cooldown).
* `/archetypes toggleReminders` Toggles the reminders about a missing Archetype or Ability Item.

Players can also customize their Spirit Mount, glider wings and helmet colors. Admins have commands for setting Archetypes, granting Archetype changes, viewing the server's Archetype distribution, and adding or removing abilities on any Archetype. Every config value can be viewed and changed in game with `/archetypes config <setting>`, and every command has a [Fabric Permissions API](https://github.com/lucko/fabric-permissions-api) node with a vanilla permission level fallback.

The full reference is on GitHub: [commands](https://github.com/borisshoes/AncestralArchetypes#commands), [configuration](https://github.com/borisshoes/AncestralArchetypes#admin-configuration-commands) and [permission nodes](https://github.com/borisshoes/AncestralArchetypes#permission-nodes).

## Try My Other Mods!
All server-side Fabric mods — no client installation required.

| Mod | Description | Links |
|---|---|---|
| **Arcana Novum** | Minecraft's biggest server-side magic mod: powerful items, multiblock structures, custom bosses and deep lore. | [![GitHub](https://img.shields.io/badge/GitHub-181717?logo=github&logoColor=white)](https://github.com/borisshoes/ArcanaNovum) [![CurseForge](https://img.shields.io/badge/CurseForge-F16436?logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/arcana-novum) |
| **Ender Nexus** | Highly configurable /home, /spawn, /warp, /tpa and /rtp commands all in one, and individually disablable. | [![GitHub](https://img.shields.io/badge/GitHub-181717?logo=github&logoColor=white)](https://github.com/borisshoes/EnderNexus/) [![CurseForge](https://img.shields.io/badge/CurseForge-F16436?logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/ender-nexus-fabric-teleports) |
| **Fabric Mail** | An in-game virtual mailbox system for sending packages and messages between online and offline players. | [![GitHub](https://img.shields.io/badge/GitHub-181717?logo=github&logoColor=white)](https://github.com/borisshoes/fabric-mail/) [![CurseForge](https://img.shields.io/badge/CurseForge-F16436?logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/fabric-mail) |
| **Fabric Trade** | Adds /trade, a secure player-to-player trading interface. | [![GitHub](https://img.shields.io/badge/GitHub-181717?logo=github&logoColor=white)](https://github.com/borisshoes/fabric-trade/) [![CurseForge](https://img.shields.io/badge/CurseForge-F16436?logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/fabric-trade) |
| **Limited AFK** | AFK detection and management with configurable kick thresholds for servers. | [![GitHub](https://img.shields.io/badge/GitHub-181717?logo=github&logoColor=white)](https://github.com/borisshoes/LimitedAFK/) [![CurseForge](https://img.shields.io/badge/CurseForge-F16436?logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/limited-afk) |
| **Links in Chat** | Makes URLs posted in chat clickable. | [![GitHub](https://img.shields.io/badge/GitHub-181717?logo=github&logoColor=white)](https://github.com/borisshoes/fabric-linksinchat/) [![CurseForge](https://img.shields.io/badge/CurseForge-F16436?logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/links-in-chat) |

## Support
Questions go to the [Discord](https://discord.gg/dTKPeNEg6r), and bug reports and suggestions go to [GitHub Issues](https://github.com/borisshoes/AncestralArchetypes/issues).

### License Notice
By using this project in any form, you hereby give your "express assent" for the terms of the license of this project, and acknowledge that I, BorisShoes, have fulfilled my obligation under the license to "make a reasonable effort under the circumstances to obtain the express assent of recipients to the terms of this License."
