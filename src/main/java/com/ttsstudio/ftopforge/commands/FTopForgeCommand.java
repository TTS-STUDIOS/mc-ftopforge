package com.ttsstudio.ftopforge.commands;

import com.ttsstudio.ftopforge.FTopForgePlugin;
import com.ttsstudio.ftopforge.core.FactionSnapshot;
import com.ttsstudio.ftopforge.core.RecalcRunner;
import com.ttsstudio.ftopforge.forensics.ForensicsDao;
import com.ttsstudio.ftopforge.forensics.ForensicsEvent;
import com.ttsstudio.ftopforge.history.AsciiChart;
import com.ttsstudio.ftopforge.history.CsvExporter;
import com.ttsstudio.ftopforge.history.HistoryDao;
import com.ttsstudio.ftopforge.history.HistoryPoint;
import com.ttsstudio.ftopforge.commands.hologram.HologramConfigWriter;
import com.ttsstudio.ftopforge.commands.hologram.HologramSubcommand;
import com.ttsstudio.ftopforge.i18n.Messages;
import com.ttsstudio.ftopforge.rewards.CronScheduler;
import com.ttsstudio.ftopforge.rewards.PayoutRunner;
import com.ttsstudio.ftopforge.rewards.SeasonRow;
import com.ttsstudio.ftopforge.rewards.SeasonsDao;
import com.ttsstudio.ftopforge.util.MoneyFormat;
import com.ttsstudio.ftopforge.worth.WorthMessageBuilder;
import com.ttsstudio.ftopforge.worth.WorthQuery;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.io.File;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class FTopForgeCommand implements CommandExecutor {

    private final FTopForgePlugin plugin;
    private final RecalcRunner runner;
    private final Messages msg;
    private final HologramSubcommand hologramSub;

    public FTopForgeCommand(FTopForgePlugin plugin, RecalcRunner runner, Messages msg) {
        this.plugin = plugin;
        this.runner = runner;
        this.msg = msg;
        HologramConfigWriter writer = new HologramConfigWriter(
            plugin::getConfig,
            new java.io.File(plugin.getDataFolder(), "config.yml"),
            plugin.getDataFolder());
        this.hologramSub = new HologramSubcommand(plugin, msg, writer);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&7/ftopforge <worth|history|forensics|recalc|rewards|reload|version|hologram> [args]"));
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "version":
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6FTopForge &7v" + plugin.getDescription().getVersion()));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&7" + com.ttsstudio.ftopforge.meta.DynamicBanner.modulesLine(plugin.runtimeState())));
                return true;
            case "recalc":
                if (!sender.hasPermission("ftopforge.admin")) { sender.sendMessage(msg.get("errors.no-permission")); return true; }
                if (runner.trigger()) sender.sendMessage(msg.get("admin.recalc-triggered"));
                else sender.sendMessage(msg.get("errors.recalc-in-progress"));
                return true;
            case "reload":
                if (!sender.hasPermission("ftopforge.admin")) { sender.sendMessage(msg.get("errors.no-permission")); return true; }
                plugin.reloadConfig();
                plugin.reloadMessages();
                plugin.shutdownHolograms();
                plugin.bootstrapHolograms();
                sender.sendMessage(msg.get("admin.reloaded"));
                return true;
            case "worth":
                return handleWorth(sender);
            case "history":
                if (!sender.hasPermission("ftopforge.history")) { sender.sendMessage(msg.get("errors.no-permission")); return true; }
                return handleHistory(sender, args);
            case "forensics":
                if (!sender.hasPermission("ftopforge.forensics")) { sender.sendMessage(msg.get("errors.no-permission")); return true; }
                return handleForensics(sender, args);
            case "rewards":
                return handleRewards(sender, args);
            case "hologram":
                if (!sender.hasPermission("ftopforge.admin")) { sender.sendMessage(msg.get("errors.no-permission")); return true; }
                hologramSub.handle(sender, args);
                return true;
            default:
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Unknown subcommand: " + sub));
                return true;
        }
    }

    // --- /ftopforge worth (uses item in hand or block player is looking at) ---
    private boolean handleWorth(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg.get("worth.console-only"));
            return true;
        }
        Player p = (Player) sender;
        @SuppressWarnings("deprecation")
        ItemStack hand = p.getItemInHand();
        String material = null;
        int amount = 1;
        String spawnerType = null;

        if (hand != null && hand.getType() != org.bukkit.Material.AIR) {
            material = hand.getType().name();
            amount = hand.getAmount();
            // Detect spawner entity type from BlockStateMeta if applicable.
            if (hand.hasItemMeta() && hand.getItemMeta() instanceof BlockStateMeta) {
                BlockStateMeta bsm = (BlockStateMeta) hand.getItemMeta();
                if (bsm.getBlockState() instanceof CreatureSpawner) {
                    CreatureSpawner cs = (CreatureSpawner) bsm.getBlockState();
                    if (cs.getSpawnedType() != null) spawnerType = cs.getSpawnedType().name();
                }
            }
        } else {
            // Try looking at a block within 8 blocks. Use null transparent-set with explicit cast for 1.8 API.
            @SuppressWarnings("deprecation")
            Block target = p.getTargetBlock((java.util.HashSet<Byte>) null, 8);
            if (target != null && target.getType() != org.bukkit.Material.AIR) {
                material = target.getType().name();
                amount = 1;
                if (target.getState() instanceof CreatureSpawner) {
                    CreatureSpawner cs = (CreatureSpawner) target.getState();
                    if (cs.getSpawnedType() != null) spawnerType = cs.getSpawnedType().name();
                }
            }
        }

        if (material == null) {
            sender.sendMessage(msg.get("worth.no-target"));
            return true;
        }

        WorthQuery.Result r = WorthQuery.compute(material, amount, spawnerType, plugin.itemPrices());
        if (r == null) {
            sender.sendMessage(msg.get("worth.no-target"));
            return true;
        }
        for (String line : WorthMessageBuilder.build(r)) sender.sendMessage(line);
        return true;
    }

    // --- /ftopforge history <faction> [weeks] | history export csv ---
    private boolean handleHistory(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(msg.get("errors.not-enough-args",
                "usage", "/ftopforge history <faction> [weeks] | /ftopforge history export csv"));
            return true;
        }
        HistoryDao dao = plugin.historyDao();
        if (dao == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cHistory module disabled."));
            return true;
        }

        // Export branch.
        if (args[1].equalsIgnoreCase("export")) {
            if (!sender.hasPermission("ftopforge.admin")) {
                sender.sendMessage(msg.get("errors.no-permission"));
                return true;
            }
            CsvExporter ex = plugin.csvExporter();
            if (ex == null) {
                sender.sendMessage(msg.get("history.export-failed", "reason", "exporter unavailable"));
                return true;
            }
            try {
                File out = ex.exportAll();
                sender.sendMessage(msg.get("history.export-success", "path", out.getAbsolutePath()));
            } catch (Exception e) {
                sender.sendMessage(msg.get("history.export-failed", "reason", e.getMessage()));
            }
            return true;
        }

        String factionArg = args[1];
        int weeks = 12;
        if (args.length >= 3) {
            try { weeks = Math.max(1, Math.min(52, Integer.parseInt(args[2]))); }
            catch (NumberFormatException ignored) {}
        }
        String factionId = resolveFactionId(factionArg);
        if (factionId == null) {
            sender.sendMessage(msg.get("errors.faction-not-found", "name", factionArg));
            return true;
        }
        long now = System.currentTimeMillis();
        long from = now - (long) weeks * 7L * 86_400_000L;
        try {
            List<HistoryPoint> points = dao.listFor(factionId, from, now);
            String displayName = points.isEmpty() ? factionArg : points.get(0).factionName();
            if (points.isEmpty()) {
                sender.sendMessage(msg.get("history.empty", "faction", displayName));
                return true;
            }
            for (String line : AsciiChart.render(displayName, points, weeks)) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
            }
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cHistory query failed: " + e.getMessage()));
        }
        return true;
    }

    // --- /ftopforge forensics <faction> [page] [filter] ---
    private boolean handleForensics(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(msg.get("errors.not-enough-args",
                "usage", "/ftopforge forensics <faction> [page] [insider]"));
            return true;
        }
        ForensicsDao dao = plugin.forensicsDao();
        if (dao == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cForensics module disabled."));
            return true;
        }
        String factionArg = args[1];
        int page = 0;
        boolean insiderOnly = false;
        ForensicsEvent.Action actionFilter = null;
        if (args.length >= 3) {
            try { page = Math.max(0, Integer.parseInt(args[2]) - 1); }
            catch (NumberFormatException ignored) {}
        }
        if (args.length >= 4) {
            String f = args[3].toLowerCase();
            if (f.equals("insider")) insiderOnly = true;
            else if (f.equals("place")) actionFilter = ForensicsEvent.Action.PLACE;
            else if (f.equals("break")) actionFilter = ForensicsEvent.Action.BREAK;
        }
        String factionId = resolveFactionId(factionArg);
        if (factionId == null) {
            sender.sendMessage(msg.get("errors.faction-not-found", "name", factionArg));
            return true;
        }
        int pageSize = 10;
        try {
            List<ForensicsEvent> rows = dao.queryByFaction(factionId, actionFilter, insiderOnly, page, pageSize);
            if (rows.isEmpty()) {
                sender.sendMessage(msg.get("forensics.no-events", "faction", factionArg));
                return true;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (ForensicsEvent e : rows) {
                // An insider is a MEMBER touching the valuables of their own faction.
                String insiderTag = e.isMember ? msg.get("forensics.insider-tag") : "";
                String line = msg.get("forensics.event-line",
                    "ts", sdf.format(new Date(e.ts)),
                    "action", e.action.name(),
                    "material", e.material,
                    "world", e.world,
                    "x", String.valueOf(e.x),
                    "y", String.valueOf(e.y),
                    "z", String.valueOf(e.z),
                    "player", e.playerName,
                    "insider_tag", insiderTag);
                sender.sendMessage(line);
            }
            sender.sendMessage(msg.get("forensics.paginated-footer",
                "page", String.valueOf(page + 1),
                "total", "?",
                "faction", factionArg,
                "nextPage", String.valueOf(page + 2)));
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cForensics query failed: " + e.getMessage()));
        }
        return true;
    }

    // --- /ftopforge rewards next | run | history [page] ---
    private boolean handleRewards(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(msg.get("errors.not-enough-args",
                "usage", "/ftopforge rewards <next|run|history [page]>"));
            return true;
        }
        String sub = args[1].toLowerCase();
        switch (sub) {
            case "next": {
                TimeZone tz = plugin.payoutTimeZone();
                long nextMs = CronScheduler.nextRunAt(plugin.payoutDayOfWeek(), plugin.payoutHour(),
                    plugin.payoutMinute(), tz, System.currentTimeMillis());
                long remaining = (nextMs - System.currentTimeMillis()) / 1000L;
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                sdf.setTimeZone(tz);
                String weekday = weekdayName(plugin.payoutDayOfWeek());
                sender.sendMessage(msg.get("rewards.next-payout",
                    "weekday", weekday,
                    "time", sdf.format(new Date(nextMs)),
                    "countdown", humanSeconds(remaining)));
                return true;
            }
            case "run": {
                if (!sender.hasPermission("ftopforge.admin")) {
                    sender.sendMessage(msg.get("errors.no-permission"));
                    return true;
                }
                PayoutRunner pr = plugin.payoutRunner();
                if (pr == null) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cRewards module disabled."));
                    return true;
                }
                java.util.ArrayList<FactionSnapshot> top = new java.util.ArrayList<>(plugin.topCache().top(10));
                if (pr.run(top, System.currentTimeMillis())) {
                    sender.sendMessage(msg.get("admin.rewards-triggered"));
                    sender.sendMessage(msg.get("rewards.manual-trigger", "admin", sender.getName()));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPayout already in progress."));
                }
                return true;
            }
            case "history": {
                SeasonsDao sd = plugin.seasonsDao();
                if (sd == null) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cRewards module disabled."));
                    return true;
                }
                int page = 0;
                if (args.length >= 3) {
                    try { page = Math.max(0, Integer.parseInt(args[2]) - 1); }
                    catch (NumberFormatException ignored) {}
                }
                try {
                    List<SeasonRow> rows = sd.list(page, 10);
                    if (rows.isEmpty()) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7No season history yet."));
                        return true;
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    for (SeasonRow r : rows) {
                        String top1Name = extractTop1Name(r.top10Json());
                        sender.sendMessage(msg.get("rewards.history-line",
                            "season", String.valueOf(r.seasonId()),
                            "date", sdf.format(new Date(r.endedAt())),
                            "faction", top1Name,
                            "payouts", countPayoutLines(r.payoutsLog())));
                    }
                } catch (SQLException e) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cSeason query failed: " + e.getMessage()));
                }
                return true;
            }
            default:
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&7Unknown rewards subcommand: " + sub));
                return true;
        }
    }

    // --- helpers ---

    /** Resolve faction by tag/name; first checks current top, then iterates real factions. */
    private String resolveFactionId(String nameOrId) {
        if (nameOrId == null) return null;
        // Top-cache fast path.
        List<FactionSnapshot> all = plugin.topCache().top(plugin.topCache().size());
        for (FactionSnapshot s : all) {
            if (s.factionId().equalsIgnoreCase(nameOrId) || s.factionName().equalsIgnoreCase(nameOrId)) {
                return s.factionId();
            }
        }
        // Fallback: scan factions hook.
        try {
            for (com.massivecraft.factions.Faction f : plugin.factionsHook().allRealFactions()) {
                if (f.getId().equalsIgnoreCase(nameOrId) || f.getTag().equalsIgnoreCase(nameOrId)) {
                    return f.getId();
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static String weekdayName(int dow) {
        switch (dow) {
            case Calendar.MONDAY: return "MONDAY";
            case Calendar.TUESDAY: return "TUESDAY";
            case Calendar.WEDNESDAY: return "WEDNESDAY";
            case Calendar.THURSDAY: return "THURSDAY";
            case Calendar.FRIDAY: return "FRIDAY";
            case Calendar.SATURDAY: return "SATURDAY";
            case Calendar.SUNDAY: return "SUNDAY";
            default: return "?";
        }
    }

    private static String humanSeconds(long secs) {
        if (secs < 0) return "--";
        long days = secs / 86400; secs %= 86400;
        long hours = secs / 3600; secs %= 3600;
        long mins = secs / 60;
        if (days > 0) return days + "d " + hours + "h " + mins + "m";
        if (hours > 0) return hours + "h " + mins + "m";
        return mins + "m";
    }

    private static String extractTop1Name(String json) {
        if (json == null || json.isEmpty()) return "--";
        int idx = json.indexOf("\"faction_name\":\"");
        if (idx < 0) return "--";
        int start = idx + "\"faction_name\":\"".length();
        int end = json.indexOf("\"", start);
        if (end < 0) return "--";
        return json.substring(start, end);
    }

    private static String countPayoutLines(String log) {
        if (log == null || log.isEmpty()) return "0";
        int count = 0;
        for (int i = 0; i < log.length(); i++) if (log.charAt(i) == '\n') count++;
        return String.valueOf(count);
    }

    @SuppressWarnings("unused")
    private static String formatMoney(long n) {
        return MoneyFormat.shortFmt(n);
    }
}
