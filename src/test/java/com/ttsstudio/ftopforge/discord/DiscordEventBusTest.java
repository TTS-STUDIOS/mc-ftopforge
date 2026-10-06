package com.ttsstudio.ftopforge.discord;

import com.ttsstudio.ftopforge.discord.DiscordEventBus.EventType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DiscordEventBusTest {

    static class RecorderPoster implements HttpPoster {
        final List<String> bodies = new ArrayList<>();
        @Override public int post(String url, String body) { bodies.add(body); return 204; }
    }

    static EventCooldownStore alwaysAllow() {
        return new EventCooldownStore(null) {
            @Override public boolean tryFire(String e, int c, long n) { return true; }
        };
    }

    private static Map<EventType, Boolean> mkEnabled(EventType... ts) {
        Map<EventType, Boolean> m = new EnumMap<>(EventType.class);
        for (EventType t : EventType.values()) m.put(t, false);
        for (EventType t : ts) m.put(t, true);
        return m;
    }

    private static Map<EventType, Integer> mkCooldowns() {
        Map<EventType, Integer> m = new EnumMap<>(EventType.class);
        for (EventType t : EventType.values()) m.put(t, 0);
        return m;
    }

    @Test
    void top1Unchanged_doesNotFireOnTop1Changed() {
        RecorderPoster p = new RecorderPoster();
        Map<EventType, Boolean> enabled = mkEnabled(EventType.ON_TOP1_CHANGED);
        DiscordEventBus bus = new DiscordEventBus(p, alwaysAllow(), "url",
            enabled, mkCooldowns(), "#FFD700", null, "f", "", null);
        EmbedBuilder.TopRow row = new EmbedBuilder.TopRow(1, "F", 100L, 100L, true);
        bus.onRecalcFinished(Collections.singletonList(row),
            "A", "A",
            Arrays.asList("A"), Arrays.asList("A"),
            1000L);
        assertFalse(p.bodies.stream().anyMatch(s -> s.contains("Nuevo Rey")));
    }

    @Test
    void top1Changed_firesOnTop1Changed() {
        RecorderPoster p = new RecorderPoster();
        Map<EventType, Boolean> enabled = mkEnabled(EventType.ON_TOP1_CHANGED);
        DiscordEventBus bus = new DiscordEventBus(p, alwaysAllow(), "url",
            enabled, mkCooldowns(), "#FFD700", null, "f", "", null);
        EmbedBuilder.TopRow row = new EmbedBuilder.TopRow(1, "F", 100L, 100L, true);
        bus.onRecalcFinished(Collections.singletonList(row),
            "A", "B",
            Arrays.asList("A"), Arrays.asList("B"),
            1000L);
        assertTrue(p.bodies.stream().anyMatch(s -> s.contains("Nuevo Rey")));
    }

    @Test
    void top10ShuffleDetected_whenOrderDiffers() {
        RecorderPoster p = new RecorderPoster();
        Map<EventType, Boolean> enabled = mkEnabled(EventType.ON_TOP10_SHUFFLE);
        DiscordEventBus bus = new DiscordEventBus(p, alwaysAllow(), "url",
            enabled, mkCooldowns(), "#FFD700", null, "f", "", null);
        EmbedBuilder.TopRow row = new EmbedBuilder.TopRow(1, "F", 100L, 100L, true);
        bus.onRecalcFinished(Collections.singletonList(row),
            "A", "A",
            Arrays.asList("A", "B", "C"), Arrays.asList("B", "A", "C"),
            1000L);
        assertTrue(p.bodies.stream().anyMatch(s -> s.contains("Reordenado")));
    }

    @Test
    void top10SameOrder_noShuffleEvent() {
        RecorderPoster p = new RecorderPoster();
        Map<EventType, Boolean> enabled = mkEnabled(EventType.ON_TOP10_SHUFFLE);
        DiscordEventBus bus = new DiscordEventBus(p, alwaysAllow(), "url",
            enabled, mkCooldowns(), "#FFD700", null, "f", "", null);
        EmbedBuilder.TopRow row = new EmbedBuilder.TopRow(1, "F", 100L, 100L, true);
        List<String> ids = Arrays.asList("A", "B", "C");
        bus.onRecalcFinished(Collections.singletonList(row),
            "A", "A", ids, ids, 1000L);
        assertTrue(p.bodies.isEmpty());
    }
}
