# EasyBuild

[中文手册](README.md)

EasyBuild is a Paper plugin for Minecraft builders. It provides quick copy, paste, fill, curve-building, and random template-paste tools powered by FastAsyncWorldEdit (FAWE).

## Requirements

| Component | Requirement |
| --- | --- |
| Server | Paper 1.21.4 |
| Java | Java 21 |
| Dependency | FastAsyncWorldEdit (FAWE) |
| Build tool | Maven 3.9+ (only when building from source) |

> FAWE is declared as a hard dependency in `plugin.yml`. Install and start FAWE successfully before installing EasyBuild.

## Installation

### Build artifact

1. Build the project as described in “Build from source” below to produce `target/EasyBuild-1.0.jar`.
2. Copy the JAR to the Paper server's `plugins/` directory.
3. Ensure a FAWE version compatible with your server is also installed in `plugins/`.
4. Restart the server, then run `/kjjzgj` (or `/kj`) in-game to open the tool menu.

### Build from source

```powershell
mvn clean package
```

On success, the plugin JAR is created at `target/EasyBuild-1.0.jar`. The first build requires internet access to download the Paper API and FAWE dependencies.

## Quick start

1. Give builders the WorldEdit/FAWE permissions required by your server.
2. Make a WorldEdit selection with the wooden axe.
3. Run `/kj` and take the tool you need from the menu.
4. Hold the tool in your main hand and use it as described below.

Several features execute FAWE commands directly, including `//copy`, `//paste`, `//set`, and `//undo`. Availability, editable regions, and size limits therefore depend on the player's existing FAWE permissions and configuration.

## Commands

| Command | Alias | Purpose |
| --- | --- | --- |
| `/kjjzgj` | `/kj` | Open the quick-building tool menu |
| `/bc` | — | Use the current WorldEdit selection as a template and build it along recorded curve points |
| `/bcpos set <name>` | — | Record the block under the crosshair as a curve point; the argument is a placeholder |
| `/bcpos undo` | — | Remove the last recorded curve point |
| `/bcpos clearall` | — | Clear all recorded curve points |

## Tools

### Iron shovel: quick copy and paste

- Right-click: run `//copy -e`.
- Left-click: paste according to the current paste settings.
- Sneak + right-click: run `//rotate 90`.
- Sneak + left-click: run `//flip`.
- Press `F` (swap hands): run `//undo`.
- Press `Q` (drop): open the paste-settings menu.

The paste-settings menu can toggle paste at the clicked block, ignore air (`-a`), paste entities (`-e`), and select the pasted area (`-s`).

### Diamond shovel: quick fill

- Right-click: toggle quick-fill mode.
- While enabled, placing a block runs `//set <held block>` for the current WorldEdit selection.
- Press `F`: run `//undo`.

### Bone meal: quick tree planting

- Left-click: open the tree-type menu.
- Right-click a block: generate the selected tree above that block.

Tree generation does not use FAWE history and normally cannot be reverted with `//undo`.

### Golden shovel: curve builder

- Right-click blocks to record curve points in order; at least two points are required.
- Left-click to use the non-air blocks in the current WorldEdit selection as a cross-section template and build along the recorded curve.
- Press `F` to run `//undo`; sneak + `F` removes the most recent curve point.
- Press `Q` to clear every curve point.

First make a WorldEdit selection containing a cross-section or building fragment, then record the path and left-click to build it. Large or complex selections can modify many blocks, so test in a safe area first.

### Arrow: three-point curve parameter tool

- Normal right-click blocks: set P1, P3, and P2 in that order. Right-click once more after all three are set to clear them.
- Left-click: toggle between `n = 2` and `n = 3`.
- Sneak + right-click: set control point `nb`.
- Sneak + left-click: set control point `nf`.

This tool currently records curve parameters only; it does not place blocks directly.

### Stick: templates and random paste

- Right-click: run `//copy`.
- Sneak + right-click: save the current clipboard as a template under the selected tag, then enter the template name in chat.
- Left-click: randomly paste a template from the selected tag while ignoring air.
- Sneak + left-click: randomly paste without ignoring air.
- Press `F`: run `//undo`.
- Press `Q`: open template and tag management, where you can create tags, choose a tag, and save the current selection.

Templates are stored in the plugin data folder in per-tag subfolders. Back up the server's `plugins/Leaf/` directory (the exact folder is determined by the plugin name) to preserve custom templates.

## Troubleshooting

**The plugin does not load or reports a missing dependency:** verify that the server is Paper 1.21.4 and that FAWE is installed and enabled.

**A tool does nothing:** make sure it is the special item obtained from `/kj`, then check the player's WorldEdit/FAWE permissions.

**Pasting or saving a template fails:** create a WorldEdit selection and copy it first. Check the console for FAWE errors and verify server edit limits.

**Curve building fails:** record at least two curve points and create a WorldEdit selection containing non-air blocks.

## Development

Source code is in `src/main/java`; the plugin descriptor is at `src/main/resources/plugin.yml`. Before submitting changes, run:

```powershell
mvn clean package
```

## License

This repository does not currently declare a license. Do not assume redistribution or commercial-use rights until a license is added.
