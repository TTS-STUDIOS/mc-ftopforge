package com.ttsstudio.ftopforge.commands.hologram;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HologramConfigWriterTest {

    @Test
    void writeLocation_updatesYamlKeys(@TempDir Path tmp) throws IOException {
        File cfg = tmp.resolve("config.yml").toFile();
        Files.write(cfg.toPath(),
            ("holograms:\n" +
             "  enabled: true\n" +
             "  location:\n" +
             "    world: world\n" +
             "    x: 0.0\n" +
             "    y: 80.0\n" +
             "    z: 0.0\n").getBytes());

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(cfg);
        HologramConfigWriter w = new HologramConfigWriter(yaml, cfg, tmp.toFile());
        w.writeLocation("nether", 10.5, 64.0, -20.5);

        YamlConfiguration reloaded = YamlConfiguration.loadConfiguration(cfg);
        assertEquals("nether", reloaded.getString("holograms.location.world"));
        assertEquals(10.5, reloaded.getDouble("holograms.location.x"));
        assertEquals(64.0, reloaded.getDouble("holograms.location.y"));
        assertEquals(-20.5, reloaded.getDouble("holograms.location.z"));
    }

    @Test
    void writeEnabled_togglesFlag(@TempDir Path tmp) throws IOException {
        File cfg = tmp.resolve("config.yml").toFile();
        Files.write(cfg.toPath(), "holograms:\n  enabled: true\n".getBytes());
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(cfg);
        HologramConfigWriter w = new HologramConfigWriter(yaml, cfg, tmp.toFile());

        w.writeEnabled(false);

        YamlConfiguration reloaded = YamlConfiguration.loadConfiguration(cfg);
        assertFalse(reloaded.getBoolean("holograms.enabled"));
    }

    @Test
    void firstMutate_createsBakFile_secondMutate_doesNotOverwrite(@TempDir Path tmp) throws IOException {
        File cfg = tmp.resolve("config.yml").toFile();
        Files.write(cfg.toPath(), "holograms:\n  enabled: true\n".getBytes());
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(cfg);
        HologramConfigWriter w = new HologramConfigWriter(yaml, cfg, tmp.toFile());

        w.writeEnabled(false);
        File bak = tmp.resolve("config.yml.bak").toFile();
        assertTrue(bak.exists(), "first mutate should create config.yml.bak");
        long bakSizeFirst = bak.length();

        // Second mutate must NOT overwrite the bak (defensive: keep the very first snapshot).
        w.writeEnabled(true);
        assertEquals(bakSizeFirst, bak.length(), "bak file should be unchanged after second mutate");
    }
}
