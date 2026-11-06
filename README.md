# BlueMapHUD

A lightweight Paper plugin that shows a [BlueMap](https://bluemap.bluecolored.de/)-sourced minimap and fullscreen map on the player's screen. It uses a small resource pack with its own namespace: no client mods, no shader overrides and no held map items.

**Status:** pre-release. It has passed a full test suite on live Paper 1.21.11 and 26.3 servers with real BlueMap tiles, using a protocol bot. It hasn't been seen in a real game client yet. See [Testing](#testing).

- Requires Paper 1.21.11 or newer, and a BlueMap version that matches your server (see below). Java Edition only.
- Works alongside other resource packs, because it never overrides a shader and only adds its own files.

| Server | BlueMap | Java |
| --- | --- | --- |
| Paper 1.21.11 | 5.18 (5.19 and newer don't load on 1.21.11) | 25 (BlueMap 5.18 needs it) |
| Paper 26.x | 5.20 or newer | 25 |

## Install

1. Put `BlueMapHUD-<version>.jar` in `plugins/`, next to BlueMap.
2. Open port `8164` so players can download the pack, or set `pack.public-url` to host it elsewhere. Servers sharing a machine each need their own `pack.port`.
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
| `/hud hide\|show coords\|players\|markers` | Toggles one part of the map. Coordinates show X, Y, Z and facing. |
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
- **Coordinates:** the coordinate line is part of the minimap, so it hides whenever the minimap does, including during boss fights and with `/hud off`. The fullscreen map always shows it.
- **Vertical position:** `/hud offset y` moves in whole map cells, up to `hud.max-offset-y`.
- **Action bar mode:** the map sits above the hotbar and can flicker if another plugin also uses the action bar. The fullscreen map still uses a boss bar.
- **Other pink boss bars:** the pack makes one boss bar colour transparent, so another plugin's bar in that colour disappears too. Choose a colour with `pack.boss-bar-color` that nothing else uses.
- **Repeated pack wipes:** the pack is re-sent once per session. If another plugin clears packs again, the map stays hidden for that player until they rejoin. This is what stops two plugins fighting in a loop.
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

The plugin has also been tested on live servers: Paper 1.21.11 with BlueMap 5.18, and Paper 26.3 with BlueMap 5.29. A protocol bot played the client, joining 26.3 through ViaBackwards. Every frame it received was decoded and compared cell by cell with BlueMap's tile files on disk.

**Passed on both servers:**

- **Map accuracy:** every zoom level, every size, all three dimensions, negative coordinates, unrendered areas and the fullscreen map. Re-rendered tiles reach the HUD within the refresh time.
- **Icons and text:** the X, Y, Z and facing readout, heading arrows at all 8 compass points, player dots, POI markers and labels (including accents, CJK and very long labels), edge arrows, the death point and coordinates.
- **One boss bar:** the map and coordinates share a single bar. A second never appeared through every `/hud` option, fullscreen, sneak + F, reloads, pack re-sends, auto-hide and a full `/reload`.
- **Hiding:** vanish, `hidePlayer`, spectators, invisibility, players hidden in BlueMap, the radius, other dimensions, permissions, other boss bars, the Wither, the Ender Dragon and every `auto-hide` setting.
- **Updates:** nothing is sent while idle, and at most 4 updates a second otherwise.
- **Pack handling:** required, optional, declined and failed downloads, `host`, `public-url`, `send: false`, bar colours, custom icons, and another plugin wiping packs with the old one-pack method.
- **Life cycle:** `/bmhud reload`, `/bluemap reload`, a full `/reload`, server restarts, saved settings and quick join/quits.
- **Bad data:** corrupt tiles, a wrong web root, an unreachable webserver and the far edge of the world.
- **Other setups:** the action bar channel, the HTTP source, and command back-off from another plugin's `plugin.yml` commands.

**Also passed:**

- **SQL storage:** on 1.21.11, tiles held in SQLite through the HTTP source matched the same world rendered to files on all 1024 cells.
- **Game code:** the Minecraft 26.1.2, 26.2 and 26.3 client code loads the generated pack and fonts, with the game's advances matching this project's for all 13,527 glyphs. The 1.21.11 client's bytecode uses the same glyph advance formula.

**20 moving players, one minute, on real terrain:**

| | Paper 1.21.11 | Paper 26.3 | Budget |
| --- | --- | --- | --- |
| Average frame build | 55 µs | 45 µs | 1 ms |
| Update size, compressed (95th percentile) | 2.3 KB | 2.3 KB | 5 KB |
| Largest update, compressed | 2.6 KB | 2.6 KB | none |
| Server tick | 6.5 ms | 6.1 ms | 50 ms |

**Not tested yet:**

- A real game client: how the map actually looks, the invisible boss bar, icons drawing over the map, GUI scales and screen widths, Sodium and Iris with a shader pack, and native 26.1 and 26.2 servers.
- The real Magic plugin. Its pack call was simulated with the same Bukkit method it uses.
- ZigZag loaded alongside the pack in a client. It overlaps only on the pink boss bar textures, which this pack replaces.
- A Bedrock join, MySQL or PostgreSQL storage, and back-off from another plugin's Brigadier commands.

To check a real client, join as an op and run `/bmhud debug pattern`. You should see a sharp colour gradient with a white border and no bar behind it.

## Building

```bash
./gradlew build
```

The jar is written to `build/libs/`. Java 21 is needed to build it, and the jar runs on Java 21 and 25.

## License

MIT. See [LICENSE](LICENSE).
