package com.cristian.ftopforge.commands.hologram;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Mutates the live config.yml on disk when admins run /ftopforge hologram set/move/disable/enable.
 * Defensive backup: copies config.yml -> config.yml.bak on the first mutate per process, never overwrites
 * an existing .bak. Paper 1.18+ preserves YAML comments on saveConfig automatically (parseComments=true
 * default), so no extra wiring is needed to keep the shipped richer schema's es-language comments.
 */
public final class HologramConfigWriter {

    private final java.util.function.Supplier<FileConfiguration> source;
    private final File cfgFile;
    private final File dataFolder;
    private boolean backupTakenThisProcess = false;
    private boolean backupJustCreated = false;

    public HologramConfigWriter(FileConfiguration cfg, File cfgFile, File dataFolder) {
        this(() -> cfg, cfgFile, dataFolder);
    }

    /**
     * Reads the configuration each time: after a reload the plugin holds a new
     * object, and writing the old one put back whatever was edited by hand.
     */
    public HologramConfigWriter(java.util.function.Supplier<FileConfiguration> source, File cfgFile, File dataFolder) {
        this.source = source;
        this.cfgFile = cfgFile;
        this.dataFolder = dataFolder;
    }

    public synchronized boolean writeLocation(String world, double x, double y, double z) {
        ensureBackup();
        FileConfiguration cfg = source.get();
        cfg.set("holograms.location.world", world);
        cfg.set("holograms.location.x", x);
        cfg.set("holograms.location.y", y);
        cfg.set("holograms.location.z", z);
        return save(cfg);
    }

    public synchronized boolean writeEnabled(boolean enabled) {
        ensureBackup();
        FileConfiguration cfg = source.get();
        cfg.set("holograms.enabled", enabled);
        return save(cfg);
    }

    /** True if this writer just created the .bak in this very call (so caller can show the message). */
    public synchronized boolean backupCreatedThisCall() {
        return backupTakenThisProcess && backupJustCreated;
    }

    private void ensureBackup() {
        backupJustCreated = false;
        if (backupTakenThisProcess) return;
        File bak = new File(dataFolder, "config.yml.bak");
        if (bak.exists()) {
            backupTakenThisProcess = true;
            return;
        }
        try {
            Files.copy(cfgFile.toPath(), bak.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            backupTakenThisProcess = true;
            backupJustCreated = true;
        } catch (IOException ignored) {
            // best-effort; not fatal
        }
    }

    private boolean save(FileConfiguration cfg) {
        try {
            if (cfg instanceof YamlConfiguration) {
                ((YamlConfiguration) cfg).save(cfgFile);
            } else {
                cfg.save(cfgFile);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
