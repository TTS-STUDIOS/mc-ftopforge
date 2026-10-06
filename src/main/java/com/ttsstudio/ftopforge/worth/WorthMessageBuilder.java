package com.ttsstudio.ftopforge.worth;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WorthMessageBuilder {
    private WorthMessageBuilder() {}

    public static List<String> build(WorthQuery.Result r) {
        List<String> out = new ArrayList<>();
        out.add(ChatColor.translateAlternateColorCodes('&',
            "&6&l[FTopForge] &eWorth de &b" + r.displayMaterial + " &7x &f" + r.amount));
        out.add(ChatColor.translateAlternateColorCodes('&',
            "&7  - Unit: &6$" + format(r.unit)));
        out.add(ChatColor.translateAlternateColorCodes('&',
            "&7  - Total: &6&l$" + format(r.total)));
        if (r.notListed) {
            out.add(ChatColor.translateAlternateColorCodes('&',
                "&c  - No price set in items.yml"));
        }
        return out;
    }

    static String format(long n) {
        return String.format(Locale.US, "%,d", n);
    }
}
