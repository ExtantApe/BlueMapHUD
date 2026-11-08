package com.extantape.bluemaphud.hud;

import com.extantape.bluemaphud.config.HudConfig;
import com.extantape.bluemaphud.markers.MarkerService;
import com.extantape.bluemaphud.render.FontLayout;
import com.extantape.bluemaphud.render.HudFonts;
import com.extantape.bluemaphud.render.HudIcon;
import com.extantape.bluemaphud.render.MapComposer;
import com.extantape.bluemaphud.render.MapFrame;
import com.extantape.bluemaphud.render.ReadoutFont;
import com.extantape.bluemaphud.render.TinyFont;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Turns a snapshot into a state key and, when that key changes, a frame. Safe off the main thread. */
public final class FrameBuilder {
    private static final int COLOR_PLAYER = 0x66D9FF;
    private static final int COLOR_SELF = 0xFFFFFF;
    private static final int COLOR_POI = 0xFFC845;
    private static final int COLOR_DEATH = 0xFF5B5B;
    private static final int COLOR_TEXT = 0xFFFFFF;
    private static final int COLOR_DIM = 0xBEC6D2;
    /** Pixels between a plate's edge and its text. */
    private static final int PLATE_PADDING = 3;
    private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private final HudConfig config;
    private final HudFonts fonts;
    private final int[] iconAdvances;
    private final MapComposer composer;
    private final MarkerService markers;

    public record Built(Component component, long estimatedBytes) {}

    public FrameBuilder(HudConfig config, HudFonts fonts, int[] iconAdvances, MapComposer composer, MarkerService markers) {
        this.config = config;
        this.fonts = fonts;
        this.iconAdvances = iconAdvances;
        this.composer = composer;
        this.markers = markers;
    }

    private int width(Snapshot s) { return s.fullscreen() ? config.fullscreen().width() : s.size(); }
    private int height(Snapshot s) { return s.fullscreen() ? config.fullscreen().height() : s.size(); }

    /** A number that stays the same exactly when a new frame would look the same. */
    public long stateKey(Snapshot s) {
        if (s.coordsOnly()) {
            long key = 1;
            key = key * 31 + s.blockX();
            key = key * 31 + s.blockY();
            key = key * 31 + s.blockZ();
            key = key * 31 + compassIndex(s.yaw());
            key = key * 31 + s.offsetX();
            key = key * 31 + s.offsetY();
            return key;
        }
        int bpc = s.zoom();
        int width = width(s), height = height(s);
        int cellX = Math.floorDiv(s.blockX(), bpc), cellZ = Math.floorDiv(s.blockZ(), bpc);
        long key = s.mapId().hashCode();
        key = key * 31 + (s.fullscreen() ? 1 : 0);
        key = key * 31 + (s.pattern() ? 1 : 0);
        key = key * 31 + bpc;
        key = key * 31 + width;
        key = key * 31 + cellX;
        key = key * 31 + cellZ;
        key = key * 31 + HudIcon.headingForYaw(s.yaw());
        key = key * 31 + s.offsetX();
        key = key * 31 + s.offsetY();
        key = key * 31 + (s.showCoords() || s.fullscreen() ? (s.blockX() * 31L + s.blockY()) * 31L + s.blockZ() : 0);
        key = key * 31 + (s.showMarkers() ? 1 : 0);
        for (int i = 0; i < s.otherPlayers().length; i++) {
            key = key * 31 + Math.floorDiv(s.otherPlayers()[i], bpc);
        }
        if (s.death() != null) key = key * 31 + Arrays.hashCode(s.death());
        if (s.showMarkers()) key = key * 31 + markers.pois(s.mapId()).hashCode();
        if (!s.pattern()) {
            key = key * 31 + composer.chunkFingerprint(s.mapId(), bpc, cellX - width / 2, cellZ - height / 2, width, height);
        }
        return key;
    }

    /** Builds a frame, reusing the session's grid buffer. */
    public Built build(Snapshot s, int[] grid) {
        if (s.coordsOnly()) return buildCoordsOnly(s);
        boolean full = s.fullscreen();
        FontLayout layout = full ? fonts.fullscreen : fonts.minimap;
        int width = width(s), height = height(s);
        int bpc = s.zoom();
        int centerCellX = Math.floorDiv(s.blockX(), bpc), centerCellZ = Math.floorDiv(s.blockZ(), bpc);
        int firstX = centerCellX - width / 2, firstZ = centerCellZ - height / 2;

        if (s.pattern()) fillPattern(grid, width, height);
        else composer.fill(grid, s.mapId(), bpc, firstX, firstZ, width, height, full ? config.fullscreen().brightness() : 1.0);

        int offsetRows = full ? 0 : Math.min(fonts.maxOffsetRows, s.offsetY() / layout.step());
        MapFrame frame = new MapFrame(layout, iconAdvances, width, height, full ? 0 : s.offsetX(), offsetRows);
        frame.drawGrid(grid);

        // North marker in the top-left corner of the map.
        frame.drawIcon(HudIcon.NORTH, 4 / layout.step() + 1, 4 / layout.step() + 1, COLOR_TEXT);

        int count = 0;
        if (s.showMarkers() && !s.pattern()) {
            List<MarkerService.Poi> pois = markers.pois(s.mapId());
            for (MarkerService.Poi poi : pois) {
                count += placeMarker(frame, layout, poi.x(), poi.z(), HudIcon.POI, COLOR_POI, config.markers().edgeArrows(),
                        full ? poi.label() : null, firstX, firstZ, width, height, bpc);
            }
            int[] death = s.death();
            if (death != null) {
                count += placeMarker(frame, layout, death[0], death[1], HudIcon.DEATH, COLOR_DEATH, config.markers().edgeArrows(),
                        null, firstX, firstZ, width, height, bpc);
            }
        }

        int[] others = s.otherPlayers();
        for (int i = 0; i + 1 < others.length; i += 2) {
            placeMarker(frame, layout, others[i], others[i + 1], HudIcon.PLAYER, COLOR_PLAYER, config.players().edgeArrows(),
                    null, firstX, firstZ, width, height, bpc);
        }

        frame.drawIcon(HudIcon.headingForYaw(s.yaw()), width / 2, height / 2, COLOR_SELF);

        int textRow = height + HudFonts.readoutRowOffset(layout);
        if (full || s.showCoords()) drawReadout(frame, s, width * layout.step(), textRow);
        if (full) drawLegend(frame, s, width * layout.step(), others.length / 2, count, textRow);
        return new Built(frame.finish(), frame.estimatedBytes());
    }

    /** The coordinate line alone, where it would sit under a map with no rows. Moves with the player's offset. */
    private Built buildCoordsOnly(Snapshot s) {
        FontLayout layout = fonts.minimap;
        int offsetRows = Math.min(fonts.maxOffsetRows, s.offsetY() / layout.step());
        MapFrame frame = new MapFrame(layout, iconAdvances, 0, 0, s.offsetX(), offsetRows);
        drawReadout(frame, s, 0, HudFonts.readoutRowOffset(layout));
        return new Built(frame.finish(), frame.estimatedBytes());
    }

    /** Draws a marker if it's on the map, or an arrow on the edge pointing at it. Returns 1 if it drew. */
    private int placeMarker(MapFrame frame, FontLayout layout, int blockX, int blockZ, int icon, int color, boolean edgeArrow,
                            String label, int firstX, int firstZ, int width, int height, int bpc) {
        int x = Math.floorDiv(blockX, bpc) - firstX;
        int z = Math.floorDiv(blockZ, bpc) - firstZ;
        if (x >= 0 && z >= 0 && x < width && z < height) {
            frame.drawIcon(icon, x, z, color);
            if (label != null && !label.isEmpty()) {
                String text = label.toUpperCase(Locale.ROOT);
                frame.drawText(text, (x + 3) * layout.step(), z, COLOR_TEXT);
            }
            return 1;
        }
        if (!edgeArrow) return 0;
        double dx = x - width / 2.0, dz = z - height / 2.0;
        double scale = Math.min((width / 2.0 - 2) / Math.max(Math.abs(dx), 1e-6), (height / 2.0 - 2) / Math.max(Math.abs(dz), 1e-6));
        frame.drawIcon(HudIcon.headingForVector(dx, dz), (int) (width / 2.0 + dx * scale), (int) (height / 2.0 + dz * scale), color);
        return 1;
    }

    /** Draws "X Y Z facing", centred under the map on a plate. */
    private void drawReadout(MapFrame frame, Snapshot s, int mapWidth, int textRow) {
        String position = s.blockX() + " " + s.blockY() + " " + s.blockZ() + " ";
        String facing = compass(s.yaw());
        int width = ReadoutFont.width(position + facing);
        int x = (mapWidth - width) / 2;
        frame.drawPlate(x - PLATE_PADDING, width - 1 + PLATE_PADDING * 2, textRow);
        int end = frame.drawReadout(position, x, textRow, COLOR_TEXT);
        frame.drawReadout(facing, end, textRow, COLOR_DIM);
    }

    /** Draws the zoom at the left of the map, and the counts at the right, on plates of their own. */
    private void drawLegend(MapFrame frame, Snapshot s, int mapWidth, int players, int markerCount, int textRow) {
        String zoom = "1:" + s.zoom();
        String counts = "PLAYERS " + players + "  MARKERS " + markerCount;
        frame.drawPlate(0, TinyFont.width(zoom) - 1 + PLATE_PADDING * 2, textRow);
        frame.drawText(zoom, PLATE_PADDING, textRow, COLOR_DIM);
        int countsWidth = TinyFont.width(counts) - 1 + PLATE_PADDING * 2;
        frame.drawPlate(mapWidth - countsWidth, countsWidth, textRow);
        frame.drawText(counts, mapWidth - countsWidth + PLATE_PADDING, textRow, COLOR_DIM);
    }

    private static String compass(float yaw) {
        return COMPASS[compassIndex(yaw)];
    }

    private static int compassIndex(float yaw) {
        return Math.floorMod((int) Math.round((yaw + 180.0) / 45.0), COMPASS.length);
    }

    /** A test pattern for checking crisp colours, alignment and the invisible bar. */
    private static void fillPattern(int[] grid, int width, int height) {
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                boolean border = x == 0 || z == 0 || x == width - 1 || z == height - 1;
                int red = x * 255 / Math.max(1, width - 1);
                int green = z * 255 / Math.max(1, height - 1);
                int blue = (x + z) % 2 == 0 ? 255 : 0;
                grid[z * width + x] = border ? 0xFFFFFF : red << 16 | green << 8 | blue;
            }
        }
    }
}
