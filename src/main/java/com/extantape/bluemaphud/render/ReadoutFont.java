package com.extantape.bluemaphud.render;

/**
 * The 4x7 pixel font for the coordinate readout. It only has the characters coordinates need, and it is
 * drawn into the pack so its exact advances are known, like {@link TinyFont}.
 */
public final class ReadoutFont {
    public static final int WIDTH = 4;
    public static final int HEIGHT = 7;
    public static final int SPACE_ADVANCE = 4;

    private static final String CHARS = "0123456789-NESW";
    private static final String[] ROWS = {
            "0110 1001 1001 1001 1001 1001 0110",
            "0010 0110 0010 0010 0010 0010 0111",
            "0110 1001 0001 0010 0100 1000 1111",
            "0110 1001 0001 0110 0001 1001 0110",
            "1001 1001 1001 1111 0001 0001 0001",
            "1111 1000 1110 0001 0001 1001 0110",
            "0110 1000 1000 1110 1001 1001 0110",
            "1111 0001 0001 0010 0010 0100 0100",
            "0110 1001 1001 0110 1001 1001 0110",
            "0110 1001 1001 0111 0001 0001 0110",
            "0000 0000 0000 1111 0000 0000 0000",
            "1001 1101 1101 1011 1011 1001 1001",
            "1111 1000 1000 1110 1000 1000 1111",
            "0111 1000 1000 0110 0001 0001 1110",
            "1001 1001 1001 1001 1111 1111 1001",
    };

    private ReadoutFont() {}

    public static int glyphCount() {
        return CHARS.length();
    }

    public static int index(char c) {
        int index = CHARS.indexOf(c);
        if (index < 0) throw new IllegalArgumentException("Not in the readout font: " + c);
        return index;
    }

    public static boolean pixel(int glyph, int x, int y) {
        return ROWS[glyph].charAt(y * (WIDTH + 1) + x) == '1';
    }

    /** The advance the client computes: the rightmost lit column plus the font's one pixel of spacing. */
    public static int advance(int glyph) {
        for (int x = WIDTH - 1; x >= 0; x--) {
            for (int y = 0; y < HEIGHT; y++) {
                if (pixel(glyph, x, y)) return x + 2;
            }
        }
        return 1;
    }

    public static int width(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += text.charAt(i) == ' ' ? SPACE_ADVANCE : advance(index(text.charAt(i)));
        }
        return width;
    }
}
