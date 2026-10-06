package com.ttsstudio.ftopforge.i18n;

import org.bukkit.ChatColor;
import java.util.Map;

public class Messages {
    private final Map<String, Object> primary;
    private final Map<String, Object> fallback;

    public Messages(Map<String, Object> primary, Map<String, Object> fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    public String get(String dottedKey, Object... namedArgs) {
        String raw = resolve(primary, dottedKey);
        if (raw == null && fallback != primary) {
            raw = resolve(fallback, dottedKey);
        }
        if (raw == null) {
            return "<missing:" + dottedKey + ">";
        }
        String prefix = stringOrEmpty(resolve(primary, "prefix"));
        if (prefix.isEmpty() && fallback != primary) {
            prefix = stringOrEmpty(resolve(fallback, "prefix"));
        }
        String s = raw.replace("%prefix%", prefix);
        if (namedArgs != null) {
            for (int i = 0; i + 1 < namedArgs.length; i += 2) {
                String name = String.valueOf(namedArgs[i]);
                String val = String.valueOf(namedArgs[i + 1]);
                s = s.replace("%" + name + "%", val);
            }
        }
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @SuppressWarnings("unchecked")
    private String resolve(Map<String, Object> root, String dottedKey) {
        if (root == null) return null;
        // Flat lookup first — works when the map came from YamlConfiguration.getValues(true),
        // which includes dot-path keys as direct entries alongside the ConfigurationSection refs.
        Object direct = root.get(dottedKey);
        if (direct instanceof String) return (String) direct;
        // Nested fallback — works when the map is a plain HashMap with nested HashMaps
        // (e.g. unit tests using toNestedMap helper, or hand-built maps).
        String[] parts = dottedKey.split("\\.");
        Object cur = root;
        for (String p : parts) {
            if (!(cur instanceof Map)) return null;
            cur = ((Map<String, Object>) cur).get(p);
            if (cur == null) return null;
        }
        return cur instanceof String ? (String) cur : null;
    }

    private static String stringOrEmpty(String s) {
        return s == null ? "" : s;
    }
}
