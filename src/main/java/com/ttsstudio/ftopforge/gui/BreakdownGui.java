package com.ttsstudio.ftopforge.gui;

import com.ttsstudio.ftopforge.core.AvgBreakdown;
import com.ttsstudio.ftopforge.core.FactionSnapshot;
import com.ttsstudio.ftopforge.core.TopCache;
import com.ttsstudio.ftopforge.i18n.Messages;
import com.ttsstudio.ftopforge.util.MoneyFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BreakdownGui {

    private final TopCache topCache;
    private final SkullCache skullCache;
    private final Messages msg;
    private final MaterialKey frameKey;
    private final MaterialKey[] catKeys = new MaterialKey[5]; // chunks, spawners, items, blocks, balance

    public BreakdownGui(TopCache topCache, SkullCache skullCache, Messages msg, ConfigurationSection guiSection) {
        this.topCache = topCache;
        this.skullCache = skullCache;
        this.msg = msg;
        String frameRaw = guiSection != null
                ? guiSection.getString("breakdown.frame", "STAINED_GLASS_PANE-9")
                : "STAINED_GLASS_PANE-9";
        this.frameKey = MaterialKey.parse(frameRaw);
        catKeys[0] = readCat(guiSection, "chunks", "GRASS");
        catKeys[1] = readCat(guiSection, "spawners", "MONSTER_EGG-50");
        catKeys[2] = readCat(guiSection, "items", "DIAMOND");
        catKeys[3] = readCat(guiSection, "blocks", "BEACON");
        catKeys[4] = readCat(guiSection, "balance", "GOLD_INGOT");
    }

    public void open(Player player, String factionId, int originPage) {
        FactionSnapshot s = topCache.byId(factionId);
        if (s == null) {
            player.sendMessage(msg.get("gui.breakdown.not_in_ranking"));
            return;
        }
        AvgBreakdown avg = topCache.averagesTop10();
        int rank = topCache.rankOf(factionId);
        long total = s.totalValue();

        String title = ChatColor.translateAlternateColorCodes('&',
                msg.get("gui.breakdown.title", "faction", s.factionName()));
        if (title.length() > 32) title = title.substring(0, 32);

        FBreakdownGuiHolder holder = new FBreakdownGuiHolder(factionId, originPage);
        Inventory inv = Bukkit.createInventory(holder, 27, title);
        holder.setInventory(inv);

        ItemStack frame = simpleNamed(frameKey.name, frameKey.data, " ");

        // row 0
        for (int i = 0; i <= 3; i++) inv.setItem(i, frame);
        inv.setItem(4, simpleNamed("BARRIER", (short) 0, msg.get("gui.breakdown.close")));
        for (int i = 5; i <= 8; i++) inv.setItem(i, frame);

        // row 1
        inv.setItem(9, frame);
        inv.setItem(10, buildCatItem("chunks", catKeys[0], s.chunksValue(), total, avg.chunks));
        inv.setItem(11, buildCatItem("spawners", catKeys[1], s.spawnersValue(), total, avg.spawners));
        inv.setItem(12, buildCatItem("items", catKeys[2], s.itemsValue(), total, avg.items));
        inv.setItem(13, buildSummary(s, rank, total));
        inv.setItem(14, buildCatItem("blocks", catKeys[3], s.blocksValue(), total, avg.blocks));
        inv.setItem(15, buildCatItem("balance", catKeys[4], s.balanceValue(), total, avg.balance));
        inv.setItem(16, frame);
        inv.setItem(17, frame);

        // row 2
        for (int i = 18; i <= 21; i++) inv.setItem(i, frame);
        inv.setItem(22, simpleNamed("ARROW", (short) 0, msg.get("gui.breakdown.back")));
        for (int i = 23; i <= 26; i++) inv.setItem(i, frame);

        player.openInventory(inv);
    }

    private ItemStack buildSummary(FactionSnapshot s, int rank, long total) {
        UUID leader = s.leaderUuid() != null ? safeUuid(s.leaderUuid()) : null;
        ItemStack stack = leader != null
                ? skullCache.getOrFetch(leader, s.leaderName())
                : new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(msg.get("gui.breakdown.summary.name",
                    "faction_name", s.factionName()));
            List<String> lore = new ArrayList<String>();
            lore.add(msg.get("gui.breakdown.summary.lore_rank", "rank", String.valueOf(rank)));
            lore.add(msg.get("gui.breakdown.summary.lore_leader",
                    "leader_name", s.leaderName() != null ? s.leaderName() : "?"));
            // member_count not tracked in FactionSnapshot — deferred to Bundle B/C
            lore.add(msg.get("gui.breakdown.summary.lore_members", "member_count", "?"));
            lore.add(" ");
            lore.add(msg.get("gui.breakdown.summary.lore_total",
                    "total_value", MoneyFormat.shortFmt(total)));
            lore.add(" ");
            lore.add(msg.get("gui.breakdown.summary.lore_snapshot",
                    "relative_time", TopGui.formatDelta(System.currentTimeMillis() - s.calculatedAt())));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack buildCatItem(String key, MaterialKey matKey, long value, long total, long avg) {
        ItemStack stack = simpleNamed(matKey.name, matKey.data,
                msg.get("gui.breakdown.cat." + key + ".name"));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            int pct = BreakdownMath.pctOf(value, total);
            BreakdownMath.Delta d = BreakdownMath.delta(value, avg);
            List<String> lore = new ArrayList<String>();
            lore.add(msg.get("gui.breakdown.cat_lore.value", "value", MoneyFormat.shortFmt(value)));
            lore.add(msg.get("gui.breakdown.cat_lore.pct", "pct", String.valueOf(pct)));
            lore.add(" ");
            lore.add(msg.get("gui.breakdown.cat_lore.avg", "avg", MoneyFormat.shortFmt(avg)));
            lore.add(msg.get("gui.breakdown.cat_lore.delta",
                    "delta_color", d.color,
                    "delta_sign", String.valueOf(d.sign),
                    "delta_pct", String.valueOf(d.pct)));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @SuppressWarnings("deprecation")
    private static ItemStack simpleNamed(String materialName, short data, String name) {
        Material mat = Material.matchMaterial(materialName);
        if (mat == null) mat = Material.STONE;
        ItemStack stack = new ItemStack(mat, 1, data);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static MaterialKey readCat(ConfigurationSection sec, String key, String fallbackRaw) {
        if (sec == null) return MaterialKey.parse(fallbackRaw);
        return MaterialKey.parse(sec.getString("breakdown.items." + key, fallbackRaw));
    }

    private static UUID safeUuid(String s) {
        try { return UUID.fromString(s); } catch (Exception e) { return null; }
    }
}
