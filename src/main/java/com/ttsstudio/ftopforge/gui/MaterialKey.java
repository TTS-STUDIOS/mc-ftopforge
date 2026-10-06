package com.ttsstudio.ftopforge.gui;

/** Parses a config string like "STAINED_GLASS_PANE-7" or "DIAMOND" into name + data byte.
 *  Tasks 8/9 use this for legacy 1.8 Material lookup (the API doesn't have flattened names). */
public final class MaterialKey {
    public final String name;
    public final short data;

    private MaterialKey(String name, short data) {
        this.name = name;
        this.data = data;
    }

    public static MaterialKey parse(String raw) {
        if (raw == null || raw.isEmpty()) return new MaterialKey("AIR", (short) 0);
        int dash = raw.indexOf('-');
        if (dash < 0) return new MaterialKey(raw, (short) 0);
        String name = raw.substring(0, dash);
        String dataStr = raw.substring(dash + 1);
        short data;
        try { data = Short.parseShort(dataStr); } catch (NumberFormatException e) { data = 0; }
        return new MaterialKey(name, data);
    }
}
