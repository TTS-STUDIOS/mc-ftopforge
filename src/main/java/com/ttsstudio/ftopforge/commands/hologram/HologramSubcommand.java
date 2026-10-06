package com.ttsstudio.ftopforge.commands.hologram;

import com.ttsstudio.ftopforge.FTopForgePlugin;
import com.ttsstudio.ftopforge.i18n.Messages;
import org.bukkit.command.CommandSender;

/**
 * Orchestrates /ftopforge hologram <set|move|info|tp|disable|enable|refresh>.
 * Plain dispatcher class — branches live in this same file as private methods.
 */
public final class HologramSubcommand {

    private final FTopForgePlugin plugin;
    private final Messages msg;
    private final HologramConfigWriter writer;

    public HologramSubcommand(FTopForgePlugin plugin, Messages msg, HologramConfigWriter writer) {
        this.plugin = plugin;
        this.msg = msg;
        this.writer = writer;
    }

    /** args[0] is always "hologram"; subcommand sits in args[1] when present. */
    public void handle(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(msg.get("hologram.usage")); return; }
        String sub = args[1].toLowerCase();
        switch (sub) {
            case "set":     handleSet(sender, args); return;
            case "move":    handleMove(sender, args); return;
            case "info":    handleInfo(sender, args); return;
            case "tp":      handleTp(sender, args); return;
            case "disable": handleDisable(sender, args); return;
            case "enable":  handleEnable(sender, args); return;
            case "refresh": handleRefresh(sender, args); return;
            default:        sender.sendMessage(msg.get("hologram.usage")); return;
        }
    }

    private void handleSet(CommandSender s, String[] args) {
        if (!(s instanceof org.bukkit.entity.Player)) {
            s.sendMessage(msg.get("hologram.set-no-player"));
            return;
        }
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) s;
        org.bukkit.Location loc = p.getLocation();
        String wname = loc.getWorld().getName();
        boolean wasFirstBackup = !new java.io.File(plugin.getDataFolder(), "config.yml.bak").exists();
        boolean ok = writer.writeLocation(wname, loc.getX(), loc.getY(), loc.getZ());
        if (!ok) {
            s.sendMessage(msg.get("hologram.config-write-failed"));
            return;
        }
        if (wasFirstBackup) s.sendMessage(msg.get("hologram.config-backup"));
        plugin.shutdownHolograms();
        plugin.bootstrapHolograms();
        if (plugin.holograms() == null) s.sendMessage(msg.get("hologram.set-no-engine"));
        s.sendMessage(msg.get("hologram.set-success",
            "world", wname,
            "x", String.format(java.util.Locale.US, "%.1f", loc.getX()),
            "y", String.format(java.util.Locale.US, "%.1f", loc.getY()),
            "z", String.format(java.util.Locale.US, "%.1f", loc.getZ())));
    }
    private void handleMove(CommandSender s, String[] args) {
        // expected: args[0]=hologram args[1]=move args[2..5]=world x y z
        if (args.length < 6) {
            s.sendMessage(msg.get("hologram.move-bad-coords"));
            return;
        }
        String wname = args[2];
        if (org.bukkit.Bukkit.getWorld(wname) == null) {
            s.sendMessage(msg.get("hologram.move-bad-world", "world", wname));
            return;
        }
        double x, y, z;
        try {
            x = Double.parseDouble(args[3]);
            y = Double.parseDouble(args[4]);
            z = Double.parseDouble(args[5]);
        } catch (NumberFormatException e) {
            s.sendMessage(msg.get("hologram.move-bad-coords"));
            return;
        }
        boolean wasFirstBackup = !new java.io.File(plugin.getDataFolder(), "config.yml.bak").exists();
        boolean ok = writer.writeLocation(wname, x, y, z);
        if (!ok) { s.sendMessage(msg.get("hologram.config-write-failed")); return; }
        if (wasFirstBackup) s.sendMessage(msg.get("hologram.config-backup"));
        plugin.shutdownHolograms();
        plugin.bootstrapHolograms();
        if (plugin.holograms() == null) s.sendMessage(msg.get("hologram.set-no-engine"));
        s.sendMessage(msg.get("hologram.set-success",
            "world", wname,
            "x", String.format(java.util.Locale.US, "%.1f", x),
            "y", String.format(java.util.Locale.US, "%.1f", y),
            "z", String.format(java.util.Locale.US, "%.1f", z)));
    }
    private void handleInfo(CommandSender s, String[] args) {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        com.ttsstudio.ftopforge.holograms.HologramService svc = plugin.holograms();
        boolean enabled = cfg.getBoolean("holograms.enabled", true);
        s.sendMessage(msg.get("hologram.info-header"));

        if (svc != null && svc.isRunning()) {
            org.bukkit.Location loc = svc.currentLocation();
            String wname = loc != null && loc.getWorld() != null ? loc.getWorld().getName() : cfg.getString("holograms.location.world", "world");
            double x = loc != null ? loc.getX() : cfg.getDouble("holograms.location.x", 0d);
            double y = loc != null ? loc.getY() : cfg.getDouble("holograms.location.y", 80d);
            double z = loc != null ? loc.getZ() : cfg.getDouble("holograms.location.z", 0d);
            s.sendMessage(msg.get("hologram.info-line-engine", "engine", plugin.runtimeState().holoEngineName != null ? plugin.runtimeState().holoEngineName : "—"));
            s.sendMessage(msg.get("hologram.info-line-location",
                "world", wname,
                "x", String.format(java.util.Locale.US, "%.1f", x),
                "y", String.format(java.util.Locale.US, "%.1f", y),
                "z", String.format(java.util.Locale.US, "%.1f", z)));
            s.sendMessage(msg.get("hologram.info-line-enabled", "state", String.valueOf(enabled)));
            s.sendMessage(msg.get("hologram.info-line-refresh", "seconds", String.valueOf(cfg.getInt("holograms.refresh-interval-seconds", 60))));
            java.util.List<String> tmpl = cfg.getStringList("holograms.format");
            s.sendMessage(msg.get("hologram.info-line-lines", "count", String.valueOf(tmpl.size())));
        } else {
            s.sendMessage(msg.get("hologram.info-line-engine", "engine", "—"));
            s.sendMessage(msg.get("hologram.info-line-location",
                "world", cfg.getString("holograms.location.world", "world"),
                "x", String.format(java.util.Locale.US, "%.1f", cfg.getDouble("holograms.location.x", 0d)),
                "y", String.format(java.util.Locale.US, "%.1f", cfg.getDouble("holograms.location.y", 80d)),
                "z", String.format(java.util.Locale.US, "%.1f", cfg.getDouble("holograms.location.z", 0d))));
            s.sendMessage(msg.get("hologram.info-line-enabled", "state", String.valueOf(enabled)));
            if (!enabled) s.sendMessage(msg.get("hologram.info-disabled"));
        }
    }
    private void handleTp(CommandSender s, String[] args) {
        if (!(s instanceof org.bukkit.entity.Player)) {
            s.sendMessage(msg.get("hologram.tp-no-player"));
            return;
        }
        com.ttsstudio.ftopforge.holograms.HologramService svc = plugin.holograms();
        if (svc == null || !svc.isRunning()) {
            s.sendMessage(msg.get("hologram.tp-no-engine"));
            return;
        }
        org.bukkit.Location loc = svc.currentLocation();
        if (loc == null) {
            s.sendMessage(msg.get("hologram.tp-no-engine"));
            return;
        }
        ((org.bukkit.entity.Player) s).teleport(loc);
        s.sendMessage(msg.get("hologram.tp-success"));
    }
    private void handleEnable(CommandSender s, String[] args) {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        if (cfg.getBoolean("holograms.enabled", true) && plugin.holograms() != null && plugin.holograms().isRunning()) {
            s.sendMessage(msg.get("hologram.enable-already"));
            return;
        }
        boolean wasFirstBackup = !new java.io.File(plugin.getDataFolder(), "config.yml.bak").exists();
        boolean ok = writer.writeEnabled(true);
        if (!ok) { s.sendMessage(msg.get("hologram.config-write-failed")); return; }
        if (wasFirstBackup) s.sendMessage(msg.get("hologram.config-backup"));
        plugin.shutdownHolograms();
        plugin.bootstrapHolograms();
        if (plugin.holograms() == null) {
            s.sendMessage(msg.get("hologram.enable-no-engine"));
            return;
        }
        s.sendMessage(msg.get("hologram.enable-success"));
    }
    private void handleDisable(CommandSender s, String[] args) {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("holograms.enabled", true) && (plugin.holograms() == null || !plugin.holograms().isRunning())) {
            s.sendMessage(msg.get("hologram.disable-already"));
            return;
        }
        boolean wasFirstBackup = !new java.io.File(plugin.getDataFolder(), "config.yml.bak").exists();
        boolean ok = writer.writeEnabled(false);
        if (!ok) { s.sendMessage(msg.get("hologram.config-write-failed")); return; }
        if (wasFirstBackup) s.sendMessage(msg.get("hologram.config-backup"));
        plugin.shutdownHolograms();
        s.sendMessage(msg.get("hologram.disable-success"));
    }
    private void handleRefresh(CommandSender s, String[] args) {
        com.ttsstudio.ftopforge.holograms.HologramService svc = plugin.holograms();
        if (svc == null || !svc.isRunning()) {
            s.sendMessage(msg.get("hologram.refresh-not-running"));
            return;
        }
        svc.forceRefresh();
        s.sendMessage(msg.get("hologram.refresh-success"));
    }
}
