package com.ttsstudio.ftopforge.discord;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class EmbedBuilderTest {

    @Test
    void colorHex_convertsToDecimalInt() {
        assertEquals(16766720, EmbedBuilder.parseColorHex("#FFD700"));
        assertEquals(16766720, EmbedBuilder.parseColorHex("FFD700"));
    }

    @Test
    void deltaField_positiveShowsUpArrowAndSign() {
        String f = EmbedBuilder.deltaField(1_000L, 800L);
        assertTrue(f.contains("▲"));
        assertTrue(f.contains("+"));
    }

    @Test
    void weeklyResetEmbed_includesPayoutsField() {
        EmbedBuilder.TopRow row = new EmbedBuilder.TopRow(1, "Dragones", 1_000_000L, 800_000L, true);
        EmbedBuilder.PayoutEntry p1 = new EmbedBuilder.PayoutEntry(1, "Dragones", "$1M");
        EmbedPayload payload = EmbedBuilder.weeklyReset(
            Collections.singletonList(row), Collections.singletonList(p1),
            "#FFD700", "footer text", null, null);
        String json = EmbedBuilder.toJson(payload);
        assertTrue(json.contains("Payouts"));
        assertTrue(json.contains("Dragones"));
        assertTrue(json.contains("$1M"));
    }

    @Test
    void roleMentionOnlyWhenKingChangeAndRoleSet() {
        EmbedBuilder.TopRow row = new EmbedBuilder.TopRow(1, "F", 1_000L, 0L, false);
        String withRole = EmbedBuilder.toJson(EmbedBuilder.kingChange(
            Collections.singletonList(row), "#FFD700", "f", null, "12345"));
        assertTrue(withRole.contains("<@&12345>"));
        String noRole = EmbedBuilder.toJson(EmbedBuilder.kingChange(
            Collections.singletonList(row), "#FFD700", "f", null, ""));
        assertFalse(noRole.contains("<@&"));
    }
}
