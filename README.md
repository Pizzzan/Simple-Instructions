# Simple Instructions

A Minecraft Fabric mod that displays interactive tutorial overlays when players join a world. This mod includes a fully functional in-game visual editor so you can build tutorials without touching a config file.

## Features

- **In-Game Plaque Editor:** Create, edit, and test your tutorials live in-game with an intuitive UI.
- **Action Types:** you can rogress steps with key presses, key holds, timers, or dismiss actions.
- **Progress Tracking:** Automatically saves player progress so they can resume later if they disconnect.
- **Highly Configurable:** configurable colors, fonts, and positioning via Mod Menu.

## Installation

1. Install Fabric Loader 0.16.10+ for Minecraft 1.20.1.
2. Install the Fabric API.
3. Drop the mod JAR into your `mods/` folder.
4. *(Optional, but highly recommended)* Install Mod Menu to easily access the global settings menu.

## Creating Tutorials

To start building your custom instructions, join a world (you must be in single-player or have OP level 2 on a server) and run:

`/simpleinstructions editor`

or just press *esc* on your keyboard and locate a button on your top right corner.

From the Plaque Editor, you can manage your steps, icons, text, and visual overrides.

### Action Types
When configuring a step, you can set how the player completes it:
- **KEY_PRESS:** The player must tap a specific key.
- **KEY_HOLD:** The player must hold down a specific key.
- **WAIT_DURATION:** A timer. Uses ticks (20 ticks = 1 second).
- **DISMISS:** Waits for the player to acknowledge the plaque by pressing any key or clicking their mouse.

### Commands
- `/simpleinstructions editor` — Opens the visual editor.
- `/simpleinstructions test` — Runs a test of your current tutorial sequence.
- `/simpleinstructions reset` — Wipes your tutorial progress to start from step 1.
- `/simpleinstructions reload` — Reloads the configuration from your files.

## Global Settings

Access the global settings using Mod Menu, or by pressing *esc* and clicking the settings icon in the top right. Every setting has a hover tooltip explaining exactly what it does.

- **General:** Toggle the tutorial, show on world join, allow skipping, and manage progress resets.
- **Appearance:** Adjust global plaque width, scale, X/Y screen position, background opacity, font type, and toggle item icons.
- **Colors:** Hex color picker for descriptions, progress bars, and backgrounds. Affects all the steps.
- **Effects:** Adjust the timings for slide-in/out animations, configure the completion flash effect, and tweak completion sound volume.

## For Modpack Developers

While you can build everything in-game, your configurations and steps are saved directly to disk. (obviously)

- **Configs & Steps:** Saved to `config/simple_instructions/config.json`.
- **Player Progress:** Saved to `config/simple_instructions/completed.json`.

If you want to ship default instructions with a resource pack instead of the config folder, you can place a JSON file at:
`assets/simple_instructions/instructions/default.json`

### Custom Textures
The default plaque uses a 64x64 PNG with 9-slice rendering (6-pixel border). To replace it globally, override this path in a resource pack:
`assets/simple_instructions/textures/gui/instruction_plaque.png`

You can also define custom textures for individual steps inside the Plaque Editor under the visual overrides settings.

## Building from Source

Requires JDK 17.

`./gradlew build`

The output JAR will be located in `build/libs/`.

## License

All Rights Reserved