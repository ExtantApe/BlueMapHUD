# BlueMapHUD

A minimap and coordinates HUD for Paper, drawn from [BlueMap](https://bluemap.bluecolored.de/)'s tiles, with a fullscreen map too. It uses a small resource pack with its own namespace: no client mods, no shader overrides and no held map items.

It's an unofficial add-on for BlueMap and isn't affiliated with BlueMap's authors.

**Status:** pre-release. It has passed a full test suite on live Paper 26.1.2, 26.2 and 26.3 servers with real BlueMap tiles, using a protocol bot. It hasn't been seen in a real game client yet. See [Testing](#testing).

- Requires Paper 26.1 or newer, Java 25, and a BlueMap version that matches your server (see below). Java Edition only. Paper 1.21.11 and older aren't supported.
- Works alongside other resource packs, because it never overrides a shader and only adds its own files.

| Server | BlueMap | Java |
| --- | --- | --- |
| Paper 26.1.2 | 5.20 or newer | 25 |
| Paper 26.2 | 5.22 or newer (skip 5.21) | 25 |
| Paper 26.3 | 5.24 or newer | 25 |

These minimums come from BlueMap's release notes. Only BlueMap 5.29 was tested, on all three. Paper's 26.1 line has no 26.1.0 build, and 26.1.1 only had alpha builds, so 26.1.2 is the one tested.

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
| `/hud mode coords\|map` | `coords` shows only the X, Y, Z and facing line, in the same bar, and works in worlds BlueMap doesn't map. `map` brings the minimap back. Both turn the HUD on. New players start in `hud.default-mode`, which is `map` unless the config says `coords`. |
| `/bmhud help` | Shows help. |
| `/bmhud reload` | Reloads the config, and re-sends the pack if it changed. |
| `/bmhud debug` | Shows cache sizes, build times and update sizes. |
| `/bmhud debug dump` | Writes your view to a PNG to compare with BlueMap. |
| `/bmhud debug pattern` | Shows a test pattern. |
| `/bmhud pack export` | Shows where the pack zip is, its SHA-1, and the address players are sent. |

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

**Hosting it elsewhere.** Set `pack.public-url` to where you upload `plugins/BlueMapHUD/pack/bluemaphud-pack.zip`. Upload it unchanged, because re-zipping changes its SHA-1 and clients reject a pack whose hash doesn't match. `{sha1}` in the address is replaced with the pack's hash, such as `https://example.com/packs/{sha1}.zip`, so the address changes whenever the pack does. The same settings and plugin version always build the same hash. On startup and `/bmhud reload` the plugin downloads the address once in the background and warns, with both hashes, if it isn't the current pack. It never blocks startup. If the server can't reach the address but players can, set `pack.verify-public-url: false`. `/bmhud pack export` shows the path, the hash and the address players are sent.

**Using a merged pack.** Set `pack.send: false`, run `/bmhud pack export`, and merge that zip into your server's pack. On startup the plugin warns if its files are missing.

**Custom icons.** Put 9x9 PNGs in `plugins/BlueMapHUD/icons/` and run `/bmhud reload`. The names are `arrow_00.png` to `arrow_15.png` (clockwise from north), `player.png`, `poi.png`, `death.png` and `north.png`. Draw them in white, because the plugin tints them.

## Known limits

- **Screen position:** the server can't see a player's screen size, so the map starts at top centre. Players can move it with `/hud offset` or `/hud corner`.
- **Other boss bars:** the minimap hides for Adventure boss bars, registered Bukkit bars and the Wither and Ender Dragon. A bar made with Bukkit's unregistered `createBossBar` can't be detected, so the minimap stays up and the two share the screen.
- **Coordinates:** the coordinate line is part of the minimap, so it hides whenever the minimap does, including during boss fights and with `/hud off`. In `/hud mode coords` it follows the same rules, apart from also showing where BlueMap has no map. The fullscreen map always shows it.
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

`./gradlew build` compiles against the Paper 26.1.2 API, the oldest supported, and runs the tests. They include a simulated client that loads the generated pack and draws frames using the font rules read from the Minecraft 26.3 client: glyph advance, the `ascent <= height` check and text centring. It checks that every map cell lands on the right pixels and that no frame drifts.

The plugin has also been tested on live Paper 26.1.2, 26.2 and 26.3 servers with BlueMap 5.29. A protocol bot played the client. It speaks the 1.21.11 protocol and joined through ViaBackwards, so the packets were translated rather than sent by a real 26.x client. Every frame it received was decoded and compared cell by cell with BlueMap's tile files on disk.

**Passed on all three servers:**

- **Map accuracy:** every zoom level, every size, all three dimensions, negative coordinates, unrendered areas and the fullscreen map. Re-rendered tiles reach the HUD within the refresh time.
- **Icons and text:** the X, Y, Z and facing readout, heading arrows at all 8 compass points, player dots, POI markers and labels (including accents, CJK and very long labels), edge arrows, the death point and coordinates.
- **One boss bar:** the map and coordinates share a single bar. A second never appeared through every `/hud` option, fullscreen, sneak + F, reloads, pack re-sends, auto-hide and a full `/reload`.
- **Hiding:** vanish, `hidePlayer`, spectators, invisibility, players hidden in BlueMap, the radius, other dimensions, permissions, other boss bars, the Wither, the Ender Dragon and every `auto-hide` setting.
- **Updates:** nothing is sent while idle, and at most 4 updates a second otherwise.
- **Pack handling:** required, optional, declined and failed downloads, `host`, `public-url` with and without `{sha1}`, the hosted-pack check (matching, stale, missing, unreachable, never answering, invalid and turned off), `send: false`, bar colours, custom icons, and another plugin wiping packs with the old one-pack method.
- **Life cycle:** `/bmhud reload`, `/bluemap reload`, a full `/reload`, server restarts, saved settings and quick join/quits.
- **Bad data:** corrupt tiles, a wrong web root, an unreachable webserver and the far edge of the world.
- **Other setups:** the action bar channel, the HTTP source, and command back-off from another plugin's `plugin.yml` commands.

**Also passed:**

- **SQL storage:** on 26.3, tiles held in SQLite through the HTTP source matched the same world rendered to files on all 1024 cells.
- **Startup:** BlueMap loaded on all 8 of 8 restarts of a 26.3 server with BlueMapHUD installed.
- **Game code:** the Minecraft 26.1.2, 26.2 and 26.3 client code loads the generated pack and fonts, with the game's advances matching this project's for all 13,527 glyphs. The 26.1, 26.1.1 and 26.1.2 clients all use resource pack format 84, the pack's minimum.

**20 moving players, one minute, on real terrain:**

| | Paper 26.1.2 | Paper 26.2 | Paper 26.3 | Budget |
| --- | --- | --- | --- | --- |
| Average frame build | 73 µs | 84 µs | 88 µs | 1 ms |
| Update size, compressed (95th percentile) | 2.3 KB | 2.3 KB | 2.3 KB | 5 KB |
| Largest update, compressed | 2.6 KB | 2.6 KB | 2.6 KB | none |
| Server tick | 12.1 ms | 11.5 ms | 7.9 ms | 50 ms |

These ran on a shared machine with other test servers running, so tick times vary with its load. One 26.3 run, with the machine's load average at 55, dropped to 18.9 TPS while its tick stayed at 18 ms; a rerun at lower load held 20 TPS.

**Not tested yet:**

- A real game client: how the map actually looks, the invisible boss bar, icons drawing over the map, GUI scales and screen widths, and Sodium and Iris with a shader pack.
- The real Magic plugin. Its pack call was simulated with the same Bukkit method it uses.
- ZigZag loaded alongside the pack in a client. It overlaps only on the pink boss bar textures, which this pack replaces.
- A Bedrock join, including Floodgate detection through `paper-plugin.yml`'s classpath sharing, MySQL or PostgreSQL storage, and back-off from another plugin's Brigadier commands.

To check a real client, join as an op and run `/bmhud debug pattern`. You should see a sharp colour gradient with a white border and no bar behind it.

## Building

```bash
./gradlew build
```

The jar is written to `build/libs/`. Java 25 is needed to build and run it. It's a Paper plugin (`paper-plugin.yml`), so it won't load on Spigot. [docs/26x-audit.md](docs/26x-audit.md) records what changed when 1.21.11 support was dropped.

## License

MIT. See [LICENSE](LICENSE).
