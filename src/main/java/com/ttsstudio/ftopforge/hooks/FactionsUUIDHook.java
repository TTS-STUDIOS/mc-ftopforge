package com.ttsstudio.ftopforge.hooks;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class FactionsUUIDHook {

    public static FactionsUUIDHook initOrFail() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Factions")) {
            throw new IllegalStateException("FactionsUUID is not enabled. FTopForge requires Factions 1.6.x.");
        }
        try {
            Class.forName("com.massivecraft.factions.Factions");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("FactionsUUID API not found on classpath", e);
        }
        return new FactionsUUIDHook();
    }

    /** All factions excluding wilderness/safezone/warzone. */
    public Collection<Faction> allRealFactions() {
        List<Faction> out = new ArrayList<>();
        for (Faction f : Factions.getInstance().getAllFactions()) {
            if (f.isWilderness() || f.isSafeZone() || f.isWarZone()) continue;
            out.add(f);
        }
        return out;
    }

    public Collection<FLocation> claimsOf(Faction f) {
        return f.getAllClaims();
    }

    public String leaderName(Faction f) {
        if (f.getFPlayerAdmin() == null) return "(no leader)";
        return f.getFPlayerAdmin().getName();
    }

    public UUID leaderUuid(Faction f) {
        if (f.getFPlayerAdmin() == null) return null;
        try {
            return UUID.fromString(f.getFPlayerAdmin().getId());
        } catch (Exception e) {
            return null;
        }
    }

    public Collection<UUID> memberUuids(Faction f) {
        List<UUID> out = new ArrayList<>();
        f.getFPlayers().forEach(fp -> {
            try { out.add(UUID.fromString(fp.getId())); } catch (Exception ignored) {}
        });
        return out;
    }

    /** Returns the faction id at (world, chunkX, chunkZ), or null for wilderness/missing. */
    public String factionIdAt(World world, int chunkX, int chunkZ) {
        if (world == null) return null;
        try {
            FLocation floc = new FLocation(world.getName(), chunkX, chunkZ);
            Faction f = Board.getInstance().getFactionAt(floc);
            if (f == null || f.isWilderness()) return null;
            return f.getId();
        } catch (Throwable t) {
            return null;
        }
    }

    /** Returns the faction id of the given player UUID, or null if none/wilderness. */
    public String factionIdOf(UUID playerUuid) {
        if (playerUuid == null) return null;
        try {
            FPlayer fp = FPlayers.getInstance().getById(playerUuid.toString());
            if (fp == null) return null;
            Faction f = fp.getFaction();
            if (f == null || f.isWilderness()) return null;
            return f.getId();
        } catch (Throwable t) {
            return null;
        }
    }
}
