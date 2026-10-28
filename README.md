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

## How this repository is maintained

The game lives in two repositories. Day-to-day work happens in a private one that holds the complete project:
source, original assets and the launcher. This public repository is the open-source side of it.

A GitHub Actions workflow in the private repository publishes here on every push to its main branch. It copies
only an explicit allowlist of paths (currently `src/`) and adds them as an ordinary commit titled
`Sync from private repository (<short sha>)`. Anything not on the allowlist, such as assets, story scripts,
the launcher or internal notes, is never copied, and a validation step fails the run instead of publishing if a
forbidden file type shows up. It authenticates with a deploy key limited to this one repository, never exports the private history and
never force-pushes, so this repository's commit history stays intact and grows linearly. If nothing in the
published paths changed, no commit is made.

The result is a single working copy for development, with the source code kept public and the assets kept private.

## Development history

The full commit history from the beginning of the project is kept. Early commits still contain the
first assets; see [ASSETS-LICENSE.md](ASSETS-LICENSE.md) for the terms that apply to them.

## License

- Source code: [MIT](LICENSE)
- Game assets: all rights reserved, see [ASSETS-LICENSE.md](ASSETS-LICENSE.md)
