package com.extantape.bluemaphud.render;

import com.extantape.bluemaphud.config.HudConfig;

/** The fonts a given config needs. The pack is generated from the same object that draws the map. */
public final class HudFonts {
    /** Pixels between the bottom of the map and the top of the coordinate line's plate. */
    private static final int READOUT_GAP = 2;
    /** Pixels kept free below the largest grid for the coordinate line and its plate. */
    private static final int TEXT_PIXELS = READOUT_GAP + FontLayout.PLATE_HEIGHT + 1;
    private static final int MAX_MINIMAP_CELLS = 40;
    /** The client's font atlas can't hold a glyph taller than this. */
    private static final int MAX_CELL_PIXELS = 240;
    /** Pixels above the action bar's text line that aren't map rows. */
    private static final int ACTION_BAR_MARGIN = 24;

    public final FontLayout minimap;
    public final FontLayout fullscreen;
    public final int maxOffsetRows;

    public HudFonts(HudConfig config) {
        int cell = config.hud().cellPixels();
        int reserved = reservedRows(cell);
        boolean actionBar = config.hud().channel() == HudConfig.Channel.ACTION_BAR;
        int offsetRows = config.hud().maxOffsetY() / cell;
        if (actionBar) {
            // These rows sit above the text line, so they need tall glyphs, and those have a size limit.
            int room = (MAX_CELL_PIXELS - ACTION_BAR_MARGIN) / cell - MAX_MINIMAP_CELLS - reserved;
            offsetRows = Math.max(0, Math.min(offsetRows, room));
        }
        maxOffsetRows = offsetRows;
        minimap = new FontLayout("hud", 0, cell, MAX_MINIMAP_CELLS + offsetRows + reserved, reserved,
                actionBar ? FontLayout.Direction.UP_FROM_BOTTOM : FontLayout.Direction.DOWN_FROM_TOP);

        int fullCell = config.fullscreen().cellPixels();
        int fullReserved = reservedRows(fullCell);
        fullscreen = new FontLayout("full", 1, fullCell, config.fullscreen().height() + fullReserved, fullReserved,
                FontLayout.Direction.DOWN_FROM_TOP);
    }

    private static int reservedRows(int cell) {
        return Math.max(2, TEXT_PIXELS / cell + 1);
    }

    /** How many rows below the map the coordinate line starts, leaving room for its plate. */
    public static int readoutRowOffset(FontLayout layout) {
        int above = READOUT_GAP + FontLayout.PLATE_MARGIN;
        return (above + layout.step() - 1) / layout.step();
    }
}
