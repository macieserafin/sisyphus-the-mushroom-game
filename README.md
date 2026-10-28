# Sisyphus the Mushroom

A 2D pixel-art action game written in plain Java, on a custom engine built with Swing / Java2D
(no game framework). Top-down combat, tile-based maps, a visual-novel style story mode, a village hub and a
built-in tile map editor.

> **This repository is not the complete game.** It contains the engine and game source code only.
> The original assets (graphics, sound, music, story scripts) and the launcher class are not included
> in the current version. The finished game will be released on **Steam**.
>
> *To repozytorium zawiera tylko kod źródłowy silnika i gry. Assety oraz klasa uruchamiająca nie są
> publiczne; gotowa gra pojawi się na Steam.*

## What is here

| Path | Contents |
|---|---|
| `src/main` | game loop, panel, UI, menus, input, sound |
| `src/entity`, `src/world` | player, enemies, NPCs, weapons, combat, collisions |
| `src/level`, `src/tile` | maps, tile sets, level and village layout |
| `src/story`, `src/dialogue` | story sequence engine, script loader, dialogue system |
| `src/mapEditor` | standalone tile map editor |

Because `res/` and the launcher are missing, the code in this repository does not run on its own.

## Development history

The full commit history from the beginning of the project is kept. Early commits still contain the
first assets; see [ASSETS-LICENSE.md](ASSETS-LICENSE.md) for the terms that apply to them.

## License

- Source code: [MIT](LICENSE)
- Game assets: all rights reserved, see [ASSETS-LICENSE.md](ASSETS-LICENSE.md)
