package com.extantape.blueminimap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MigrationTest {

    @Test
    void copiesTheOldFolderWhenTheNewOneIsMissing(@TempDir Path plugins) throws Exception {
        Path old = plugins.resolve("BlueMapHUD");
        Files.createDirectories(old.resolve("sub"));
        Files.writeString(old.resolve("config.yml"), "profiles: {}\n");
        Files.writeString(old.resolve("changes.log"), "{\"a\":1}\n");
        Files.writeString(old.resolve("sub/x.json"), "[]");

        String msg = Migration.copyIfMissing(old, plugins.resolve("BlueMinimap"));

        assertNotNull(msg);
        assertEquals("profiles: {}\n", Files.readString(plugins.resolve("BlueMinimap/config.yml")));
        assertEquals("{\"a\":1}\n", Files.readString(plugins.resolve("BlueMinimap/changes.log")));
        assertEquals("[]", Files.readString(plugins.resolve("BlueMinimap/sub/x.json")));
        assertTrue(Files.exists(old.resolve("config.yml")), "the old folder is left as a backup");
        assertFalse(Files.exists(plugins.resolve("BlueMinimap.migrating")));
    }

    @Test
    void neverOverwritesAnExistingNewFolder(@TempDir Path plugins) throws Exception {
        Files.createDirectories(plugins.resolve("BlueMapHUD"));
        Files.writeString(plugins.resolve("BlueMapHUD/config.yml"), "old");
        Files.createDirectories(plugins.resolve("BlueMinimap"));
        Files.writeString(plugins.resolve("BlueMinimap/config.yml"), "new");

        assertNull(Migration.copyIfMissing(plugins.resolve("BlueMapHUD"), plugins.resolve("BlueMinimap")));
        assertEquals("new", Files.readString(plugins.resolve("BlueMinimap/config.yml")));
    }

    @Test
    void doesNothingOnAFreshServer(@TempDir Path plugins) {
        assertNull(Migration.copyIfMissing(plugins.resolve("BlueMapHUD"), plugins.resolve("BlueMinimap")));
        assertFalse(Files.exists(plugins.resolve("BlueMinimap")));
    }

    @Test
    void clearsAHalfFinishedCopyFromACrash(@TempDir Path plugins) throws Exception {
        Files.createDirectories(plugins.resolve("BlueMapHUD"));
        Files.writeString(plugins.resolve("BlueMapHUD/config.yml"), "real");
        Files.createDirectories(plugins.resolve("BlueMinimap.migrating"));
        Files.writeString(plugins.resolve("BlueMinimap.migrating/junk"), "half");

        assertNotNull(Migration.copyIfMissing(plugins.resolve("BlueMapHUD"), plugins.resolve("BlueMinimap")));
        assertEquals("real", Files.readString(plugins.resolve("BlueMinimap/config.yml")));
        assertFalse(Files.exists(plugins.resolve("BlueMinimap/junk")));
    }
}
