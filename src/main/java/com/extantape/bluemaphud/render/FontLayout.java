package com.extantape.bluemaphud.render;

import net.kyori.adventure.key.Key;

/**
 * One generated font: a stack of rows, each with its own vertical position in the font file.
 *
 * <p>These numbers come from the client's own code (checked against the 26.3 jar):
 * <ul>
 *   <li>A bitmap glyph's top is drawn at {@code textY + 7 - ascent}.</li>
 *   <li>Its advance is {@code (int)(0.5 + litWidth * scale) + 1}, so every glyph leaves a one pixel gap
 *       that needs a one pixel step back to close.</li>
 *   <li>A provider is rejected unless {@code ascent <= height}. Rows above the text baseline therefore
 *       use tall cells padded with transparency.</li>
 * </ul>
 */
public final class FontLayout {
    /** Run lengths are powers of two up to this many cells. */
    public static final int RUN_LENGTHS = 7;
    public static final int MAX_RUN = 1 << (RUN_LENGTHS - 1);
    /** Space between the action bar text line and the bottom of the map. */
    private static final int ACTION_BAR_GAP = 12;
    /** Keeps boss bar rows below the point where {@code ascent <= height} stops holding. */
    private static final int BOSS_BAR_TOP = 8;

    private static final int SPACE_BASE = 0xE000;
    private static final int RUN_BASE = 0xE100;
    private static final int SHEET_BASE = 0xF0000;

    public enum Direction { DOWN_FROM_TOP, UP_FROM_BOTTOM }

    private final String name;
    private final Key font;
    private final int layoutIndex;
    private final int step;
    private final int rows;
    private final int reservedRows;
    private final Direction direction;

    /** @param reservedRows rows kept free below the grid for the coordinate line */
    public FontLayout(String name, int layoutIndex, int step, int rows, int reservedRows, Direction direction) {
        this.name = name;
        this.font = Key.key("bluemaphud", name);
        this.layoutIndex = layoutIndex;
        this.step = step;
        this.rows = rows;
        this.reservedRows = reservedRows;
        this.direction = direction;
    }

    public String name() { return name; }
    public Key font() { return font; }
    public int step() { return step; }
    public int rows() { return rows; }

    /** Pixels below the text line's origin where this row starts. */
    public int rowTop(int row) {
        return direction == Direction.DOWN_FROM_TOP
                ? BOSS_BAR_TOP + row * step
                : -ACTION_BAR_GAP - (rows - row) * step;
    }

    /**
     * The row holding a grid line. Boss bar maps hang down from the top and move down with the offset.
     * Action bar maps sit on the text line and move up with it.
     */
    public int rowFor(int gridRow, int gridRows, int offsetRows) {
        return direction == Direction.DOWN_FROM_TOP ? gridRow + offsetRows : rows - reservedRows - gridRows - offsetRows + gridRow;
    }

    public int mapAscent(int row) { return 7 - rowTop(row); }
    /** An icon is centred on its row, so it lines up with the map cell it belongs to. */
    public int iconAscent(int row) { return 7 - (rowTop(row) - HudIcon.SIZE / 2 + step / 2); }
    /** Tiny text is one pixel lower, so it sits centred in a row of readout text. */
    public int textAscent(int row) { return 7 - (rowTop(row) + 1); }
    public int readoutAscent(int row) { return 7 - rowTop(row); }
    public int plateAscent(int row) { return 7 - (rowTop(row) - PLATE_MARGIN); }

    /** The smallest cell height, in cells of {@code scale} pixels, that keeps {@code ascent <= height}. */
    public static int paddedCells(int maxAscent, int scale, int minimum) {
        return Math.max(minimum, (int) Math.ceil(maxAscent / (double) scale));
    }

    public int maxMapAscent() { return Math.max(mapAscent(0), mapAscent(rows - 1)); }
    public int maxIconAscent() { return Math.max(iconAscent(0), iconAscent(rows - 1)); }
    public int maxTextAscent() { return Math.max(textAscent(0), textAscent(rows - 1)); }
    public int maxReadoutAscent() { return Math.max(readoutAscent(0), readoutAscent(rows - 1)); }
    public int maxPlateAscent() { return Math.max(plateAscent(0), plateAscent(rows - 1)); }

    /** The run glyph of {@code 1 << lengthIndex} cells on a row. */
    public int runChar(int row, int lengthIndex) {
        return RUN_BASE + layoutIndex * 0x1000 + row * RUN_LENGTHS + lengthIndex;
    }

    /** Plates are powers of two up to this many pixels wide. */
    public static final int PLATE_LENGTHS = 8;
    /** A plate reaches this far above and below the readout's text. */
    public static final int PLATE_MARGIN = 2;
    public static final int PLATE_HEIGHT = ReadoutFont.HEIGHT + PLATE_MARGIN * 2;

    private static final int SHEET_ROW_STRIDE = HudIcon.COUNT + TinyFont.glyphCount() + ReadoutFont.glyphCount() + PLATE_LENGTHS;
    private static final int LAYOUT_STRIDE = 0x8000;

    public int iconChar(int row, int icon) {
        return SHEET_BASE + layoutIndex * LAYOUT_STRIDE + row * SHEET_ROW_STRIDE + icon;
    }

    public int textChar(int row, int glyph) {
        return SHEET_BASE + layoutIndex * LAYOUT_STRIDE + row * SHEET_ROW_STRIDE + HudIcon.COUNT + glyph;
    }

    public int readoutChar(int row, int glyph) {
        return textChar(row, TinyFont.glyphCount() + glyph);
    }

    /** The plate glyph {@code 1 << lengthIndex} pixels wide. */
    public int plateChar(int row, int lengthIndex) {
        return readoutChar(row, ReadoutFont.glyphCount() + lengthIndex);
    }

    /**
     * The space character for an advance of {@code sign * 2^power} pixels. Every layout uses the same
     * table, with powers from 0 to {@value #MAX_SPACE_POWER}.
     */
    public static int spaceChar(boolean negative, int power) {
        return SPACE_BASE + (negative ? 0x10 : 0) + power;
    }

    public static final int MAX_SPACE_POWER = 10;
}
