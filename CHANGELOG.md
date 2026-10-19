# Changelog

All notable changes to FTopForge are documented here.

## [1.2.7] - 2026-10-19

### Fixed
- **Weekly rewards can no longer be paid twice.** The payout is now saved before the reward commands run. If saving fails nothing is paid and it is retried (next restart or next scheduled time); before, the commands ran anyway and could run again after recovering.
- **The ranking stays frozen until the reward commands have actually run.** It was unfrozen one step before them.

## [1.2.5] - 2026-10-05 — Code review fixes

### Fixed
- **Weekly rewards were lost after a restart.** The ranking was empty until the first recalculation, so a catch-up or scheduled payout in that window paid nobody and still marked the week as paid. The ranking is now restored from the last recalculation at startup, an empty ranking is never paid or recorded, and a payout owed at startup runs when the first recalculation ends.
- **`/ftop` is no longer empty for half an hour after every restart,** and the first recalculation after a restart no longer announces a "new king" on Discord.
- **Items in double chests were counted twice.**
- **A recalculation asked for while rewards are being paid** no longer broadcasts that it started or leaves an unfinished row behind.
- **`/ftopforge forensics <faction> <page> insider`** lists members of the faction, and the `(insider)` tag marks them. Both were on outsiders.
- **Dragging items onto the top or breakdown menu** no longer loses them.
- **`/ftopforge hologram set` after `/ftopforge reload`** no longer writes back the configuration from before the reload.
- `999,950` is shown as `1.0M`, not `1000.0K`.

## [1.2.3] - 2026-05-21 — Hologram in-game commands + reload rebuild

### Added
- **`/ftopforge hologram` subcommand tree** (perm `ftopforge.admin`):
  - `set` — sets the hologram to where the player is standing. Mutates `config.yml > holograms.location` and respawns the live hologram (no restart).
  - `move <world> <x> <y> <z>` — explicit coordinates. Validates that the world is loaded; rejects non-numeric coords.
  - `info` — prints current engine, location, enabled state, refresh interval, template line count.
  - `tp` — teleports the player to the live hologram location.
  - `disable` / `enable` — flips `holograms.enabled` in `config.yml` and respawns / removes the live hologram.
  - `refresh` — forces an immediate `engine.update(renderLines())` without waiting for the timer.
- **Defensive backup:** first hologram mutate per server process copies `config.yml` → `data/config.yml.bak`. Subsequent mutates never overwrite that snapshot.
- **Tab completion** across `/ftopforge` and `/ftop`:
  - Top-level subcommands filtered by `ftopforge.admin`.
  - `hologram` subcommands.
  - `move <TAB>` lists loaded world names.
  - `rewards`/`history`/`forensics` second-arg suggestions.
- New messages keys under `hologram.*` in `messages-es.yml` + `messages-en.yml` (set-success / move-bad-world / info-line-* / etc.).
- New tests: `HologramServiceAccessorsTest`, `HologramConfigWriterTest`, `HologramSubcommandParsingTest`, `HologramTabCompletionTest`. Total **167 tests**, 0 failures.

### Changed
- **`/ftopforge reload`** now also calls `shutdownHolograms() + bootstrapHolograms()`. Changes to `holograms.{enabled,engine,format,refresh-interval-seconds,location.*}` take effect live without server restart.
- `FTopForgePlugin.onEnable` hologram bootstrap block extracted to `bootstrapHolograms()` + companion `shutdownHolograms()`. Same path is reused by the reload flow and by every hologram subcommand mutate.
- `HologramService` gained `currentLocation()`, `isRunning()`, `forceRefresh()` accessors. Existing fields remain `final` — no live mutators; the shutdown+bootstrap pattern is the single change vector.
- `plugin.yml` usage line + `FTopForgeCommand` default usage updated to include `hologram` in the menu.

### Notes
- Hologram model remains **singleton** (`ftopforge-top` DH name). Multi-instance (named holograms) is a candidate for v1.3.0; the new accessors and the shutdown+bootstrap pattern are the building blocks a multi refactor would reuse.
- Paper 1.18+ preserves YAML comments on `saveConfig()` (`parseComments=true` default). The shipped richer schema's es-language comments survive `set`/`move`/`disable`/`enable` mutates without extra wiring. The `.bak` is the recovery path if a server fork ever breaks this assumption.
- `set` from console: rejected with `hologram.set-no-player`. `tp` from console: rejected with `hologram.tp-no-player`. `move` works from both.

### Deploy
- Tiamat session 16. Backup `_migracion/backups/2026-05-21_FACTIONS_pre_session16/`. Deploy via `tiamat-deployer` subagent.

## [1.2.4] — 2026-09-21

### Fixed
- **Numbers no longer follow the server's system language.** `String.format` was used
  without a `Locale`, so the very same jar printed `$12.50` on an English machine and
  `$12,50` on a Spanish or French one, and the thousands separator flipped with it. Every
  player-facing number is written with `Locale.ROOT` now, and a test keeps it that way.

## [1.2.2] - 2026-05-20 — Config polish + items.yml key fix + bStats registered

### Fixed
- **Silent value miss en `items.yml`:** las keys de `Spawners` y `Blocks` usaban CamelCase (`IronGolem`, `DiamondBlock`) que tras `key.toUpperCase()` no matcheaban con `EntityType.name()` / `Material.name()` (que usan `UPPER_SNAKE_CASE`: `IRON_GOLEM`, `DIAMOND_BLOCK`). El plugin no validaba ni warneaba — devolvía `0` para esos lookups. Heredado del baseline cracked v5.x. Afectaba a IRON_GOLEM, MAGMA_CUBE, MUSHROOM_COW, PIG_ZOMBIE (1.8-1.15), WITHER_SKELETON, y todos los bloques compound de items.yml#Blocks (DIAMOND_BLOCK, EMERALD_BLOCK, etc.).
- Reescritas keys a UPPER_SNAKE_CASE en items.yml shipped + comentarios explicando la regla. Añadidos ZOMBIFIED_PIGLIN, WITHER_SKELETON, CAVE_SPIDER, GUARDIAN, ENDERMITE, LAPIS_BLOCK, BEACON.

### Changed
- **bStats `PLUGIN_ID`:** `0` placeholder (DEC-041) → **`31455`** (registrado en https://bstats.org/plugin/bukkit/FTopForge/31455).
- **`items.yml` baseline paid:** spawners endgame uplifted (IRON_GOLEM 1M→2.5M, SILVERFISH 1.5M→1.8M, +WITHER_SKELETON 1.5M, +GUARDIAN 600k), mob farms planas (COW 50k→25k, OCELOT 10k→15k), mid-tier mantenido. Bloques mid-tier subidos (DIAMOND_BLOCK 1700→2500, IRON_BLOCK 1200→1500, +LAPIS_BLOCK 300, +BEACON 50000).
- **`config.yml` rewards baseline paid:** rank 1 1M+64 diamond_block → **5M + 1 beacon + 64 golden_apple + 64 diamond_block**; rank 2 500k+32db → **2M + 32 golden_apple + 64 diamond_block**; rank 3 250k+16db → **750k + 16 golden_apple + 32 diamond_block**. Removido el `broadcast` inline del rank 1 (redundante con `rewards.broadcast: true` que usa el template `messages.yml#rewards.payout-broadcast`).
- **`config.yml` polish para downloaders:** sección-level + key-level comments en español, ejemplos de webhook URL Discord, ejemplos de timezone IANA (UTC, Europe/Madrid, America/Argentina/Buenos_Aires), nota explicativa del schema UPPER_SNAKE_CASE.

### Notes
- `BStatsHook.PLUGIN_ID` constante hardcoded → cualquier server con el jar 1.2.2 reportará a la página oficial. Fork/recompile requerido para cambiar destino.
- Live config FACTIONS sincronizado con shipped richer schema (gui.top.filler, gui.breakdown.frame+items, gui.skull-cache.{warmup-size,async-fetch-concurrency}, discord.cooldown-hours). El plugin ya leía estas keys con defaults — la sincronización solo expone los toggles a operadores.

### Deploy
- Sesión 14 (Tiamat regular). Backup `_migracion/backups/2026-05-20_FACTIONS_pre_session14/`.

## [1.2.1] - 2026-05-19 — Hotfix: Messages.resolve flat-first lookup

### Fixed
- **Critical:** `Messages.resolve` returned `<missing:KEY>` for every nested key in production. `MessagesLoader.loadOne` loads YAML via `YamlConfiguration.getValues(true)` which produces a flat map with dot-path keys + `ConfigurationSection` refs at branch nodes — NOT nested HashMaps as the resolver expected. Latent since Bundle A (v1.1.0); surfaced post-Custom 9 when the user opened `/ftop` with an empty top and saw `<missing:gui.top.empty>` in the action bar. All `gui.*`, `breakdown.*`, and the new Custom 9 nested keys were affected.
- Pre-existing unit tests passed because `MessagesTest` used a private `toNestedMap(yaml)` helper that converted to true nested HashMaps, masking the production code path.

### Added
- Regression test `resolvesNestedKey_whenLoadedViaYamlGetValuesTrue_productionPath` that exercises the exact production path (`yaml.getValues(true)`) and asserts `gui.top.empty` + `gui.top.title` resolve.
- 150 → 151 tests, 0 failures.

### Notes
- Pre-fix workaround: replaced `messages-es.yml` and `messages-en.yml` on production (they were stale Custom 7 baseline files preserved by `saveDefaultConfig` across Bundle A and Custom 9 deploys). Originals backed up.
- DEC-043 added documenting the flat-first lookup pattern.

## [1.2.0] - 2026-05-19 — Bundles B+C+D + Polish (Custom 9 / Sesión 12)

### Added
- **Holograms** (Bundle B): DH soft-dep autodetect (HD dropped — DEC-038), refresh scheduler, line formatter con 16 placeholders `%top1..N_*%` y `%next_recalc%`.
- **Discord webhook** (Bundle C): embed payload con JSON manual, 4 event types (recalc-finished, top1-changed, top10-shuffle, weekly-reset), cooldown configurable por event-type (default 6h shuffles, 0 weekly-reset). HttpURLConnection + Semaphore(2). Skipped en deploy (disabled in config).
- **PAPI placeholders** (Bundle C): 48 placeholders (top1..10 × {name, value, value_raw, leader} + 4 player + 4 meta) vía `PlaceholderResolver` pure-logic + `FtopForgePlaceholders` shim con lazy Class.forName load.
- **History** (Bundle D): tabla `ftf_history` (composite PK recalc_id+faction_id), `HistoryService` subscribed a `RecalcRunner.onFinish`, retención 90d con cleanup-on-recalc, ASCII chart 12×8 con footer Pico/Min/Δ, RFC-4180 CSV exporter streaming a `plugins/FTopForge/exports/history-YYYYMMDDHHmmss.csv`.
- **Forensics** (Bundle D): tabla `ftf_forensics`, BlockPlace/BlockBreak listeners (MONITOR priority), batch async writer (ConcurrentLinkedDeque + flush por size/interval + overflow protection), `FactionsClaimResolver` con LRU+TTL 30s cache (DEC-040). `ForensicsDao` con insertBatch / queryByFaction / deleteOlderThan.
- **Rewards + Season** (Bundle D): tablas `ftf_seasons` + `ftf_meta`, `CronScheduler` weekly con timezone IANA (5 tests), catch-up on startup si missed (DEC-039), `PayoutRunner` con freeze/unfreeze via `CalculationEngine.setFrozen` + `Bukkit.dispatchCommand` via `CommandDispatcher` interface. Manual `/ftopforge rewards run`. Soft season strategy (no worth reset, solo season marker).
- **`/ftopforge worth`** (Polish): item-in-hand o block-target lookup vía raytrace, MOB_SPAWNER detection via `BlockStateMeta`, formato chat con flag notListed.
- **Dynamic banner modules line** (Polish): placeholder runtime `${ftf.modules}` (DEC-042 — renombrado desde `${modules}` para evitar clash con built-in Maven `project.modules`). `RuntimeState` POJO populated durante `onEnable` wiring. Línea final: `modules: GUI | Holo(DH) | PAPI | History | Forensics | Rewards | bStats`.
- **bStats** (Polish): `org.bstats:bstats-bukkit:3.0.2` shadeado + relocado a `com.ttsstudio.ftopforge.libs.bstats`. `BStatsHook.enable` con `pluginId=0` placeholder (DEC-041) + 3 custom charts (faction_count, recalc_duration_ms, modules_enabled drilldown).
- **+71 tests** (79 baseline → **150 tests**). 0 failures.

### Changed
- `RecalcRunner` — añadido `FinishCallback` nested interface + `onFinish(cb)` registration + invocation hook en el lambda (additive, sin breaking changes).
- `TopCache` — añadidos `previousTop1Id()` y `previousTop10Ids()` accessors con snapshot capture al inicio de `rebuild()` antes del write-lock.
- `CalculationEngine` — añadido `setFrozen(boolean)` / `isFrozen()` + freeze check en `tryStart` (usado por `PayoutRunner` durante weekly payouts).
- `FactionsUUIDHook` — añadidos `factionIdAt(World, int, int)` y `factionIdOf(UUID)`.
- `Dialect` — extendido con DDLs para 4 tablas nuevas + index methods.
- `JdbcRepository` — añadido `getDataSource()` accessor (los DAOs de Bundle B/C/D necesitan DataSource, no single Connection).
- `Banner.print(plugin, storageType)` — añadido overload `print(plugin, storageType, modulesLine)`; 2-arg shim sigue funcionando.

### Fixed
- Banner modules line resolution: Maven sustituía `${modules}` (built-in `project.modules`) por `[]` durante resource filtering en single-module projects. Renombrado runtime placeholder a `${ftf.modules}` (DEC-042).

### Schema (additive)
- `ftf_history` (composite PK recalc_id+faction_id, índices en finished_at y faction_id)
- `ftf_forensics` (autoincrement id, 3 índices: faction, chunk, player)
- `ftf_seasons` (autoincrement season_id, índice en ended_at)
- `ftf_meta` (KV store con VARCHAR meta_key PK + meta_value)

### Deploy
- Backups en `_migracion/backups/2026-05-19_FACTIONS_pre_bundle_bcd/` (ftopforge-1.1.0.jar + ftopforge-1.2.0-RC1.jar + FTopForge config dir).
- Deploy via `tiamat-deployer`: RC1 (commit `fa57e7d`) → smoke → fix banner placeholder → 1.2.0 final (commit `b6d6fc5`) → redeploy → Done en 13.986s, 0 SEVERE.
- Jar: `ftopforge-1.2.0.jar` ≈ 12.81 MB (HikariCP + sqlite-jdbc + bStats shaded).

## 1.1.0 (2026-05-19) — Bundle A: GUI top + breakdown

### Added
- GUI top con paginación (6 filas, top 45/página, cabezas del líder).
- GUI breakdown por faction (3 filas, summary + 5 categorías + comparación vs media top 10).
- Skull cache persistido a SQLite (`ftf_skull_cache`, TTL 24h, warmup 200).
- `/ftop gui` y `/ftop chat` para forzar output.
- Auto-detect player→GUI / console→chat (`gui.use-gui-on-ftop: auto`).
- Config nuevo: `gui.top.*`, `gui.breakdown.*`, `gui.skull-cache.*`.
- i18n strings nuevos en `messages-{es,en}.yml` bajo `gui.*`.

### Internal
- `TopCache.topRange(int, int)` + `TopCache.averagesTop10()`.
- `RecalcRunner` getters `lastFinishedAt()` + `isRunning()` + `nextScheduledAt()`.
- Tests nuevos: SkullCacheDaoTest, SkullCacheTest, TopGuiLayoutTest, MaterialKeyTest, BreakdownMathTest, FTopCommandRoutingTest. Total ~79 tests.

## 1.0.0 (2026-05-19) — Custom 7 baseline
- Initial release replacing cracked FactionsTop v5.0.7.2 on FACTIONS.
- See spec `docs/superpowers/specs/2026-05-19-ftopforge-design.md` for full §4.1 baseline scope.
