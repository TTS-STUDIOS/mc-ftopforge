package com.ttsstudio.ftopforge.forensics;

import com.ttsstudio.ftopforge.hooks.FactionsUUIDHook;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Set;

/**
 * Bukkit listener that observes whitelisted block place/break events in
 * claimed chunks and enqueues them on the {@link ForensicsBatchWriter}.
 *
 * <p>The decision logic lives in {@link ForensicsListenerFilter} so it can be
 * unit-tested without a Bukkit runtime — this class is just the plumbing that
 * pulls inputs out of the events.
 */
public final class ForensicsListener implements Listener {

    private final Set<String> whitelist;
    private final boolean nonMembersOnly;
    private final FactionsClaimResolver claimResolver;
    private final FactionsUUIDHook factionsHook;
    private final ForensicsBatchWriter batchWriter;

    public ForensicsListener(Set<String> whitelist, boolean nonMembersOnly,
                             FactionsClaimResolver claimResolver, FactionsUUIDHook factionsHook,
                             ForensicsBatchWriter batchWriter) {
        this.whitelist = whitelist;
        this.nonMembersOnly = nonMembersOnly;
        this.claimResolver = claimResolver;
        this.factionsHook = factionsHook;
        this.batchWriter = batchWriter;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        handle(e.getPlayer(), e.getBlock(), ForensicsEvent.Action.PLACE);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        handle(e.getPlayer(), e.getBlock(), ForensicsEvent.Action.BREAK);
    }

    private void handle(Player p, Block b, ForensicsEvent.Action action) {
        String material = b.getType().name();
        int cx = b.getChunk().getX();
        int cz = b.getChunk().getZ();
        String territoryFac = claimResolver.factionAt(b.getWorld(), cx, cz);
        String playerFac = factionsHook.factionIdOf(p.getUniqueId());
        ForensicsListenerFilter.Result r = ForensicsListenerFilter.decide(
            material, playerFac, territoryFac, whitelist, nonMembersOnly);
        if (!r.shouldRecord) return;
        ForensicsEvent ev = new ForensicsEvent(
            System.currentTimeMillis(), p.getUniqueId(), p.getName(), action, material,
            b.getWorld().getName(), b.getX(), b.getY(), b.getZ(), cx, cz,
            territoryFac, r.isMember);
        batchWriter.enqueue(ev);
    }
}
