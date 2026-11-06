# BlueMapHUD

A lightweight Paper plugin that shows a [BlueMap](https://bluemap.bluecolored.de/)-sourced minimap and fullscreen map on the player's screen. It uses a small resource pack with its own namespace: no client mods, no shader overrides and no held map items.

**Status:** pre-release. The build and tests pass, but it hasn't been run on a live server with a real client yet. See [Testing](#testing).

- Requires Paper 1.21.11 or newer, and BlueMap 5.15 or newer. Java Edition only.
- Works alongside other resource packs, because it never overrides a shader and only adds its own files.

## Install

1. Put `BlueMapHUD-<version>.jar` in `plugins/`, next to BlueMap.
2. Open port `8164` so players can download the pack, or set `pack.public-url` to host it elsewhere.
3. Start the server. Settings are in `plugins/BlueMapHUD/config.yml`.

If BlueMap stores its maps in SQL, set `tiles.source: http`.

## Commands

| Command | What it does |
| --- | --- |
| `/map` | Opens or closes the fullscreen map. Sneak + F does the same. |
| `/map zoom <n\|in\|out>` | Sets the fullscreen zoom. |
| `/hud`, `/hud on`, `/hud off` | Toggles the minimap. The choice is saved per player. |
| `/hud zoom <n\|in\|out>` | Sets blocks per cell: 1, 2, 4, 8, 16, 32 or 64. |
| `/hud size small\|medium\|large` | Sets the size to 24, 32 or 40 cells. |
| `/hud offset <x> <y>` | Moves the minimap, in GUI pixels from top centre. |
| `/hud corner left\|right\|center` | Moves it to a rough preset position. |
| `/hud hide\|show coords\|players\|markers` | Toggles one part of the map. |
| `/bmhud help` | Shows help. |
| `/bmhud reload` | Reloads the config, and re-sends the pack if it changed. |
| `/bmhud debug` | Shows cache sizes, build times and update sizes. |
| `/bmhud debug dump` | Writes your view to a PNG to compare with BlueMap. |
| `/bmhud debug pattern` | Shows a test pattern. |
| `/bmhud pack export` | Shows where the pack zip is, for merging. |

If another plugin owns a name, BlueMapHUD backs off: `/map` to `/worldmap`, then `/bmhud map`, and `/hud` to `/minimap`, then `/bmhud hud`. It logs which names it got. `/bmhud map`, `/bmhud hud`, `/bluemaphud:map` and `/bluemaphud:hud` always work. New names apply after a restart.

## Permissions

| Permission | Default | Allows |
| --- | --- | --- |
| `bluemaphud.use` | everyone | Seeing the map at all. Removing it hides everything for that group. |
| `bluemaphud.fullscreen` | everyone | The fullscreen map. |
| `bluemaphud.seeplayers` | everyone | Other players on the map. |
| `bluemaphud.admin` | ops | `/bmhud reload`, `debug` and `pack`. |

## Resource packs

The plugin sends its pack alongside any others using the multi-pack API, and players don't see a map until their client confirms it loaded. Its only file outside its own namespace is a transparent boss bar texture, in pink by default. If another pack restyles that colour, set `pack.boss-bar-color` to one it leaves alone.

**Using it with Magic.** Magic sends its pack with Bukkit's older one-pack method, which clears every other server pack. BlueMapHUD re-sends its pack once when that happens, but it's better to stop the conflict. In `plugins/Magic/config.yml` set `resource_pack: ""`, then add Magic's pack to your main pack.

**Using a merged pack.** Set `pack.send: false`, run `/bmhud pack export`, and merge that zip into your server's pack. On startup the plugin warns if its files are missing.

**Custom icons.** Put 9x9 PNGs in `plugins/BlueMapHUD/icons/` and run `/bmhud reload`. The names are `arrow_00.png` to `arrow_15.png` (clockwise from north), `player.png`, `poi.png`, `death.png` and `north.png`. Draw them in white, because the plugin tints them.

## Known limits

- **Screen position:** the server can't see a player's screen size, so the map starts at top centre. Players can move it with `/hud offset` or `/hud corner`.
- **Other boss bars:** the minimap hides for Adventure boss bars, registered Bukkit bars and the Wither and Ender Dragon. A bar made with Bukkit's unregistered `createBossBar` can't be detected, so the minimap stays up and the two share the screen.
- **Vertical position:** `/hud offset y` moves in whole map cells, up to `hud.max-offset-y`.
- **Action bar mode:** the map sits above the hotbar and can flicker if another plugin also uses the action bar. The fullscreen map still uses a boss bar.
- **Bedrock players:** Geyser and Floodgate players don't get a map, because they can't use the pack.

## How it works

Each map cell is one or more characters in a generated font, coloured with hex text colour and with the shadow turned off, so a whole map is a single text component shown as a boss bar title. Colours come from BlueMap's low-res tiles, which are shrunk once and cached in 64x64 cell chunks by two background threads.

Every 5 ticks the main thread copies each player's state, and background threads compare it with the last frame. If nothing visible changed, nothing is sent.

Measured in the test suite on a synthetic map:

| | Result | Budget |
| --- | --- | --- |
| Minimap frame build | about 40 µs | 1 ms |
| Minimap update, 32x32 with 20 players, compressed | about 1.7 KB | 5 KB |
| Fullscreen update, 96x54, compressed | about 4.5 KB | none |

Real terrain has more colour variation than the test map, so expect larger updates in play. `/bmhud debug` reports sizes before compression.

## Testing

`./gradlew build` compiles against the Paper 1.21.11 API and runs the tests. They include a simulated client that loads the generated pack and draws frames using the font rules read from the Minecraft 26.3 client: glyph advance, the `ascent <= height` check and text centring. It checks that every map cell lands on the right pixels and that no frame drifts.

It's a simulation, so these still need a live check:

- Clients 1.21.11, 26.1.2, 26.2 and 26.3, each on vanilla, Sodium, and Sodium + Iris with a shader pack.
- GUI scales 2, 3, 4 and Auto, at widths 1280, 1920 and 2560.
- ZigZag on the client, and sent by the server.
- Magic's pack sent before and after ours, with a Magic spell or boss bar showing.
- Accepting and declining a required pack, and a Bedrock join.
- BlueMap on file storage and on SQL storage.
- Command back-off when `/map` and `/hud` are already taken.

## Building

```bash
./gradlew build
```

The jar is written to `build/libs/`. Java 21 is needed to build it, and it also runs on the Java 25 servers that 26.x uses.

## License

MIT. See [LICENSE](LICENSE).
