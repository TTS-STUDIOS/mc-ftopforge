package com.ttsstudio.ftopforge.i18n;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class MessagesLoader {

    public static Messages load(JavaPlugin plugin, String lang) {
        Map<String, Object> primary = loadOne(plugin, "messages-" + lang + ".yml");
        Map<String, Object> fallback = "es".equalsIgnoreCase(lang)
                ? primary
                : loadOne(plugin, "messages-es.yml");
        return new Messages(primary, fallback);
    }

    private static Map<String, Object> loadOne(JavaPlugin plugin, String fileName) {
        File out = new File(plugin.getDataFolder(), fileName);
        if (!out.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource(fileName)) {
                if (in != null) {
                    Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException ignored) {
            }
        }
        YamlConfiguration yaml = new YamlConfiguration();
        if (out.exists()) {
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(out.toPath()), StandardCharsets.UTF_8)) {
                yaml.load(reader);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load " + fileName + ": " + e.getMessage());
            }
        } else {
            return new java.util.HashMap<>();
        }
        return yaml.getValues(true);
    }
}
