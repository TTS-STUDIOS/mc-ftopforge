package com.ttsstudio.ftopforge.holograms;

import com.ttsstudio.ftopforge.util.MoneyFormat;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HologramLineFormatter {
    private static final Pattern TOP_PATTERN =
        Pattern.compile("%top([0-9]+)_(name|value|value_raw|leader)%");
    private static final Pattern NEXT_RECALC_PATTERN = Pattern.compile("%next_recalc%");

    public interface FactionLike {
        String name(); long totalValue(); String leaderName();
    }

    private HologramLineFormatter() {}

    public static String format(String template,
                                List<? extends FactionLike> orderedTop,
                                long secondsUntilRecalc) {
        if (template == null) return "";
        String out = template;
        Matcher m = TOP_PATTERN.matcher(out);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            int n = Integer.parseInt(m.group(1));
            String field = m.group(2);
            String value;
            if (n < 1 || n > orderedTop.size()) {
                value = "--";
            } else {
                FactionLike f = orderedTop.get(n - 1);
                switch (field) {
                    case "name": value = f.name(); break;
                    case "value": value = MoneyFormat.shortFmt(f.totalValue()); break;
                    case "value_raw": value = Long.toString(f.totalValue()); break;
                    case "leader": value = f.leaderName() != null ? f.leaderName() : "--"; break;
                    default: value = "--";
                }
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        out = sb.toString();
        out = NEXT_RECALC_PATTERN.matcher(out).replaceAll(Matcher.quoteReplacement(humanSeconds(secondsUntilRecalc)));
        return ChatColor.translateAlternateColorCodes('&', out);
    }

    static String humanSeconds(long secs) {
        if (secs < 0) return "--";
        long m = secs / 60; long s = secs % 60;
        return m + "m " + s + "s";
    }
}
