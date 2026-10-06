package com.ttsstudio.ftopforge.i18n;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MessagesTest {

    @Test
    void getReturnsResolvedStringWithPrefix() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("prefix", "&6[&eFTop&6] ");
        Map<String, Object> calc = new HashMap<>();
        calc.put("starting", "%prefix%&7Calculating...");
        raw.put("calculation", calc);
        Messages m = new Messages(raw, raw);
        String out = m.get("calculation.starting");
        assertTrue(out.contains("Calculating"), "should contain literal text");
        assertFalse(out.contains("%prefix%"), "should have substituted %prefix%");
        assertTrue(out.contains("[") && out.contains("FTop"), "prefix should be embedded");
    }

    @Test
    void getWithArgsSubstitutesNamedPlaceholders() {
        Map<String, Object> raw = new HashMap<>();
        Map<String, Object> cmd = new HashMap<>();
        Map<String, Object> ftop = new HashMap<>();
        ftop.put("line", "&6%rank%. &b%faction_name% &a$%value%");
        cmd.put("ftop", ftop);
        raw.put("command", cmd);
        raw.put("prefix", "");
        Messages m = new Messages(raw, raw);
        String out = m.get("command.ftop.line", "rank", "1", "faction_name", "Sparta", "value", "9999");
        assertTrue(out.contains("1.") && out.contains("Sparta") && out.contains("9999"));
        assertFalse(out.contains("%rank%"));
    }

    @Test
    void missingKeyFallsBackToDefaultLang() {
        Map<String, Object> en = new HashMap<>();
        en.put("prefix", "");
        Map<String, Object> only_en = new HashMap<>();
        only_en.put("starting", "EN starting");
        en.put("calculation", only_en);
        Map<String, Object> es = new HashMap<>();
        es.put("prefix", "");
        // ES is missing the calculation.starting key
        Messages m = new Messages(es, en);
        String out = m.get("calculation.starting");
        assertEquals("EN starting", out);
    }

    @Test
    void missingEverywhereReturnsKeyMarker() {
        Map<String, Object> empty = new HashMap<>();
        Messages m = new Messages(empty, empty);
        String out = m.get("nope.does.not.exist");
        assertTrue(out.contains("nope.does.not.exist"), "should echo missing key for debugging");
    }

    // -----------------------------------------------------------------------
    // Helpers for YAML-backed tests
    // -----------------------------------------------------------------------

    private static Messages loadFromYaml(String lang) {
        Map<String, Object> primary = loadYaml("messages-" + lang + ".yml");
        Map<String, Object> fallback = "es".equalsIgnoreCase(lang)
                ? primary
                : loadYaml("messages-es.yml");
        return new Messages(primary, fallback);
    }

    private static Map<String, Object> loadYaml(String fileName) {
        InputStream in = MessagesTest.class.getClassLoader().getResourceAsStream(fileName);
        if (in == null) return new HashMap<>();
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(reader);
            return toNestedMap(yaml);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toNestedMap(org.bukkit.configuration.ConfigurationSection section) {
        Map<String, Object> result = new HashMap<>();
        for (String key : section.getKeys(false)) {
            if (section.isConfigurationSection(key)) {
                result.put(key, toNestedMap(section.getConfigurationSection(key)));
            } else {
                result.put(key, section.get(key));
            }
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Task 6: gui.top.* and gui.breakdown.* keys
    // -----------------------------------------------------------------------

    @Test
    void gui_top_keys_resolveInEs() {
        Messages m = loadFromYaml("es");
        assertNotEquals("<missing:gui.top.title>", m.get("gui.top.title", "page", "1", "total", "1"));
        assertNotEquals("<missing:gui.top.skull.name>", m.get("gui.top.skull.name", "rank", "1", "faction_name", "X"));
        assertNotEquals("<missing:gui.top.skull.lore_leader>", m.get("gui.top.skull.lore_leader", "leader_name", "X"));
        assertNotEquals("<missing:gui.top.skull.lore_value>", m.get("gui.top.skull.lore_value", "formatted_value", "1k"));
        assertNotEquals("<missing:gui.top.skull.lore_action>", m.get("gui.top.skull.lore_action"));
        assertNotEquals("<missing:gui.top.info.name>", m.get("gui.top.info.name"));
        assertNotEquals("<missing:gui.top.info.lore_last>", m.get("gui.top.info.lore_last", "relative_time", "hace 1m"));
        assertNotEquals("<missing:gui.top.info.lore_next>", m.get("gui.top.info.lore_next", "countdown", "5m"));
        assertNotEquals("<missing:gui.top.info.lore_total>", m.get("gui.top.info.lore_total", "total", "12"));
        assertNotEquals("<missing:gui.top.nav.prev>", m.get("gui.top.nav.prev"));
        assertNotEquals("<missing:gui.top.nav.next>", m.get("gui.top.nav.next"));
        assertNotEquals("<missing:gui.top.nav.close>", m.get("gui.top.nav.close"));
    }

    @Test
    void gui_breakdown_keys_resolveInEs() {
        Messages m = loadFromYaml("es");
        assertNotEquals("<missing:gui.breakdown.title>", m.get("gui.breakdown.title", "faction", "X"));
        assertNotEquals("<missing:gui.breakdown.summary.name>", m.get("gui.breakdown.summary.name", "faction_name", "X"));
        for (String cat : new String[]{"chunks", "spawners", "items", "blocks", "balance"}) {
            assertNotEquals("<missing:gui.breakdown.cat." + cat + ".name>",
                    m.get("gui.breakdown.cat." + cat + ".name"));
        }
        assertNotEquals("<missing:gui.breakdown.back>", m.get("gui.breakdown.back"));
        assertNotEquals("<missing:gui.breakdown.close>", m.get("gui.breakdown.close"));
    }

    @Test
    void gui_keys_resolveInEn() {
        Messages m = loadFromYaml("en");
        assertNotEquals("<missing:gui.top.title>", m.get("gui.top.title", "page", "1", "total", "1"));
        assertNotEquals("<missing:gui.breakdown.title>", m.get("gui.breakdown.title", "faction", "X"));
    }

    @Test
    void errors_mustBePlayer_resolves() {
        Messages m = loadFromYaml("es");
        assertNotEquals("<missing:errors.must-be-player>", m.get("errors.must-be-player"));
    }

    @Test
    void resolvesNestedKey_whenLoadedViaYamlGetValuesTrue_productionPath() throws Exception {
        // Regression test for the Bundle A → 1.2.0 bug:
        // MessagesLoader loads YAML via yaml.getValues(true), which produces a flat map
        // with dot-path keys + ConfigurationSection refs. The unit-test toNestedMap helper
        // disguised the bug. This test exercises the REAL production code path.
        java.io.InputStream in = MessagesTest.class.getClassLoader().getResourceAsStream("messages-es.yml");
        org.bukkit.configuration.file.YamlConfiguration yaml = new org.bukkit.configuration.file.YamlConfiguration();
        yaml.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
        java.util.Map<String, Object> primary = yaml.getValues(true);
        Messages m = new Messages(primary, primary);
        // gui.top.empty must resolve (it's the action bar message on /ftop with empty top)
        String empty = m.get("gui.top.empty");
        assertFalse(empty.startsWith("<missing:"), "gui.top.empty should NOT be missing, got: " + empty);
        assertTrue(empty.contains("ranking") || empty.contains("recálculo"), "should have meaningful content, got: " + empty);
        // gui.top.title is another nested key that must work
        String title = m.get("gui.top.title", "page", "1", "total", "1");
        assertFalse(title.startsWith("<missing:"), "gui.top.title should NOT be missing, got: " + title);
    }
}
