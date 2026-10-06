package com.ttsstudio.ftopforge.papi;

import com.ttsstudio.ftopforge.util.MoneyFormat;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderResolver {

    public interface FactionLike {
        String factionId();
        String factionName();
        long totalValue();
        String leaderName();
    }

    private static final Pattern TOP_PATTERN =
        Pattern.compile("^top([0-9]+)_(name|value|value_raw|leader)$");

    private final Supplier<List<? extends FactionLike>> topSupplier;
    private final LongSupplier secondsUntilRecalcSupplier;
    private final LongSupplier lastRecalcAgoSecondsSupplier;
    private final Function<UUID, String> playerFactionIdResolver;

    public PlaceholderResolver(Supplier<List<? extends FactionLike>> topSupplier,
                               LongSupplier secondsUntilRecalcSupplier,
                               LongSupplier lastRecalcAgoSecondsSupplier,
                               Function<UUID, String> playerFactionIdResolver) {
        this.topSupplier = topSupplier;
        this.secondsUntilRecalcSupplier = secondsUntilRecalcSupplier;
        this.lastRecalcAgoSecondsSupplier = lastRecalcAgoSecondsSupplier;
        this.playerFactionIdResolver = playerFactionIdResolver;
    }

    public String resolve(UUID playerUuid, String key) {
        if (key == null) return "";

        Matcher m = TOP_PATTERN.matcher(key);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1));
            List<? extends FactionLike> top = topSupplier.get();
            if (n < 1 || n > top.size()) return "";
            FactionLike f = top.get(n - 1);
            switch (m.group(2)) {
                case "name": return f.factionName();
                case "value": return MoneyFormat.shortFmt(f.totalValue());
                case "value_raw": return Long.toString(f.totalValue());
                case "leader": return f.leaderName() != null ? f.leaderName() : "";
                default: return "";
            }
        }

        switch (key) {
            case "rank": {
                if (playerUuid == null) return "—";
                String fid = playerFactionIdResolver.apply(playerUuid);
                if (fid == null) return "—";
                List<? extends FactionLike> top = topSupplier.get();
                for (int i = 0; i < top.size(); i++) if (fid.equals(top.get(i).factionId())) return Integer.toString(i + 1);
                return "—";
            }
            case "faction_name": {
                if (playerUuid == null) return "";
                String fid = playerFactionIdResolver.apply(playerUuid);
                if (fid == null) return "";
                for (FactionLike f : topSupplier.get()) if (fid.equals(f.factionId())) return f.factionName();
                return "";
            }
            case "faction_value": {
                if (playerUuid == null) return "0";
                String fid = playerFactionIdResolver.apply(playerUuid);
                if (fid == null) return "0";
                for (FactionLike f : topSupplier.get()) if (fid.equals(f.factionId())) return MoneyFormat.shortFmt(f.totalValue());
                return "0";
            }
            case "faction_value_raw": {
                if (playerUuid == null) return "0";
                String fid = playerFactionIdResolver.apply(playerUuid);
                if (fid == null) return "0";
                for (FactionLike f : topSupplier.get()) if (fid.equals(f.factionId())) return Long.toString(f.totalValue());
                return "0";
            }
            case "factions_total": return Integer.toString(topSupplier.get().size());
            case "next_recalc": return humanSeconds(secondsUntilRecalcSupplier.getAsLong());
            case "next_recalc_seconds": return Long.toString(secondsUntilRecalcSupplier.getAsLong());
            case "last_recalc_ago": return humanSeconds(lastRecalcAgoSecondsSupplier.getAsLong());
            default: return "";
        }
    }

    static String humanSeconds(long secs) {
        if (secs < 0) return "--";
        long m = secs / 60; long s = secs % 60;
        return m + "m " + s + "s";
    }
}
