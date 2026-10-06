package com.ttsstudio.ftopforge.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ItemPrices {

    private final Map<String, Long> items;     // key: ITEM_NAME or COMPOUND_KEY
    private final Map<String, Long> spawners;  // key: MOB_NAME uppercase
    private final Map<String, Long> blocks;    // key: MATERIAL_NAME uppercase
    private final long godApple;
    private final long enderDragonEgg;

    public ItemPrices(Map<String, Long> items, Map<String, Long> spawners, Map<String, Long> blocks,
                      long godApple, long enderDragonEgg) {
        this.items = normalize(items);
        this.spawners = normalize(spawners);
        this.blocks = normalize(blocks);
        this.godApple = godApple;
        this.enderDragonEgg = enderDragonEgg;
    }

    public static ItemPrices loadFromYaml(JavaPlugin plugin) {
        File f = new File(plugin.getDataFolder(), "items.yml");
        if (!f.exists()) plugin.saveResource("items.yml", false);
        FileConfiguration y = YamlConfiguration.loadConfiguration(f);

        Map<String, Long> items = new HashMap<>();
        // Diamond/Iron/Gold blocks of items map: KEY = "<METAL>_<PIECE>" (e.g. DIAMOND_SWORD, IRON_HELMET)
        for (String metal : new String[]{"Diamond", "Iron", "Gold"}) {
            if (!y.isConfigurationSection(metal)) continue;
            for (String piece : y.getConfigurationSection(metal).getKeys(false)) {
                long v = y.getLong(metal + "." + piece, 0L);
                if (v > 0) {
                    items.put((metal + "_" + piece).toUpperCase(), v);
                }
            }
        }
        if (y.contains("GoldenApple")) items.put("GOLDEN_APPLE", y.getLong("GoldenApple"));

        Map<String, Long> spawners = new HashMap<>();
        if (y.isConfigurationSection("Spawners")) {
            for (String mob : y.getConfigurationSection("Spawners").getKeys(false)) {
                long v = y.getLong("Spawners." + mob, 0L);
                if (v > 0) spawners.put(mob.toUpperCase(), v);
            }
        }

        Map<String, Long> blocks = new HashMap<>();
        if (y.isConfigurationSection("Blocks")) {
            for (String mat : y.getConfigurationSection("Blocks").getKeys(false)) {
                long v = y.getLong("Blocks." + mat, 0L);
                if (v > 0) blocks.put(mat.toUpperCase(), v);
            }
        }

        long godApple = y.getLong("GodApple", 0L);
        long edragonEgg = y.getLong("MobDrops.EnderDragonEgg", 0L);

        return new ItemPrices(items, spawners, blocks, godApple, edragonEgg);
    }

    public long itemValue(String materialName) {
        if (materialName == null) return 0L;
        Long v = items.get(materialName.toUpperCase());
        return v == null ? 0L : v;
    }

    public long spawnerValue(String mobName) {
        if (mobName == null) return 0L;
        Long v = spawners.get(mobName.toUpperCase());
        return v == null ? 0L : v;
    }

    public long blockValue(String materialName) {
        if (materialName == null) return 0L;
        Long v = blocks.get(materialName.toUpperCase());
        return v == null ? 0L : v;
    }

    public long godAppleValue() { return godApple; }
    public long enderDragonEggValue() { return enderDragonEgg; }

    private static Map<String, Long> normalize(Map<String, Long> in) {
        Map<String, Long> out = new HashMap<>();
        for (Map.Entry<String, Long> e : in.entrySet()) {
            out.put(e.getKey().toUpperCase(), e.getValue());
        }
        return out;
    }
}
