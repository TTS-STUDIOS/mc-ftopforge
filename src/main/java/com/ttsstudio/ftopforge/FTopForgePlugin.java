package com.ttsstudio.ftopforge;

import com.ttsstudio.ftopforge.commands.FTopCommand;
import com.ttsstudio.ftopforge.commands.FTopForgeCommand;
import com.ttsstudio.ftopforge.core.BalanceAggregator;
import com.ttsstudio.ftopforge.core.BlockValuator;
import com.ttsstudio.ftopforge.core.CalculationEngine;
import com.ttsstudio.ftopforge.core.ChunkValuator;
import com.ttsstudio.ftopforge.core.FactionSnapshot;
import com.ttsstudio.ftopforge.core.ItemPrices;
import com.ttsstudio.ftopforge.core.ItemValuator;
import com.ttsstudio.ftopforge.core.RecalcRunner;
import com.ttsstudio.ftopforge.core.SpawnerValuator;
import com.ttsstudio.ftopforge.core.TopCache;
import com.ttsstudio.ftopforge.discord.DiscordEventBus;
import com.ttsstudio.ftopforge.discord.DiscordWebhook;
import com.ttsstudio.ftopforge.discord.EmbedBuilder;
import com.ttsstudio.ftopforge.discord.EventCooldownStore;
import com.ttsstudio.ftopforge.forensics.FactionsClaimResolver;
import com.ttsstudio.ftopforge.forensics.ForensicsBatchWriter;
import com.ttsstudio.ftopforge.forensics.ForensicsDao;
import com.ttsstudio.ftopforge.forensics.ForensicsListener;
import com.ttsstudio.ftopforge.gui.BreakdownGui;
import com.ttsstudio.ftopforge.gui.GuiManager;
import com.ttsstudio.ftopforge.gui.SkullCache;
import com.ttsstudio.ftopforge.gui.TopGui;
import com.ttsstudio.ftopforge.history.CsvExporter;
import com.ttsstudio.ftopforge.history.HistoryDao;
import com.ttsstudio.ftopforge.history.HistoryService;
import com.ttsstudio.ftopforge.holograms.HologramEngine;
import com.ttsstudio.ftopforge.holograms.HologramLineFormatter;
import com.ttsstudio.ftopforge.holograms.HologramService;
import com.ttsstudio.ftopforge.hooks.FactionsUUIDHook;
import com.ttsstudio.ftopforge.hooks.VaultHook;
import com.ttsstudio.ftopforge.i18n.Messages;
import com.ttsstudio.ftopforge.i18n.MessagesLoader;
import com.ttsstudio.ftopforge.meta.BStatsHook;
import com.ttsstudio.ftopforge.meta.DynamicBanner;
import com.ttsstudio.ftopforge.meta.MetaDao;
import com.ttsstudio.ftopforge.meta.RuntimeState;
import com.ttsstudio.ftopforge.papi.PlaceholderResolver;
import com.ttsstudio.ftopforge.rewards.CronScheduler;
import com.ttsstudio.ftopforge.rewards.PayoutRunner;
import com.ttsstudio.ftopforge.rewards.SeasonService;
import com.ttsstudio.ftopforge.rewards.SeasonsDao;
import com.ttsstudio.ftopforge.storage.JdbcRepository;
import com.ttsstudio.ftopforge.storage.SkullCacheDao;
import com.ttsstudio.ftopforge.util.Banner;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;

public class FTopForgePlugin extends JavaPlugin {

    private Messages messages;
    private FactionsUUIDHook factionsHook;
    private VaultHook vaultHook;
    private JdbcRepository repo;
    private TopCache topCache;
    private RecalcRunner recalcRunner;
    private SkullCache skullCache;
    private GuiManager guiManager;
    private int recalcTaskId = -1;

    // Bundle B/C/D state
    private CalculationEngine engine;
    private ItemPrices prices;
    private MetaDao metaDao;
    private HistoryDao historyDao;
    private CsvExporter csvExporter;
    private ForensicsDao forensicsDao;
    private ForensicsBatchWriter forensicsBatchWriter;
    private int forensicsFlushTaskId = -1;
    private SeasonsDao seasonsDao;
    private SeasonService seasonService;
    private PayoutRunner payoutRunner;
    private int payoutTaskId = -1;
    private int payoutDayOfWeek;
    private int payoutHour;
    private int payoutMinute;
    private TimeZone payoutTz;
    private HologramService hologramService;
    private DiscordEventBus discordBus;
    private final RuntimeState runtimeState = new RuntimeState();
    private volatile long lastRecalcDurationMs = 0L;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("items.yml", false);
        saveResource("messages-es.yml", false);
        saveResource("messages-en.yml", false);

        FileConfiguration cfg = getConfig();
        String lang = cfg.getString("language", "en");
        String storageType = cfg.getString("storage.type", "sqlite");

        this.messages = MessagesLoader.load(this, lang);

        try {
            this.factionsHook = FactionsUUIDHook.initOrFail();
            this.vaultHook = VaultHook.initOrFail();
        } catch (IllegalStateException e) {
            getLogger().severe("Hard-dep init failed: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        try {
            this.repo = JdbcRepository.init(this);
            bootstrapBundleBcdTables();
        } catch (SQLException e) {
            getLogger().severe("Storage init failed: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.prices = ItemPrices.loadFromYaml(this);
        BlockValuator bv = new BlockValuator(prices, cfg.getConfigurationSection("Blocks"));
        SpawnerValuator sv = new SpawnerValuator(prices);
        ItemValuator iv = new ItemValuator(prices);
        ChunkValuator cv = new ChunkValuator(bv, sv, iv,
                cfg.getBoolean("options.include-block-value-in-total-worth", true),
                cfg.getBoolean("options.include-spawner-value-in-total-worth", true),
                cfg.getBoolean("options.include-item-worth-in-total-worth", true));

        BalanceAggregator ba = new BalanceAggregator(vaultHook,
                cfg.getBoolean("options.include-player-balance-in-total-worth", true),
                cfg.getBoolean("options.include-offline-balances", false));

        this.engine = new CalculationEngine(this, factionsHook, cv, ba,
                cfg.getInt("options.chunks-calculated-per-second", 20),
                cfg.getInt("options.player-balances-calculated-per-second", 20));

        long delaySeconds = cfg.getLong("options.ftop-recalculation-delay-seconds", 1800L);

        this.topCache = new TopCache();
        this.recalcRunner = new RecalcRunner(this, engine, repo, topCache,
                cfg.getBoolean("options.broadcast-when-starting-calculation", true),
                cfg.getBoolean("options.broadcast-when-stopping-calculation", true),
                messages.get("calculation.starting"),
                messages.get("calculation.finished"),
                delaySeconds);

        recalcRunner.recoverStaleOnEnable();
        int restored = recalcRunner.restoreOnEnable();
        if (restored > 0) getLogger().info("Ranking restored from the last recalculation: " + restored + " factions.");

        // --- Bundle A (GUI) wiring ---
        runtimeState.guiEnabled = cfg.getBoolean("gui.enabled", true);
        if (runtimeState.guiEnabled) {
            try {
                SkullCacheDao skullDao = new SkullCacheDao(repo);
                skullDao.ensureSchema();
                long ttlMs = cfg.getLong("gui.skull-cache.ttl-hours", 24L) * 3600_000L;
                int warmup = cfg.getInt("gui.skull-cache.warmup-size", 200);
                int concurrency = cfg.getInt("gui.skull-cache.async-fetch-concurrency", 4);
                this.skullCache = new SkullCache(this, skullDao, ttlMs, warmup, concurrency);
                skullCache.warmupSync();
                int purged = skullCache.purgeExpired();
                if (purged > 0) getLogger().info("SkullCache: purged " + purged + " expired rows.");

                TopGui topGui = new TopGui(topCache, skullCache, messages, recalcRunner, cfg.getConfigurationSection("gui"));
                BreakdownGui breakdownGui = new BreakdownGui(topCache, skullCache, messages, cfg.getConfigurationSection("gui"));
                this.guiManager = new GuiManager(this, topGui, breakdownGui, topCache);
                guiManager.register();
                getLogger().info("Bundle A GUI ready (skull-cache TTL " + cfg.getLong("gui.skull-cache.ttl-hours", 24L) + "h, warmup " + warmup + ").");
            } catch (Throwable t) {
                getLogger().log(Level.SEVERE, "Bundle A GUI init failed: " + t.getMessage(), t);
                this.skullCache = null;
                this.guiManager = null;
                runtimeState.guiEnabled = false;
            }
        } else {
            getLogger().info("gui.enabled=false — Bundle A skipped.");
        }
        // --- end Bundle A ---

        // --- History wiring ---
        if (cfg.getBoolean("history.enabled", true)) {
            try {
                this.historyDao = new HistoryDao(repo.getDataSource());
                int retentionDays = cfg.getInt("history.retention-days", 90);
                boolean cleanup = cfg.getBoolean("history.cleanup-on-recalc", true);
                HistoryService historyService = new HistoryService(this, historyDao, retentionDays, cleanup);
                recalcRunner.onFinish(historyService::onRecalcFinished);
                File exportsDir = new File(getDataFolder(), "exports");
                this.csvExporter = new CsvExporter(repo.getDataSource(), exportsDir);
                runtimeState.historyEnabled = true;
                getLogger().info("History module ready (retention " + retentionDays + "d, cleanup-on-recalc=" + cleanup + ").");
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "History module init failed: " + t.getMessage(), t);
            }
        }

        // --- Forensics wiring ---
        if (cfg.getBoolean("forensics.enabled", true)) {
            try {
                this.forensicsDao = new ForensicsDao(repo.getDataSource());
                List<String> whitelistRaw = cfg.getStringList("forensics.whitelist-blocks");
                java.util.Set<String> whitelist = new java.util.HashSet<>();
                for (String s : whitelistRaw) whitelist.add(s.toUpperCase());
                boolean nonMembersOnly = cfg.getBoolean("forensics.log-non-members-only", false);
                int batchSize = cfg.getInt("forensics.flush-batch-size", 100);
                int maxRows = cfg.getInt("forensics.max-rows-per-chunk", 10000);
                int flushSecs = cfg.getInt("forensics.flush-interval-seconds", 5);
                FactionsClaimResolver claimResolver = new FactionsClaimResolver(factionsHook);
                this.forensicsBatchWriter = new ForensicsBatchWriter(
                    forensicsDao, batchSize, maxRows, getLogger());
                ForensicsListener listener = new ForensicsListener(
                    whitelist, nonMembersOnly, claimResolver, factionsHook, forensicsBatchWriter);
                Bukkit.getPluginManager().registerEvents(listener, this);
                long flushTicks = Math.max(20L, (long) flushSecs * 20L);
                this.forensicsFlushTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(
                    this, forensicsBatchWriter::tryFlush, flushTicks, flushTicks).getTaskId();
                // Retention cleanup: piggyback on recalc finish (cheap, periodic).
                int retentionDays = cfg.getInt("forensics.retention-days", 30);
                recalcRunner.onFinish((id, finishedAt, top) -> {
                    long cutoff = finishedAt - (long) retentionDays * 86_400_000L;
                    try {
                        int deleted = forensicsDao.deleteOlderThan(cutoff);
                        if (deleted > 0) getLogger().fine("[forensics] retention: deleted " + deleted + " old rows");
                    } catch (SQLException e) {
                        getLogger().log(Level.WARNING, "[forensics] retention delete failed", e);
                    }
                });
                runtimeState.forensicsEnabled = true;
                getLogger().info("Forensics module ready (whitelist=" + whitelist.size() + " materials, flush " + flushSecs + "s).");
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Forensics module init failed: " + t.getMessage(), t);
            }
        }

        // --- Meta KV (shared by Rewards + Discord cooldowns) ---
        this.metaDao = new MetaDao(repo.getDataSource());

        // --- Rewards wiring ---
        if (cfg.getBoolean("rewards.enabled", true)) {
            try {
                this.seasonsDao = new SeasonsDao(repo.getDataSource());
                this.seasonService = new SeasonService(metaDao, seasonsDao);
                Map<Integer, List<String>> rankCommands = new LinkedHashMap<>();
                ConfigurationSection ranks = cfg.getConfigurationSection("rewards.ranks");
                if (ranks != null) {
                    for (String k : ranks.getKeys(false)) {
                        try {
                            int rank = Integer.parseInt(k);
                            List<String> cmds = cfg.getStringList("rewards.ranks." + k + ".commands");
                            if (cmds != null && !cmds.isEmpty()) rankCommands.put(rank, cmds);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                boolean broadcast = cfg.getBoolean("rewards.broadcast", true);
                String broadcastTpl = messages.get("rewards.payout-broadcast");
                final CalculationEngine eng = this.engine;
                Runnable freezer = cfg.getBoolean("rewards.freeze-during-payout", true)
                    ? () -> eng.setFrozen(true) : () -> {};
                // The commands are queued on the next tick: unfreeze behind them, not before.
                Runnable unfreezer = cfg.getBoolean("rewards.freeze-during-payout", true)
                    ? () -> Bukkit.getScheduler().runTask(this, () -> eng.setFrozen(false)) : () -> {};
                PayoutRunner.CommandDispatcher dispatcher =
                    cmd -> Bukkit.getScheduler().runTask(this,
                        () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
                PayoutRunner.Broadcaster broadcaster = Bukkit::broadcastMessage;
                this.payoutRunner = new PayoutRunner(seasonService, rankCommands,
                    broadcast, broadcastTpl, dispatcher, broadcaster, freezer, unfreezer, getLogger());

                String dowName = cfg.getString("rewards.schedule.day-of-week", "MONDAY");
                this.payoutDayOfWeek = CronScheduler.parseDayOfWeek(dowName);
                this.payoutHour = cfg.getInt("rewards.schedule.hour", 0);
                this.payoutMinute = cfg.getInt("rewards.schedule.minute", 0);
                this.payoutTz = CronScheduler.resolveZone(cfg.getString("rewards.schedule.timezone", "system"));

                long now = System.currentTimeMillis();
                if (seasonService.shouldCatchUp(now)) {
                    getLogger().warning(messages.get("rewards.catch-up-warn",
                        "last", String.valueOf(metaDao.getLong("last_payout_at").orElse(0L)),
                        "missed", "1+"));
                    if (!payoutRunner.run(new ArrayList<>(topCache.top(10)), now)) {
                        // No ranking to pay from yet: pay as soon as the first recalculation ends.
                        final java.util.concurrent.atomic.AtomicBoolean owed =
                            new java.util.concurrent.atomic.AtomicBoolean(true);
                        recalcRunner.onFinish((id, finishedAt, top) -> {
                            if (!owed.compareAndSet(true, false)) return;
                            payoutRunner.run(new ArrayList<>(topCache.top(10)), System.currentTimeMillis());
                        });
                    }
                }
                scheduleNextPayout(now);
                runtimeState.rewardsEnabled = true;
                getLogger().info("Rewards module ready (" + dowName + " " + payoutHour + ":" + String.format(java.util.Locale.ROOT, "%02d", payoutMinute) + " " + payoutTz.getID() + ", ranks=" + rankCommands.size() + ").");
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Rewards module init failed: " + t.getMessage(), t);
            }
        }

        // --- Holograms wiring ---
        bootstrapHolograms();

        // --- Discord wiring ---
        if (cfg.getBoolean("discord.enabled", false)) {
            String webhookUrl = cfg.getString("discord.webhook-url", "");
            if (webhookUrl != null && !webhookUrl.trim().isEmpty()) {
                try {
                    DiscordWebhook poster = new DiscordWebhook(getLogger());
                    EventCooldownStore cooldownStore = new EventCooldownStore(metaDao);
                    Map<DiscordEventBus.EventType, Boolean> enabledMap = new HashMap<>();
                    enabledMap.put(DiscordEventBus.EventType.ON_RECALC_FINISHED, cfg.getBoolean("discord.events.on-recalc-finished", false));
                    enabledMap.put(DiscordEventBus.EventType.ON_TOP1_CHANGED, cfg.getBoolean("discord.events.on-top1-changed", true));
                    enabledMap.put(DiscordEventBus.EventType.ON_TOP10_SHUFFLE, cfg.getBoolean("discord.events.on-top10-shuffle", false));
                    enabledMap.put(DiscordEventBus.EventType.ON_WEEKLY_RESET, cfg.getBoolean("discord.events.on-weekly-reset", true));
                    Map<DiscordEventBus.EventType, Integer> cooldownMap = new HashMap<>();
                    cooldownMap.put(DiscordEventBus.EventType.ON_RECALC_FINISHED, cfg.getInt("discord.cooldown-hours.on-recalc-finished", 0));
                    cooldownMap.put(DiscordEventBus.EventType.ON_TOP1_CHANGED, cfg.getInt("discord.cooldown-hours.on-top1-changed", 6));
                    cooldownMap.put(DiscordEventBus.EventType.ON_TOP10_SHUFFLE, cfg.getInt("discord.cooldown-hours.on-top10-shuffle", 6));
                    cooldownMap.put(DiscordEventBus.EventType.ON_WEEKLY_RESET, cfg.getInt("discord.cooldown-hours.on-weekly-reset", 0));
                    String color = cfg.getString("discord.embed.color", "#FFD700");
                    String thumb = cfg.getString("discord.embed.thumbnail-url", "");
                    String footer = cfg.getString("discord.embed.footer", "FTopForge");
                    String roleId = cfg.getString("discord.mention-role-id-on-king-change", "");
                    this.discordBus = new DiscordEventBus(poster, cooldownStore, webhookUrl,
                        enabledMap, cooldownMap, color, thumb, footer, roleId, getLogger());

                    final TopCache tc = this.topCache;
                    recalcRunner.onFinish((id, finishedAt, top) -> {
                        // prev-worth lookup against current top (delta=0 first time since no prior snapshot in memory).
                        // For now we pass hasPrev=false; richer history-aware delta is a follow-up.
                        List<EmbedBuilder.TopRow> rows = new ArrayList<>();
                        List<FactionSnapshot> currentTop10 = tc.top(10);
                        for (int i = 0; i < currentTop10.size(); i++) {
                            FactionSnapshot s = currentTop10.get(i);
                            rows.add(new EmbedBuilder.TopRow(i + 1, s.factionName(), s.totalValue(), 0L, false));
                        }
                        String currTop1 = currentTop10.isEmpty() ? null : currentTop10.get(0).factionId();
                        List<String> currTop10Ids = new ArrayList<>();
                        for (FactionSnapshot s : currentTop10) currTop10Ids.add(s.factionId());
                        // Dispatch on async thread to avoid blocking server tick on HTTP.
                        final String prevT1 = tc.previousTop1Id();
                        final List<String> prevT10 = tc.previousTop10Ids();
                        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                            try {
                                discordBus.onRecalcFinished(rows, prevT1, currTop1, prevT10, currTop10Ids, System.currentTimeMillis());
                            } catch (Throwable t) {
                                getLogger().log(Level.WARNING, "[discord] dispatch failed", t);
                            }
                        });
                    });
                    runtimeState.discordEnabled = true;
                    getLogger().info("Discord module ready.");
                } catch (Throwable t) {
                    getLogger().log(Level.WARNING, "Discord module init failed: " + t.getMessage(), t);
                }
            } else {
                getLogger().info("Discord enabled but webhook-url empty — module skipped.");
            }
        }

        // --- PAPI wiring (lazy load to avoid hard dep) ---
        if (cfg.getBoolean("papi.enabled", true) && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                final TopCache tc = this.topCache;
                final RecalcRunner rr = this.recalcRunner;
                final FactionsUUIDHook fh = this.factionsHook;
                java.util.function.Supplier<List<? extends PlaceholderResolver.FactionLike>> topSupplier = () -> {
                    List<FactionSnapshot> top = tc.top(10);
                    List<PlaceholderResolver.FactionLike> out = new ArrayList<>(top.size());
                    for (FactionSnapshot s : top) {
                        out.add(new PlaceholderResolver.FactionLike() {
                            @Override public String factionId() { return s.factionId(); }
                            @Override public String factionName() { return s.factionName(); }
                            @Override public long totalValue() { return s.totalValue(); }
                            @Override public String leaderName() { return s.leaderName(); }
                        });
                    }
                    return out;
                };
                java.util.function.LongSupplier nextRecalcSecs = () -> {
                    long next = rr.nextScheduledAt();
                    if (next <= 0) return -1L;
                    return Math.max(0L, (next - System.currentTimeMillis()) / 1000L);
                };
                java.util.function.LongSupplier lastRecalcAgo = () -> {
                    long last = rr.lastFinishedAt();
                    if (last <= 0) return -1L;
                    return Math.max(0L, (System.currentTimeMillis() - last) / 1000L);
                };
                java.util.function.Function<UUID, String> playerFactionResolver = fh::factionIdOf;
                PlaceholderResolver resolver = new PlaceholderResolver(
                    topSupplier, nextRecalcSecs, lastRecalcAgo, playerFactionResolver);
                // Lazy-instantiate the expansion via reflection so the class doesn't need to load
                // unless PAPI is actually present.
                Class<?> cls = Class.forName("com.ttsstudio.ftopforge.papi.FtopForgePlaceholders");
                Object expansion = cls.getConstructor(PlaceholderResolver.class, String.class)
                    .newInstance(resolver, getDescription().getVersion());
                cls.getMethod("register").invoke(expansion);
                runtimeState.papiRegistered = true;
                getLogger().info("PAPI expansion registered (identifier=ftopforge).");
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "PAPI module init failed: " + t.getMessage(), t);
            }
        }

        // --- bStats wiring ---
        if (cfg.getBoolean("bstats.enabled", true)) {
            try {
                new BStatsHook().enable(this,
                    () -> topCache.size(),
                    () -> (int) Math.min(Integer.MAX_VALUE, lastRecalcDurationMs),
                    runtimeState);
                runtimeState.bstatsEnabled = true;
                getLogger().info("bStats hook enabled.");
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "bStats init failed: " + t.getMessage(), t);
            }
        }

        // --- Banner with dynamic modules ---
        Banner.print(this, storageType, DynamicBanner.modulesLine(runtimeState));

        String guiFlag = cfg.getString("gui.use-gui-on-ftop", "auto");
        getCommand("ftop").setExecutor(new FTopCommand(topCache, messages,
                cfg.getInt("options.factions-per-page", 10), guiManager, guiFlag));
        getCommand("ftopforge").setExecutor(new FTopForgeCommand(this, recalcRunner, messages));

        com.ttsstudio.ftopforge.commands.FTopForgeTabCompleter tabber =
            new com.ttsstudio.ftopforge.commands.FTopForgeTabCompleter(this);
        getCommand("ftop").setTabCompleter(tabber);
        getCommand("ftopforge").setTabCompleter(tabber);

        long ticks = Math.max(20L, delaySeconds * 20L);
        this.recalcTaskId = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override public void run() { recalcRunner.trigger(); }
        }, ticks, ticks).getTaskId();

        getLogger().info("FTopForge ready. First recalc in ~" + delaySeconds + "s. Use /ftopforge recalc to force.");
    }

    private void bootstrapBundleBcdTables() throws SQLException {
        try (Connection c = repo.connection(); Statement s = c.createStatement()) {
            s.executeUpdate(repo.dialect().historyDdl());
            for (String idx : repo.dialect().historyIndexesDdl()) {
                if (idx != null && !idx.isEmpty()) s.executeUpdate(idx);
            }
            s.executeUpdate(repo.dialect().forensicsDdl());
            for (String idx : repo.dialect().forensicsIndexesDdl()) {
                if (idx != null && !idx.isEmpty()) s.executeUpdate(idx);
            }
            s.executeUpdate(repo.dialect().seasonsDdl());
            for (String idx : repo.dialect().seasonsIndexesDdl()) {
                if (idx != null && !idx.isEmpty()) s.executeUpdate(idx);
            }
            s.executeUpdate(repo.dialect().metaDdl());
        }
    }

    private void scheduleNextPayout(long fromMs) {
        long nextMs = CronScheduler.nextRunAt(payoutDayOfWeek, payoutHour, payoutMinute, payoutTz, fromMs);
        long delayTicks = Math.max(20L, (nextMs - System.currentTimeMillis()) / 50L);
        if (payoutTaskId != -1) Bukkit.getScheduler().cancelTask(payoutTaskId);
        this.payoutTaskId = Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                if (payoutRunner != null) {
                    payoutRunner.run(new ArrayList<>(topCache.top(10)), System.currentTimeMillis());
                }
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "[payout] tick failed", t);
            }
            scheduleNextPayout(System.currentTimeMillis());
        }, delayTicks).getTaskId();
    }

    @Override
    public void onDisable() {
        if (recalcTaskId != -1) Bukkit.getScheduler().cancelTask(recalcTaskId);
        if (payoutTaskId != -1) Bukkit.getScheduler().cancelTask(payoutTaskId);
        if (forensicsFlushTaskId != -1) Bukkit.getScheduler().cancelTask(forensicsFlushTaskId);
        if (hologramService != null) hologramService.stop();
        if (forensicsBatchWriter != null) {
            try { forensicsBatchWriter.tryFlush(); } catch (Throwable ignored) {}
        }
        if (guiManager != null) {
            guiManager.closeAll();
            guiManager.unregister();
        }
        if (skullCache != null) skullCache.flush();
        if (repo != null) repo.shutdown();
        getLogger().info("FTopForge disabled");
    }

    public void reloadMessages() {
        String lang = getConfig().getString("language", "en");
        this.messages = MessagesLoader.load(this, lang);
    }

    // Getters used by FTopForgeCommand
    public Messages messages() { return messages; }
    public TopCache topCache() { return topCache; }
    public FactionsUUIDHook factionsHook() { return factionsHook; }
    public ItemPrices itemPrices() { return prices; }
    public HistoryDao historyDao() { return historyDao; }
    public CsvExporter csvExporter() { return csvExporter; }
    public ForensicsDao forensicsDao() { return forensicsDao; }
    public SeasonsDao seasonsDao() { return seasonsDao; }
    public PayoutRunner payoutRunner() { return payoutRunner; }
    public RuntimeState runtimeState() { return runtimeState; }
    public int payoutDayOfWeek() { return payoutDayOfWeek; }
    public int payoutHour() { return payoutHour; }
    public int payoutMinute() { return payoutMinute; }
    public TimeZone payoutTimeZone() { return payoutTz; }

    public HologramService holograms() { return hologramService; }

    /** Bootstrap the holograms module from current config. Idempotent: safe to call after shutdownHolograms. */
    public void bootstrapHolograms() {
        org.bukkit.configuration.file.FileConfiguration cfg = getConfig();
        if (!cfg.getBoolean("holograms.enabled", true)) {
            runtimeState.holoEngineName = null;
            return;
        }
        try {
            String engineCfg = cfg.getString("holograms.engine", "auto");
            HologramEngine hEngine = HologramService.detect(engineCfg,
                name -> Bukkit.getPluginManager().isPluginEnabled(name), this, getLogger());
            if (hEngine == null) {
                runtimeState.holoEngineName = null;
                return;
            }
            String wname = cfg.getString("holograms.location.world", "world");
            double hx = cfg.getDouble("holograms.location.x", 0d);
            double hy = cfg.getDouble("holograms.location.y", 80d);
            double hz = cfg.getDouble("holograms.location.z", 0d);
            Location loc = HologramService.resolveLocation(wname, hx, hy, hz, getLogger());
            if (loc == null) {
                runtimeState.holoEngineName = null;
                return;
            }
            List<String> template = cfg.getStringList("holograms.format");
            int refresh = cfg.getInt("holograms.refresh-interval-seconds", 60);
            final TopCache tc = this.topCache;
            final RecalcRunner rr = this.recalcRunner;
            java.util.function.Supplier<List<HologramLineFormatter.FactionLike>> topSupplier = () -> {
                List<FactionSnapshot> top = tc.top(10);
                List<HologramLineFormatter.FactionLike> out = new ArrayList<>(top.size());
                for (FactionSnapshot s : top) {
                    out.add(new HologramLineFormatter.FactionLike() {
                        @Override public String name() { return s.factionName(); }
                        @Override public long totalValue() { return s.totalValue(); }
                        @Override public String leaderName() { return s.leaderName(); }
                    });
                }
                return out;
            };
            java.util.function.LongSupplier nextRecalcSecs = () -> {
                long next = rr.nextScheduledAt();
                if (next <= 0) return -1L;
                return Math.max(0L, (next - System.currentTimeMillis()) / 1000L);
            };
            this.hologramService = new HologramService(this, hEngine, loc, template, refresh,
                topSupplier, nextRecalcSecs);
            hologramService.start();
            runtimeState.holoEngineName = "DH";
            getLogger().info("Holograms module ready (DH @ " + wname + " " + hx + "," + hy + "," + hz + ").");
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Holograms module init failed: " + t.getMessage(), t);
            this.hologramService = null;
            runtimeState.holoEngineName = null;
        }
    }

    /** Stop the hologram service and null the field. Idempotent. */
    public void shutdownHolograms() {
        if (hologramService != null) {
            try { hologramService.stop(); } catch (Throwable ignored) {}
            hologramService = null;
        }
        runtimeState.holoEngineName = null;
    }
}
