package com.ttsstudio.ftopforge.holograms;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class HologramService {

    public interface PluginPresenceChecker { boolean isEnabled(String pluginName); }

    public static HologramEngine detect(String configEngine,
                                        PluginPresenceChecker checker,
                                        Plugin plugin,
                                        Logger log) {
        String pref = configEngine == null ? "auto" : configEngine.toLowerCase();
        boolean dh = checker.isEnabled("DecentHolograms");
        // HD is intentionally unsupported in this build (T7 fallback)
        switch (pref) {
            case "auto":
                if (dh) return loadDh();
                log.warning("[FTopForge] holograms enabled pero DecentHolograms no esta presente; modulo deshabilitado.");
                return null;
            case "decentholograms":
                if (dh) return loadDh();
                log.warning("[FTopForge] holograms.engine=decentholograms pero DH no esta enabled; modulo deshabilitado.");
                return null;
            case "holographicdisplays":
                log.warning("[FTopForge] holograms.engine=holographicdisplays no soportado en este build (HD jar no incluido); modulo deshabilitado.");
                return null;
            default:
                log.warning("[FTopForge] holograms.engine valor desconocido: " + pref);
                return null;
        }
    }

    private static HologramEngine loadDh() {
        try {
            Class<?> cls = Class.forName("com.ttsstudio.ftopforge.holograms.DhEngine");
            return (HologramEngine) cls.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to load DhEngine", t);
        }
    }

    // --- Runtime side (smoke-only, not unit-tested) ---

    private final Plugin plugin;
    private final HologramEngine engine;
    private final Location location;
    private final List<String> template;
    private final int refreshIntervalSeconds;
    private final Supplier<List<HologramLineFormatter.FactionLike>> topSupplier;
    private final LongSupplier secondsUntilRecalcSupplier;
    private int taskId = -1;

    public HologramService(Plugin plugin, HologramEngine engine, Location location, List<String> template,
                           int refreshIntervalSeconds,
                           Supplier<List<HologramLineFormatter.FactionLike>> topSupplier,
                           LongSupplier secondsUntilRecalcSupplier) {
        this.plugin = plugin; this.engine = engine; this.location = location; this.template = template;
        this.refreshIntervalSeconds = Math.max(5, refreshIntervalSeconds);
        this.topSupplier = topSupplier; this.secondsUntilRecalcSupplier = secondsUntilRecalcSupplier;
    }

    public void start() {
        if (engine == null) return;
        engine.create(location, renderLines());
        long ticks = (long) refreshIntervalSeconds * 20L;
        taskId = Bukkit.getScheduler().runTaskTimer(plugin,
            new Runnable() { public void run() { engine.update(renderLines()); } }, ticks, ticks).getTaskId();
    }

    public void stop() {
        if (taskId != -1) { Bukkit.getScheduler().cancelTask(taskId); taskId = -1; }
        if (engine != null) engine.remove();
    }

    private List<String> renderLines() {
        List<HologramLineFormatter.FactionLike> top = topSupplier.get();
        long secs = secondsUntilRecalcSupplier.getAsLong();
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String t : template) out.add(HologramLineFormatter.format(t, top, secs));
        return out;
    }

    /** Helper: resolve world+xyz from config. Returns null + log warn if world missing. */
    public static Location resolveLocation(String worldName, double x, double y, double z, Logger log) {
        if (worldName == null) {
            log.warning("[FTopForge] holograms.location world is null; modulo deshabilitado.");
            return null;
        }
        World w = Bukkit.getWorld(worldName);
        if (w == null) {
            log.warning("[FTopForge] holograms.location world '" + worldName + "' no existe; modulo deshabilitado.");
            return null;
        }
        return new Location(w, x, y, z);
    }

    // --- Accessors added in 1.2.3 for /ftopforge hologram info/tp/refresh ---

    /** Returns a defensive copy of the current location, or null if the service has none. */
    public Location currentLocation() {
        return location == null ? null : location.clone();
    }

    /** True iff the scheduler task is active. */
    public boolean isRunning() {
        return taskId != -1;
    }

    /** Forces an immediate engine.update with current rendered lines. No-op when stopped. */
    public void forceRefresh() {
        if (engine == null || taskId == -1) return;
        engine.update(renderLines());
    }
}
