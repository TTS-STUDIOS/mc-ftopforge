package com.ttsstudio.ftopforge.core;

import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

public class ChunkValuator {

    private final BlockValuator blockValuator;
    private final SpawnerValuator spawnerValuator;
    private final ItemValuator itemValuator;
    private final boolean includeBlocks, includeSpawners, includeItems;

    public ChunkValuator(BlockValuator b, SpawnerValuator s, ItemValuator i,
                         boolean includeBlocks, boolean includeSpawners, boolean includeItems) {
        this.blockValuator = b;
        this.spawnerValuator = s;
        this.itemValuator = i;
        this.includeBlocks = includeBlocks;
        this.includeSpawners = includeSpawners;
        this.includeItems = includeItems;
    }

    /**
     * Scan all tile entities + value-carrying blocks in this chunk.
     * Tile entities (chests, spawners, hoppers, beacons) are quick to enumerate via Chunk.getTileEntities().
     * The chunk MUST be loaded by the caller before invoking this method.
     */
    public void scan(Chunk chunk, FactionSnapshot.Builder snapshot) {
        if (chunk == null || !chunk.isLoaded()) return;

        // tile entities only — these are the value-bearing blocks (spawners, chests, hoppers, beacons).
        // Iterating raw blocks (16x256x16 = 65536) per chunk would be too slow for big maps.
        BlockState[] tiles;
        try {
            tiles = chunk.getTileEntities();
        } catch (Throwable t) {
            return;
        }

        for (BlockState st : tiles) {
            if (st == null) continue;
            Block b = st.getBlock();

            if (includeSpawners) {
                long sp = spawnerValuator.valueOf(b);
                if (sp > 0) {
                    snapshot.addSpawnersValue(sp);
                    String mob = spawnerValuator.mobOf(b);
                    if (mob != null) snapshot.incSpawnerCount(mob);
                }
            }

            if (includeBlocks) {
                long bv = blockValuator.valueOf(b);
                if (bv > 0) {
                    snapshot.addBlocksValue(bv);
                    snapshot.incBlockCount(b.getType().name());
                }
            }

            if (includeItems) {
                long iv = itemValuator.valueOfContents(b);
                if (iv > 0) {
                    snapshot.addItemsValue(iv);
                }
            }
        }
    }
}
