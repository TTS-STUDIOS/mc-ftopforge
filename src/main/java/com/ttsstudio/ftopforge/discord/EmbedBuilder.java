package com.ttsstudio.ftopforge.discord;

import com.ttsstudio.ftopforge.util.MoneyFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class EmbedBuilder {
    private EmbedBuilder() {}

    public static final class TopRow {
        public final int rank;
        public final String factionName;
        public final long worth;
        public final long prevWorth;
        public final boolean hasPrev;
        public TopRow(int rank, String name, long worth, long prevWorth, boolean hasPrev) {
            this.rank = rank;
            this.factionName = name;
            this.worth = worth;
            this.prevWorth = prevWorth;
            this.hasPrev = hasPrev;
        }
    }

    public static final class PayoutEntry {
        public final int rank;
        public final String factionName;
        public final String reward;
        public PayoutEntry(int r, String n, String reward) {
            this.rank = r;
            this.factionName = n;
            this.reward = reward;
        }
    }

    public static int parseColorHex(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        return Integer.parseInt(h, 16);
    }

    public static String deltaField(long curr, long prev) {
        long diff = curr - prev;
        if (diff > 0) return "▲ +" + MoneyFormat.shortFmt(diff);
        if (diff < 0) return "▼ -" + MoneyFormat.shortFmt(Math.abs(diff));
        return "= 0";
    }

    public static EmbedPayload recalcFinished(List<TopRow> top10, String colorHex, String footer, String thumbnail) {
        EmbedPayload p = baseEmbed("Top Facciones — Recalc Finalizado", colorHex, footer, thumbnail);
        for (TopRow r : top10) p.fields.add(buildField(r));
        return p;
    }

    public static EmbedPayload kingChange(List<TopRow> top10, String colorHex, String footer, String thumbnail, String mentionRoleId) {
        EmbedPayload p = baseEmbed("Nuevo Rey del Top", colorHex, footer, thumbnail);
        for (TopRow r : top10) p.fields.add(buildField(r));
        if (mentionRoleId != null && !mentionRoleId.isEmpty()) p.content = "<@&" + mentionRoleId + ">";
        return p;
    }

    public static EmbedPayload top10Shuffle(List<TopRow> top10, String colorHex, String footer, String thumbnail) {
        EmbedPayload p = baseEmbed("Top 10 — Reordenado", colorHex, footer, thumbnail);
        for (TopRow r : top10) p.fields.add(buildField(r));
        return p;
    }

    public static EmbedPayload weeklyReset(List<TopRow> top10, List<PayoutEntry> payouts,
                                           String colorHex, String footer, String thumbnail,
                                           String mentionRoleId) {
        EmbedPayload p = baseEmbed("Cierre Semanal — Payouts", colorHex, footer, thumbnail);
        for (TopRow r : top10) p.fields.add(buildField(r));
        StringBuilder payoutsTxt = new StringBuilder();
        for (PayoutEntry pe : payouts) {
            payoutsTxt.append("#").append(pe.rank).append(" ").append(pe.factionName).append(": ").append(pe.reward).append("\n");
        }
        EmbedPayload.Field f = new EmbedPayload.Field();
        f.name = "Payouts";
        f.value = payoutsTxt.toString();
        f.inline = false;
        p.fields.add(f);
        if (mentionRoleId != null && !mentionRoleId.isEmpty()) p.content = "<@&" + mentionRoleId + ">";
        return p;
    }

    private static EmbedPayload baseEmbed(String title, String colorHex, String footer, String thumbnail) {
        EmbedPayload p = new EmbedPayload();
        p.title = title;
        p.color = parseColorHex(colorHex);
        p.footer = footer;
        p.thumbnailUrl = thumbnail;
        p.timestampIso = Instant.now().toString();
        p.fields = new ArrayList<>();
        return p;
    }

    private static EmbedPayload.Field buildField(TopRow r) {
        EmbedPayload.Field f = new EmbedPayload.Field();
        f.name = "#" + r.rank + " " + r.factionName;
        String valueStr = "$" + MoneyFormat.shortFmt(r.worth);
        if (r.hasPrev) valueStr += " (" + deltaField(r.worth, r.prevWorth) + " vs prev)";
        f.value = valueStr;
        f.inline = false;
        return f;
    }

    public static String toJson(EmbedPayload p) {
        StringBuilder sb = new StringBuilder("{");
        if (p.content != null) sb.append("\"content\":\"").append(esc(p.content)).append("\",");
        sb.append("\"embeds\":[{");
        sb.append("\"title\":\"").append(esc(p.title)).append("\",");
        sb.append("\"color\":").append(p.color).append(",");
        if (p.thumbnailUrl != null && !p.thumbnailUrl.isEmpty())
            sb.append("\"thumbnail\":{\"url\":\"").append(esc(p.thumbnailUrl)).append("\"},");
        if (p.footer != null) sb.append("\"footer\":{\"text\":\"").append(esc(p.footer)).append("\"},");
        if (p.timestampIso != null) sb.append("\"timestamp\":\"").append(p.timestampIso).append("\",");
        sb.append("\"fields\":[");
        for (int i = 0; i < p.fields.size(); i++) {
            if (i > 0) sb.append(",");
            EmbedPayload.Field f = p.fields.get(i);
            sb.append("{\"name\":\"").append(esc(f.name)).append("\",\"value\":\"").append(esc(f.value)).append("\",\"inline\":").append(f.inline).append("}");
        }
        sb.append("]}]}");
        return sb.toString();
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
