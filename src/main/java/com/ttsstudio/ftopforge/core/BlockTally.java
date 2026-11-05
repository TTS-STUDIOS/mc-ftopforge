package com.ttsstudio.ftopforge.core;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.EnumMap;
import java.util.Map;

/** Counts every priced block in a chunk, tile entity or not. Bedrock never counts: it is the world floor. */
public final class BlockTally {

    public interface Blocks {
        Material typeAt(int x, int y, int z);
        boolean sectionEmpty(int sectionIndex);
    }

    public interface Prices { long valueOf(Material m); }

    private static final MethodHandle GET_TYPE = find(ChunkSnapshot.class, "getBlockType",
        MethodType.methodType(Material.class, int.class, int.class, int.class));
    private static final MethodHandle MIN_HEIGHT = find(World.class, "getMinHeight",
        MethodType.methodType(int.class));

    private BlockTally() {}

    /** Material -> count for every block with a price; empty sections are skipped. */
    public static Map<Material, Integer> count(Blocks blocks, int minY, int maxY, Prices prices) {
        Map<Material, Integer> out = new EnumMap<>(Material.class);
        Map<Material, Boolean> priced = new EnumMap<>(Material.class);
        for (int base = minY; base < maxY; base += 16) {
            if (blocks.sectionEmpty((base - minY) >> 4)) continue;
            int top = Math.min(maxY, base + 16);
            for (int y = base; y < top; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        Material m = blocks.typeAt(x, y, z);
                        if (m == null || m == Material.AIR || m == Material.BEDROCK) continue;
                        Boolean p = priced.get(m);
                        if (p == null) { p = prices.valueOf(m) > 0; priced.put(m, p); }
                        if (p) out.merge(m, 1, Integer::sum);
                    }
                }
            }
        }
        return out;
    }

    /** Reads the chunk through a snapshot; null if this server cannot. Main thread only. */
    public static Map<Material, Integer> count(Chunk chunk, Prices prices) {
        try {
            final ChunkSnapshot snap = chunk.getChunkSnapshot(false, false, false);
            World w = chunk.getWorld();
            int minY = MIN_HEIGHT != null ? (int) MIN_HEIGHT.invoke(w) : 0;
            int maxY = w.getMaxHeight();
            return count(new Blocks() {
                public Material typeAt(int x, int y, int z) { return type(snap, x, y, z); }
                public boolean sectionEmpty(int i) {
                    try { return snap.isSectionEmpty(i); } catch (Throwable t) { return false; }
                }
            }, minY, maxY, prices);
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private static Material type(ChunkSnapshot snap, int x, int y, int z) {
        if (GET_TYPE != null) {
            try { return (Material) GET_TYPE.invoke(snap, x, y, z); }
            catch (Throwable t) { throw new IllegalStateException(t); }
        }
        return Material.getMaterial(snap.getBlockTypeId(x, y, z));
    }

    private static MethodHandle find(Class<?> owner, String name, MethodType type) {
        try { return MethodHandles.publicLookup().findVirtual(owner, name, type); }
        catch (Throwable t) { return null; }
    }
}
