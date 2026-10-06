package com.ttsstudio.ftopforge.commands.hologram;

import com.ttsstudio.ftopforge.i18n.Messages;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class HologramSubcommandParsingTest {

    private Messages stubMessages() {
        Messages m = Mockito.mock(Messages.class);
        // Return the key itself when asked, so we can assert "what key would be sent".
        Mockito.when(m.get(Mockito.anyString())).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(m.get(Mockito.anyString(), Mockito.<String>any())).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(m.get(Mockito.anyString(), Mockito.<String>any(), Mockito.<String>any())).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(m.get(Mockito.anyString(), Mockito.<String>any(), Mockito.<String>any(), Mockito.<String>any(), Mockito.<String>any())).thenAnswer(inv -> inv.getArgument(0));
        return m;
    }

    @Test
    void emptyArgs_sendsUsage() {
        CommandSender s = mock(CommandSender.class);
        HologramSubcommand sub = new HologramSubcommand(null, stubMessages(), null);

        sub.handle(s, new String[] { "hologram" });

        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(s, atLeastOnce()).sendMessage(cap.capture());
        // At least one message should be the usage key.
        boolean foundUsage = cap.getAllValues().stream().anyMatch(v -> v.equals("hologram.usage"));
        org.junit.jupiter.api.Assertions.assertTrue(foundUsage, "expected hologram.usage message");
    }

    @Test
    void unknownSub_sendsUsage() {
        CommandSender s = mock(CommandSender.class);
        HologramSubcommand sub = new HologramSubcommand(null, stubMessages(), null);

        sub.handle(s, new String[] { "hologram", "frobnicate" });

        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(s, atLeastOnce()).sendMessage(cap.capture());
        boolean foundUsage = cap.getAllValues().stream().anyMatch(v -> v.equals("hologram.usage"));
        org.junit.jupiter.api.Assertions.assertTrue(foundUsage);
    }

    @Test
    void set_fromConsole_sendsNoPlayerError() {
        org.bukkit.command.CommandSender s = mock(org.bukkit.command.CommandSender.class);
        HologramSubcommand sub = new HologramSubcommand(null, stubMessages(), null);

        sub.handle(s, new String[] { "hologram", "set" });

        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(s, atLeastOnce()).sendMessage(cap.capture());
        boolean found = cap.getAllValues().stream().anyMatch(v -> v.equals("hologram.set-no-player"));
        org.junit.jupiter.api.Assertions.assertTrue(found);
    }

    @Test
    void move_tooFewArgs_sendsBadCoords() {
        CommandSender s = mock(CommandSender.class);
        HologramSubcommand sub = new HologramSubcommand(null, stubMessages(), null);

        sub.handle(s, new String[] { "hologram", "move", "world", "1" });

        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(s, atLeastOnce()).sendMessage(cap.capture());
        org.junit.jupiter.api.Assertions.assertTrue(
            cap.getAllValues().stream().anyMatch(v -> v.equals("hologram.move-bad-coords")));
    }

    @Test
    void move_badDouble_sendsBadCoords() {
        CommandSender s = mock(CommandSender.class);
        // Mock Bukkit to return null for any world lookup (simulates missing world in unit test env).
        try (org.mockito.MockedStatic<org.bukkit.Bukkit> bukkit = Mockito.mockStatic(org.bukkit.Bukkit.class)) {
            bukkit.when(() -> org.bukkit.Bukkit.getWorld(Mockito.anyString())).thenReturn(null);
            HologramSubcommand sub = new HologramSubcommand(null, stubMessages(), null);

            sub.handle(s, new String[] { "hologram", "move", "nonexistent_world", "x", "y", "z" });

            ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
            verify(s, atLeastOnce()).sendMessage(cap.capture());
            // Bukkit.getWorld returns null → move-bad-world.
            org.junit.jupiter.api.Assertions.assertTrue(
                cap.getAllValues().stream().anyMatch(v -> v.equals("hologram.move-bad-world")));
        }
    }
}
