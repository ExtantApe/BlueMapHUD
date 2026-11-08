package com.extantape.bluemaphud.pack;

import com.extantape.bluemaphud.config.HudConfig;
import com.extantape.bluemaphud.render.FontLayout;
import com.extantape.bluemaphud.render.HudFonts;
import com.extantape.bluemaphud.render.HudIcon;
import com.extantape.bluemaphud.render.IconSet;
import com.extantape.bluemaphud.render.ReadoutFont;
import com.extantape.bluemaphud.render.TinyFont;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generates the resource pack in memory. Everything lives under the bluemaphud namespace, apart from
 * the optional transparent boss bar textures. No shaders are involved.
 */
public final class PackBuilder {
    /** Format 84 is every 26.1 client (26.1, 26.1.1, 26.1.2), the oldest supported. The range stays open so new clients don't warn. */
    private static final int MIN_FORMAT = 84;
    private static final int MAX_FORMAT = 999;
    private static final int SHEET_COLUMNS = 16;
    private static final long FIXED_ZIP_TIME = 1_000_000_000_000L;
    private static final int RUN_CELL_WIDTH = FontLayout.MAX_RUN;
    /** The plate behind text: about half transparent, tinted black by the frame. */
    /**
     * The plate is tinted black and about 69% opaque: the lightest plate that keeps both the white numbers
     * and the dimmer facing above a 4.5:1 contrast ratio even over pure white, while the map still shows faintly.
     */
    private static final int PLATE_PIXEL = 0xB0FFFFFF;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public record Built(byte[] zip, String sha1Hex, byte[] sha1) {}

    private PackBuilder() {}

    public static Built build(HudConfig config, HudFonts fonts, IconSet icons) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        files.put("pack.mcmeta", packMeta());
        addLayout(files, fonts.minimap, icons);
        addLayout(files, fonts.fullscreen, icons);
        if (config.pack().hideBossBar()) {
            byte[] blank = png(new BufferedImage(182, 5, BufferedImage.TYPE_INT_ARGB));
            String color = config.pack().bossBarColor();
            files.put("assets/minecraft/textures/gui/sprites/boss_bar/" + color + "_background.png", blank);
            files.put("assets/minecraft/textures/gui/sprites/boss_bar/" + color + "_progress.png", blank);
        }
        byte[] zip = zip(files);
        byte[] sha1 = sha1(zip);
        return new Built(zip, HexFormat.of().formatHex(sha1), sha1);
    }

    private static byte[] packMeta() {
        JsonObject pack = new JsonObject();
        pack.addProperty("description", "BlueMapHUD minimap");
        pack.addProperty("min_format", MIN_FORMAT);
        pack.addProperty("max_format", MAX_FORMAT);
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        return GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
    }

    private static void addLayout(Map<String, byte[]> files, FontLayout layout, IconSet icons) throws IOException {
        String name = layout.name();
        int runCells = FontLayout.paddedCells(layout.maxMapAscent(), layout.step(), 1);
        int iconCells = FontLayout.paddedCells(layout.maxIconAscent(), 1, HudIcon.SIZE);
        int textCells = FontLayout.paddedCells(layout.maxTextAscent(), 1, TinyFont.HEIGHT);

        files.put("assets/bluemaphud/textures/font/" + name + "_runs.png", png(runSheet(runCells)));
        files.put("assets/bluemaphud/textures/font/" + name + "_icons.png", png(iconSheet(icons, iconCells)));
        files.put("assets/bluemaphud/textures/font/" + name + "_text.png", png(textSheet(textCells)));
        int readoutCells = FontLayout.paddedCells(layout.maxReadoutAscent(), 1, ReadoutFont.HEIGHT);
        int plateCells = FontLayout.paddedCells(layout.maxPlateAscent(), 1, FontLayout.PLATE_HEIGHT);
        files.put("assets/bluemaphud/textures/font/" + name + "_readout.png", png(readoutSheet(readoutCells)));
        files.put("assets/bluemaphud/textures/font/" + name + "_plate.png", png(plateSheet(plateCells)));

        JsonArray providers = new JsonArray();
        providers.add(spaceProvider());
        for (int row = 0; row < layout.rows(); row++) {
            String[] runs = new String[FontLayout.RUN_LENGTHS];
            for (int i = 0; i < runs.length; i++) runs[i] = new String(Character.toChars(layout.runChar(row, i)));
            providers.add(bitmap(name + "_runs", runCells * layout.step(), layout.mapAscent(row), runs));

            final int r = row;
            providers.add(bitmap(name + "_icons", iconCells, layout.iconAscent(row),
                    sheetRows(HudIcon.COUNT, i -> layout.iconChar(r, i))));
            providers.add(bitmap(name + "_text", textCells, layout.textAscent(row),
                    sheetRows(TinyFont.glyphCount(), i -> layout.textChar(r, i))));
            providers.add(bitmap(name + "_readout", readoutCells, layout.readoutAscent(row),
                    sheetRows(ReadoutFont.glyphCount(), i -> layout.readoutChar(r, i))));
            String[] plates = new String[FontLayout.PLATE_LENGTHS];
            for (int i = 0; i < plates.length; i++) plates[i] = new String(Character.toChars(layout.plateChar(row, i)));
            providers.add(bitmap(name + "_plate", plateCells, layout.plateAscent(row), plates));
        }
        JsonObject font = new JsonObject();
        font.add("providers", providers);
        files.put("assets/bluemaphud/font/" + name + ".json", GSON.toJson(font).getBytes(StandardCharsets.UTF_8));
    }

    private static JsonObject spaceProvider() {
        JsonObject advances = new JsonObject();
        for (int power = 0; power <= FontLayout.MAX_SPACE_POWER; power++) {
            advances.addProperty(new String(Character.toChars(FontLayout.spaceChar(false, power))), 1 << power);
            advances.addProperty(new String(Character.toChars(FontLayout.spaceChar(true, power))), -(1 << power));
        }
        JsonObject provider = new JsonObject();
        provider.addProperty("type", "space");
        provider.add("advances", advances);
        return provider;
    }

    private static JsonObject bitmap(String file, int height, int ascent, String[] chars) {
        JsonObject provider = new JsonObject();
        provider.addProperty("type", "bitmap");
        provider.addProperty("file", "bluemaphud:font/" + file + ".png");
        provider.addProperty("height", height);
        provider.addProperty("ascent", ascent);
        JsonArray rows = new JsonArray();
        for (String row : chars) rows.add(row);
        provider.add("chars", rows);
        return provider;
    }

    private interface CharAt { int at(int index); }

    /** Lays characters out in the same grid as the sheets, with unused cells mapped to NUL. */
    private static String[] sheetRows(int count, CharAt chars) {
        int rows = (count + SHEET_COLUMNS - 1) / SHEET_COLUMNS;
        String[] result = new String[rows];
        for (int row = 0; row < rows; row++) {
            StringBuilder builder = new StringBuilder();
            for (int column = 0; column < SHEET_COLUMNS; column++) {
                int index = row * SHEET_COLUMNS + column;
                builder.appendCodePoint(index < count ? chars.at(index) : 0);
            }
            result[row] = builder.toString();
        }
        return result;
    }

    /** One column of cells. Row {@code i} lights its top {@code 2^i} pixels across, leaving padding below. */
    private static BufferedImage runSheet(int cellHeight) {
        BufferedImage image = new BufferedImage(RUN_CELL_WIDTH, cellHeight * FontLayout.RUN_LENGTHS, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < FontLayout.RUN_LENGTHS; i++) {
            for (int x = 0; x < 1 << i; x++) image.setRGB(x, i * cellHeight, 0xFFFFFFFF);
        }
        return image;
    }

    private static BufferedImage iconSheet(IconSet icons, int cellHeight) {
        int rows = (HudIcon.COUNT + SHEET_COLUMNS - 1) / SHEET_COLUMNS;
        BufferedImage image = new BufferedImage(HudIcon.SIZE * SHEET_COLUMNS, cellHeight * rows, BufferedImage.TYPE_INT_ARGB);
        for (int icon = 0; icon < HudIcon.COUNT; icon++) {
            int originX = icon % SHEET_COLUMNS * HudIcon.SIZE;
            int originY = icon / SHEET_COLUMNS * cellHeight;
            BufferedImage art = icons.image(icon);
            for (int y = 0; y < HudIcon.SIZE; y++) {
                for (int x = 0; x < HudIcon.SIZE; x++) image.setRGB(originX + x, originY + y, art.getRGB(x, y));
            }
        }
        return image;
    }

    private static BufferedImage textSheet(int cellHeight) {
        int count = TinyFont.glyphCount();
        int rows = (count + SHEET_COLUMNS - 1) / SHEET_COLUMNS;
        BufferedImage image = new BufferedImage(TinyFont.WIDTH * SHEET_COLUMNS, cellHeight * rows, BufferedImage.TYPE_INT_ARGB);
        for (int glyph = 0; glyph < count; glyph++) {
            int originX = glyph % SHEET_COLUMNS * TinyFont.WIDTH;
            int originY = glyph / SHEET_COLUMNS * cellHeight;
            for (int y = 0; y < TinyFont.HEIGHT; y++) {
                for (int x = 0; x < TinyFont.WIDTH; x++) {
                    if (TinyFont.pixel(glyph, x, y)) image.setRGB(originX + x, originY + y, 0xFFFFFFFF);
                }
            }
        }
        return image;
    }

    private static BufferedImage readoutSheet(int cellHeight) {
        int count = ReadoutFont.glyphCount();
        int rows = (count + SHEET_COLUMNS - 1) / SHEET_COLUMNS;
        BufferedImage image = new BufferedImage(ReadoutFont.WIDTH * SHEET_COLUMNS, cellHeight * rows, BufferedImage.TYPE_INT_ARGB);
        for (int glyph = 0; glyph < count; glyph++) {
            int originX = glyph % SHEET_COLUMNS * ReadoutFont.WIDTH;
            int originY = glyph / SHEET_COLUMNS * cellHeight;
            for (int y = 0; y < ReadoutFont.HEIGHT; y++) {
                for (int x = 0; x < ReadoutFont.WIDTH; x++) {
                    if (ReadoutFont.pixel(glyph, x, y)) image.setRGB(originX + x, originY + y, 0xFFFFFFFF);
                }
            }
        }
        return image;
    }

    /** One column of cells. Row {@code i} is a plate {@code 2^i} pixels wide, with padding below. */
    private static BufferedImage plateSheet(int cellHeight) {
        BufferedImage image = new BufferedImage(1 << (FontLayout.PLATE_LENGTHS - 1), cellHeight * FontLayout.PLATE_LENGTHS, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < FontLayout.PLATE_LENGTHS; i++) {
            for (int y = 0; y < FontLayout.PLATE_HEIGHT; y++) {
                for (int x = 0; x < 1 << i; x++) image.setRGB(x, i * cellHeight + y, PLATE_PIXEL);
            }
        }
        return image;
    }

    private static byte[] png(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /** Fixed timestamps keep the zip, and so its hash, the same when nothing has changed. */
    private static byte[] zip(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                ZipEntry entry = new ZipEntry(file.getKey());
                entry.setTime(FIXED_ZIP_TIME);
                zip.putNextEntry(entry);
                zip.write(file.getValue());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static byte[] sha1(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-1").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
