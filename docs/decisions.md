# Decisions

Owner decisions for BlueMapHUD, newest first.

| Date | Decision | Why |
| --- | --- | --- |
| 2026-11-07 | Themes: Classic (default, today's look), Galaxy, Desert and High Contrast. One JSON file per theme, the server picks the default (`hud.theme`), players pick their own with `/hud theme`, and every theme must pass the same contrast test. | Different servers and players want different looks, and some need high contrast. One data file per theme keeps adding or fixing one away from the code. |
| 2026-11-07 | The clock: Desert shows a dune and cactus with the sun or moon moving across the sky; Galaxy shows a small sun on a planet's orbit; time is 24-hour, such as "18:30". | Chosen by the owner from mockups. 24-hour time is the shortest and needs no letters. |
| 2026-11-07 | The clock changes every 5 in-game minutes by default (`hud.clock.step-minutes`). | The owner asked for the most realistic rate that causes no problems. Measured on Paper 26.3 with one player standing still: every minute sends 1.2 updates a second, about 2.0 KB/s with the minimap (281 B/s coordinates-only); every 5 minutes sends 0.23 a second, about 390 B/s (54 B/s); every 10, about 220 B/s. Server tick time didn't change at any step. Moving players already get frequent updates, so the cost only shows when standing still. Every 5 minutes changes about every 4 real seconds and costs a fifth of every minute. |
| 2026-11-07 | The clock is part of every theme rather than one server's branding, and players can hide it with `/hud hide clock`. | BlueMapHUD is a public plugin, so the server's look belongs in a theme it enables. |
| 2026-11-07 | Keep showing other players within 512 blocks for everyone (`players.radius`, `bluemaphud.seeplayers` on by default). | Different groups of players, such as children and adults, never share a world, so a wide radius doesn't show anyone's position to strangers. Vanished, spectating, invisible and BlueMap-hidden players stay hidden. |
| 2026-11-07 | Keep the name BlueMapHUD, lead the description, README and topics with "minimap", and say it's an unofficial add-on not affiliated with BlueMap's authors. | So people searching for a minimap find it, without implying it's part of BlueMap. |
| 2026-11-07 | Add the live test kit to the repo, after the 26.x change. | So the live tests can be rerun by anyone, without carrying 1.21.11 paths. |
| 2026-11-07 | Support Paper 26.1 and newer only: `api-version` 26.1, Java 25, `paper-plugin.yml`. Drop 1.21.11. | Every Minecraft plugin targets 26.1+ and is Paper-only, so there's one platform to test and maintain. |
| 2026-11-07 | Host the pack at a per-build address with `{sha1}` in `pack.public-url`, and check the hosted file on startup and reload, warning only. | Player tunnels carry only the game port, and a stale hosted pack would kick players because the pack is required. |
| 2026-11-07 | Coordinates stay hidden during boss fights, along with the minimap. | A second boss bar must never appear, and showing them on the action bar instead could flicker with other plugins. |
| 2026-11-07 | Add `hud.default-mode: map\|coords` to `config.yml`, defaulting to `map`. | A server replacing a coordinates plugin can start new players on coordinates only. A player's own `/hud mode` choice always wins. |
| 2026-11-06 | Add a coordinates-only mode (`/hud mode coords`) in the same single boss bar. | Players can keep coordinates without the map, so a server doesn't need a separate coordinates plugin. |
