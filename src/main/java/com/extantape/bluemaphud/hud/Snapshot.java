package com.extantape.bluemaphud.hud;

/**
 * Everything a frame needs, copied on the main thread so that background threads never touch Bukkit.
 *
 * @param otherPlayers flat x, z pairs of the players this viewer may see
 * @param death        the viewer's last death position on this map as {x, z}, or null
 * @param coordsOnly   draw only the coordinate line, which needs no map, so {@code mapId} may be empty
 */
public record Snapshot(
        String mapId, int blockX, int blockY, int blockZ, float yaw, boolean fullscreen,
        int zoom, int size, int offsetX, int offsetY,
        boolean showCoords, boolean showMarkers,
        int[] otherPlayers, int[] death, boolean pattern, boolean coordsOnly
) {}
