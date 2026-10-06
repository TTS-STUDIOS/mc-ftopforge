package com.ttsstudio.ftopforge.commands.hologram;

import com.ttsstudio.ftopforge.FTopForgePlugin;
import com.ttsstudio.ftopforge.commands.FTopForgeTabCompleter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HologramTabCompletionTest {

    @Test
    void topLevel_withAdmin_includesAdminOnlySubs() {
        FTopForgePlugin plugin = mock(FTopForgePlugin.class);
        Command cmd = mock(Command.class);
        when(cmd.getName()).thenReturn("ftopforge");
        CommandSender s = mock(CommandSender.class);
        when(s.hasPermission("ftopforge.admin")).thenReturn(true);

        FTopForgeTabCompleter c = new FTopForgeTabCompleter(plugin);
        List<String> got = c.onTabComplete(s, cmd, "ftopforge", new String[] { "" });

        assertTrue(got.contains("hologram"));
        assertTrue(got.contains("recalc"));
        assertTrue(got.contains("reload"));
        assertTrue(got.contains("rewards"));
        assertTrue(got.contains("worth"));
    }

    @Test
    void topLevel_withoutAdmin_excludesAdminOnlySubs() {
        FTopForgePlugin plugin = mock(FTopForgePlugin.class);
        Command cmd = mock(Command.class);
        when(cmd.getName()).thenReturn("ftopforge");
        CommandSender s = mock(CommandSender.class);
        when(s.hasPermission("ftopforge.admin")).thenReturn(false);

        FTopForgeTabCompleter c = new FTopForgeTabCompleter(plugin);
        List<String> got = c.onTabComplete(s, cmd, "ftopforge", new String[] { "" });

        assertFalse(got.contains("hologram"));
        assertFalse(got.contains("recalc"));
        assertFalse(got.contains("reload"));
        assertTrue(got.contains("worth"));
        assertTrue(got.contains("version"));
    }

    @Test
    void hologram_secondArg_listsAllSevenSubcommands() {
        FTopForgePlugin plugin = mock(FTopForgePlugin.class);
        Command cmd = mock(Command.class);
        when(cmd.getName()).thenReturn("ftopforge");
        CommandSender s = mock(CommandSender.class);
        when(s.hasPermission("ftopforge.admin")).thenReturn(true);

        FTopForgeTabCompleter c = new FTopForgeTabCompleter(plugin);
        List<String> got = c.onTabComplete(s, cmd, "ftopforge", new String[] { "hologram", "" });

        assertTrue(got.contains("set"));
        assertTrue(got.contains("move"));
        assertTrue(got.contains("info"));
        assertTrue(got.contains("tp"));
        assertTrue(got.contains("disable"));
        assertTrue(got.contains("enable"));
        assertTrue(got.contains("refresh"));
        assertEquals(7, got.size());
    }

    @Test
    void hologram_prefixFilter_appliesPartialMatch() {
        FTopForgePlugin plugin = mock(FTopForgePlugin.class);
        Command cmd = mock(Command.class);
        when(cmd.getName()).thenReturn("ftopforge");
        CommandSender s = mock(CommandSender.class);
        when(s.hasPermission("ftopforge.admin")).thenReturn(true);

        FTopForgeTabCompleter c = new FTopForgeTabCompleter(plugin);
        List<String> got = c.onTabComplete(s, cmd, "ftopforge", new String[] { "hologram", "in" });

        assertEquals(1, got.size());
        assertEquals("info", got.get(0));
    }

    @Test
    void rewards_secondArg_listsNextRunHistory() {
        FTopForgePlugin plugin = mock(FTopForgePlugin.class);
        Command cmd = mock(Command.class);
        when(cmd.getName()).thenReturn("ftopforge");
        CommandSender s = mock(CommandSender.class);
        when(s.hasPermission("ftopforge.admin")).thenReturn(true);

        FTopForgeTabCompleter c = new FTopForgeTabCompleter(plugin);
        List<String> got = c.onTabComplete(s, cmd, "ftopforge", new String[] { "rewards", "" });

        assertTrue(got.contains("next"));
        assertTrue(got.contains("run"));
        assertTrue(got.contains("history"));
    }

    @Test
    void ftop_listsPageOrGui() {
        FTopForgePlugin plugin = mock(FTopForgePlugin.class);
        com.ttsstudio.ftopforge.core.TopCache tc = new com.ttsstudio.ftopforge.core.TopCache();
        when(plugin.topCache()).thenReturn(tc);
        Command cmd = mock(Command.class);
        when(cmd.getName()).thenReturn("ftop");
        CommandSender s = mock(CommandSender.class);

        FTopForgeTabCompleter c = new FTopForgeTabCompleter(plugin);
        List<String> got = c.onTabComplete(s, cmd, "ftop", new String[] { "" });

        assertTrue(got.contains("gui"));
    }
}
