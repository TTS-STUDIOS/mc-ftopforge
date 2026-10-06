package com.ttsstudio.ftopforge.commands;

import com.ttsstudio.ftopforge.FTopForgePlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class FTopForgeTabCompleter implements TabCompleter {

    private static final List<String> TOP_PUBLIC = Arrays.asList("worth", "history", "forensics", "version");
    private static final List<String> TOP_ADMIN_ONLY = Arrays.asList("recalc", "reload", "rewards", "hologram");
    private static final List<String> HOLOGRAM_SUBS = Arrays.asList("set", "move", "info", "tp", "disable", "enable", "refresh");
    private static final List<String> REWARDS_SUBS = Arrays.asList("next", "run", "history");
    private static final List<String> FTOP_SUBS = Arrays.asList("gui", "1", "2", "3");

    private final FTopForgePlugin plugin;

    public FTopForgeTabCompleter(FTopForgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        try {
            if ("ftop".equalsIgnoreCase(cmd.getName())) return completeFtop(sender, args);
            return completeFtopforge(sender, args);
        } catch (Throwable t) {
            // Never let a tab completer bring down command dispatch.
            plugin.getLogger().warning("[tabcomplete] " + t.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> completeFtopforge(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(TOP_PUBLIC);
            if (sender.hasPermission("ftopforge.admin")) options.addAll(TOP_ADMIN_ONLY);
            return prefixMatch(args[0], options);
        }
        if (args.length >= 2) {
            String head = args[0].toLowerCase();
            if (head.equals("hologram")) {
                if (args.length == 2) return prefixMatch(args[1], HOLOGRAM_SUBS);
                if (args.length == 3 && args[1].equalsIgnoreCase("move")) {
                    List<String> worlds = new ArrayList<>();
                    for (World w : Bukkit.getWorlds()) worlds.add(w.getName());
                    return prefixMatch(args[2], worlds);
                }
                return Collections.emptyList();
            }
            if (head.equals("rewards") && args.length == 2) return prefixMatch(args[1], REWARDS_SUBS);
            if (head.equals("history") && args.length == 2) {
                List<String> opts = new ArrayList<>(topFactionTags());
                opts.add("export");
                return prefixMatch(args[1], opts);
            }
            if (head.equals("forensics") && args.length == 2) return prefixMatch(args[1], topFactionTags());
        }
        return Collections.emptyList();
    }

    private List<String> completeFtop(CommandSender sender, String[] args) {
        if (args.length == 1) return prefixMatch(args[0], FTOP_SUBS);
        return Collections.emptyList();
    }

    private List<String> topFactionTags() {
        List<String> tags = new ArrayList<>();
        try {
            for (com.ttsstudio.ftopforge.core.FactionSnapshot s : plugin.topCache().top(10)) {
                String tag = s.factionName();
                if (tag != null && !tag.isEmpty()) tags.add(tag);
            }
        } catch (Throwable ignored) {}
        return tags;
    }

    private static List<String> prefixMatch(String typed, List<String> all) {
        List<String> out = new ArrayList<>();
        StringUtil.copyPartialMatches(typed, all, out);
        Collections.sort(out);
        return out;
    }
}
