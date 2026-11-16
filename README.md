<a id="readme-top"></a>

<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="media/banner-dark.png">
  <source media="(prefers-color-scheme: light)" srcset="media/banner-light.png">
  <img alt="Sisyphus the Mushroom" src="media/banner-dark.png" width="100%">
</picture>

<br />

![Java 21](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white)
![Engine](https://img.shields.io/badge/engine-custom%20Swing%20%2F%20Java2D-8a2be2?style=for-the-badge)
![Status](https://img.shields.io/badge/status-in%20development-success?style=for-the-badge)
![Beta](https://img.shields.io/badge/beta-2027-c0392b?style=for-the-badge)
![Steam](https://img.shields.io/badge/Steam-coming%20soon-171a21?style=for-the-badge&logo=steam&logoColor=white)

**Fall in the forest, wake up in the village, go back in stronger.**

[Trailer](#-trailer) &middot; [Features](#-features) &middot; [Roadmap](#-roadmap) &middot; [Under the hood](#-under-the-hood) &middot; [Build](#-build-and-run) &middot; [Author](#-author)

</div>

<details>
  <summary>Table of contents</summary>
  <ol>
    <li><a href="#-trailer">Trailer</a></li>
    <li><a href="#-about-the-game">About the game</a></li>
    <li><a href="#-features">Features</a></li>
    <li><a href="#-roadmap">Roadmap</a></li>
    <li><a href="#-under-the-hood">Under the hood</a></li>
    <li><a href="#-build-and-run">Build and run</a></li>
    <li><a href="#-license">License</a></li>
    <li><a href="#-author">Author</a></li>
  </ol>
</details>

## 🎬 Trailer

<div align="center">

<!-- When the trailer is on YouTube, replace the href with its URL. Nothing else changes. -->
<a href="#-trailer">
  <img src="media/trailer-coming-soon.webp" alt="Trailer coming soon" width="720">
</a>

*Gameplay trailer in production. Beta release planned for 2027.*

</div>

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## ✨ About the game

**Sisyphus the Mushroom** is a top-down 2D action RPG in hand-made pixel art. Falling in the forest does not end the run: you wake up again, keep your progress, and head back out a little stronger.

The first chapter runs from a strange prologue, through a quiet village and its people, to the first boss, the Slime King. The story is told through visual-novel scenes, in-world documents and short conversations rather than long cutscenes.

<div align="center">
  <img src="media/day-night-cycle.gif" alt="The village over 24 hours, from night to dawn to dusk" width="640">
  <br />
  <sub>The village across a full in-game day. Live rendering, no mock-ups.</sub>
</div>

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## 🍄 Features

| | |
|---|---|
| ⚔️ **Combat with a swap rhythm** | Short sword, heavy sword, pistol and shotgun. Switching weapons mid-fight grants a damage bonus with a cooldown, so you plan swings instead of mashing one button. |
| 🎯 **Marks and detonations** | Ranged attacks mark enemies, melee attacks detonate the marks. Relics change how both halves behave. |
| 👑 **Multi-phase bosses** | The Slime King changes tactics across three phases, each with its own music. |
| 🌗 **A living world** | A full day and night cycle with dawn and dusk, local light sources, weather-like ambience and a clock that advances when you sleep or fall. |
| 🏘️ **A village that remembers** | Residents have conversation menus, errands and reactions to what you did. A mining station, a forge, a dojo and an anchor stone you return to. |
| 🌲 **Forest to explore** | Trails, valleys, lakes and fog. A map and minimap with discovery masks, iron ore to mine and shortcuts to open. |
| 📖 **Story you read in the world** | Notes, pages and reports you can pick up and re-read. A built-in visual-novel layer for the scenes that need it. |
| 🎶 **Original soundtrack** | Dozens of tracks and effects composed with a custom synthesizer: one theme per place, per boss phase and per mood. |
| 💾 **Careful saves** | Slot saves with migration of older versions, and a resume point that is separate from the respawn point. |

<div align="center">
  <img src="media/boss-fight.webp" alt="Slime King boss fight highlights" width="640">
  <br />
  <sub>The Slime King: a swap-attack hit, a poison spiral and the crystal shell phase.</sub>
</div>

### 📸 Gallery

<div align="center">
<table>
  <tr>
    <td align="center"><img src="media/screenshot-village-night.png" alt="The village square at night" width="100%"><br><sub><b>The village at night</b>: lamps, dusk light and the quest tracker</sub></td>
    <td align="center"><img src="media/screenshot-village-day.png" alt="The village square at dawn" width="100%"><br><sub><b>Dawn in the village</b>: the same square a few hours later</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="media/screenshot-forest.png" alt="A forest trail in the evening" width="100%"><br><sub><b>The forest</b>: trails, lamplight and a minimap that fills in as you explore</sub></td>
    <td align="center"><img src="media/screenshot-boss.png" alt="The Slime King, second phase" width="100%"><br><sub><b>The Slime King</b>: a three-phase boss fight</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="media/screenshot-camp.png" alt="A conversation at a campfire" width="100%"><br><sub><b>Conversations</b> with the people you meet</sub></td>
    <td align="center"><img src="media/screenshot-library.png" alt="Inside the scholar's library" width="100%"><br><sub><b>Interiors</b>: the village houses can be entered</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="media/screenshot-journal.png" alt="The quest journal" width="100%"><br><sub><b>The journal</b>: main quest, side quests and notes</sub></td>
    <td align="center"><img src="media/screenshot-title.png" alt="The title screen" width="100%"><br><sub><b>The title screen</b></sub></td>
  </tr>
</table>
</div>

> [!NOTE]
> More screenshots and clips will be added as the beta gets closer.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## 🗺️ Roadmap

- [x] Prologue, training and the first encounter
- [x] Village with interiors, residents, shops and an anchor stone
- [x] First forest with trails, lakes, mining and shortcuts
- [x] Slime King boss fight with rewards
- [x] Day and night cycle, sleep, death and return loop
- [x] Original music and effects, PCM audio mixer
- [x] Authoring tools for maps, placements and story scenes
- [ ] Chapter One story rework and polish pass
- [ ] Chapters 2 to 8 and their bosses
- [ ] Closed testing
- [ ] **Public beta, 2027**
- [ ] **Steam release**

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## 🔧 Under the hood

This repository publishes the game's source code. The game is built **without a game engine**: windowing, the render pipeline, lighting, audio mixing, saves and tooling are written from scratch on top of Java 21, Swing and Java2D.

<details>
<summary><b>Architecture at a glance</b></summary>

```mermaid
flowchart LR
  Input[input] --> Main[main: loop and state]
  Main --> World[world: entities, collisions]
  Main --> UI[ui: HUD, menus]
  World --> Render[world/render: lighting, ambience]
  World --> Combat[world/combat: projectiles]
  Main --> Story[story + dialogue]
  Main --> Progress[progression: skills, relics, shops]
  Progress --> Save[save]
  Main --> Audio[audio: PCM mixer and music director]
```

| Package | Responsibility |
|---|---|
| `main` | Game panel, loop and game state |
| `audio`, `settings`, `input` | Playback, player settings, keyboard handling |
| `ui`, `debug` | HUD, menus, window management, developer tools |
| `progression`, `save` | Skills, relics, wallet, shops, persistence |
| `entity` | Actors, animation, combat rules, weapons, enemies, NPCs |
| `world`, `world/combat`, `world/render` | World objects, collisions, projectiles, lighting |
| `level`, `tile` | Campaign structure, maps, tile data and rendering |
| `story`, `dialogue` | Story sequences, quests, conversations |

</details>

<details>
<summary><b>Engineering highlights</b></summary>

- A **custom PCM mixer** with its own thread, crossfades, voice limits and a cache budget, instead of per-clip playback.
- A **CPU lighting and ambience pipeline** that produces a smooth day and night cycle at whole-number pixel scales.
- **Versioned saves** with migration, rollback on failed writes and a clear split between the resume point and the respawn point.
- A **narrative state machine** for scenes, quests, conversation menus and area triggers, driven by script files.
- **Private authoring tools** for maps and story scenes, and generators for art and audio, so content is reproducible.
- A large automated test suite: JUnit tests plus chapter regression scenarios that drive the real game headlessly and check rewards, saves, audio and rendering. Continuous integration runs on Windows and Linux.

</details>

<details>
<summary><b>By the numbers</b></summary>

| | |
|---|---|
| Language | Java 21 |
| Public source files | 140+ |
| Public source lines | roughly 28,000 |
| Automated tests | 130+ unit tests and a set of integration scenarios |
| Engine dependencies | none |

</details>

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## 🚀 Build and run

> [!IMPORTANT]
> This repository contains only the source code. The artwork, music, maps and story scripts are not published, so the game cannot be built from this repository alone. Playable builds will be released through the beta and Steam.

The code expects Java 21 and the project layout described above. If you want to read the code, start with `src/main/`, then follow the packages in the table.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## 📝 License

The **source code** is released under the [MIT License](LICENSE).

All game assets (graphics, music, sound effects, fonts, maps and story) and the screenshots and clips in `media/` are Copyright (c) 2026 Maciej Serafin, **all rights reserved**. They are not covered by the MIT license; see [ASSETS-LICENSE.md](ASSETS-LICENSE.md).

## 👤 Author

**Maciej Serafin**

- GitHub: [@macieserafin](https://github.com/macieserafin)

If you like the project, a ⭐ helps a lot while the Steam page is being prepared.

---

<div align="center">
  <sub>Made with Java, pixels and too many mushrooms.</sub>
</div>
