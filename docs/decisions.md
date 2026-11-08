# Decisions

Owner decisions for BlueMapHUD, newest first.

| Date | Decision | Why |
| --- | --- | --- |
| 2026-11-07 | Keep the name BlueMapHUD, lead the description, README and topics with "minimap", and say it's an unofficial add-on not affiliated with BlueMap's authors. | So people searching for a minimap find it, without implying it's part of BlueMap. |
| 2026-11-07 | Add the live test kit to the repo, after the 26.x change. | So the live tests can be rerun by anyone, without carrying 1.21.11 paths. |
| 2026-11-07 | Support Paper 26.1 and newer only: `api-version` 26.1, Java 25, `paper-plugin.yml`. Drop 1.21.11. | Every Minecraft plugin targets 26.1+ and is Paper-only, so there's one platform to test and maintain. |
| 2026-11-07 | Host the pack at a per-build address with `{sha1}` in `pack.public-url`, and check the hosted file on startup and reload, warning only. | Player tunnels carry only the game port, and a stale hosted pack would kick players because the pack is required. |
