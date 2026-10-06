package com.ttsstudio.ftopforge.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class Banner {
    private Banner() {}

    public static void print(JavaPlugin plugin, String storageType) {
        print(plugin, storageType, null);
    }

    public static void print(JavaPlugin plugin, String storageType, String modulesLine) {
        try (InputStream in = plugin.getResource("banner.txt")) {
            if (in == null) {
                plugin.getLogger().info("FTopForge v" + plugin.getDescription().getVersion() + " by ttsstudio");
                return;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String resolved = line.replace("${storage_type}", storageType);
                    if (modulesLine != null) {
                        resolved = resolved.replace("${ftf.modules}", modulesLine);
                    }
                    plugin.getLogger().info(resolved);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Banner print failed: " + e.getMessage());
        }
    }
}
