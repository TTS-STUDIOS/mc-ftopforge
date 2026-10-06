package com.ttsstudio.ftopforge.discord;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dispatches Discord webhook events based on recalc outcomes.
 * Honors per-event enable flags and cooldowns via {@link EventCooldownStore}.
 */
public final class DiscordEventBus {

    public enum EventType { ON_RECALC_FINISHED, ON_TOP1_CHANGED, ON_TOP10_SHUFFLE, ON_WEEKLY_RESET }

    private final HttpPoster poster;
    private final EventCooldownStore cooldown;
    private final String webhookUrl;
    private final Map<EventType, Boolean> enabled;
    private final Map<EventType, Integer> cooldownHours;
    private final String colorHex;
    private final String thumbnail;
    private final String footer;
    private final String mentionRoleIdOnKingChange;
    private final Logger log;

    public DiscordEventBus(HttpPoster poster, EventCooldownStore cooldown, String webhookUrl,
                           Map<EventType, Boolean> enabled, Map<EventType, Integer> cooldownHours,
                           String colorHex, String thumbnail, String footer,
                           String mentionRoleIdOnKingChange, Logger log) {
        this.poster = poster;
        this.cooldown = cooldown;
        this.webhookUrl = webhookUrl;
        this.enabled = enabled;
        this.cooldownHours = cooldownHours;
        this.colorHex = colorHex;
        this.thumbnail = thumbnail;
        this.footer = footer;
        this.mentionRoleIdOnKingChange = mentionRoleIdOnKingChange;
        this.log = log;
    }

    public void onRecalcFinished(List<EmbedBuilder.TopRow> top10WithDeltas,
                                 String prevTop1Id, String currTop1Id,
                                 List<String> prevTop10Ids, List<String> currTop10Ids,
                                 long nowMs) {
        if (Boolean.TRUE.equals(enabled.get(EventType.ON_RECALC_FINISHED))) {
            tryFireAndPost(EventType.ON_RECALC_FINISHED, nowMs,
                EmbedBuilder.toJson(EmbedBuilder.recalcFinished(top10WithDeltas, colorHex, footer, thumbnail)));
        }
        if (currTop1Id != null && !currTop1Id.equals(prevTop1Id)
            && Boolean.TRUE.equals(enabled.get(EventType.ON_TOP1_CHANGED))) {
            tryFireAndPost(EventType.ON_TOP1_CHANGED, nowMs,
                EmbedBuilder.toJson(EmbedBuilder.kingChange(top10WithDeltas, colorHex, footer, thumbnail, mentionRoleIdOnKingChange)));
        }
        if (!java.util.Objects.equals(prevTop10Ids, currTop10Ids)
            && Boolean.TRUE.equals(enabled.get(EventType.ON_TOP10_SHUFFLE))) {
            tryFireAndPost(EventType.ON_TOP10_SHUFFLE, nowMs,
                EmbedBuilder.toJson(EmbedBuilder.top10Shuffle(top10WithDeltas, colorHex, footer, thumbnail)));
        }
    }

    public void onWeeklyReset(List<EmbedBuilder.TopRow> top10,
                              List<EmbedBuilder.PayoutEntry> payouts, long nowMs) {
        if (!Boolean.TRUE.equals(enabled.get(EventType.ON_WEEKLY_RESET))) return;
        tryFireAndPost(EventType.ON_WEEKLY_RESET, nowMs,
            EmbedBuilder.toJson(EmbedBuilder.weeklyReset(top10, payouts, colorHex, footer, thumbnail, null)));
    }

    private void tryFireAndPost(EventType type, long nowMs, String jsonBody) {
        try {
            int cd = cooldownHours.getOrDefault(type, 0);
            String key = type.name().toLowerCase().replace('_', '-');
            if (!cooldown.tryFire(key, cd, nowMs)) return;
        } catch (java.sql.SQLException e) {
            if (log != null) log.log(Level.WARNING, "[discord] cooldown store failed", e);
            return;
        }
        int status = poster.post(webhookUrl, jsonBody);
        if (status != 204 && status != -1 && log != null) {
            log.warning("[discord] webhook POST returned " + status);
        }
    }
}
