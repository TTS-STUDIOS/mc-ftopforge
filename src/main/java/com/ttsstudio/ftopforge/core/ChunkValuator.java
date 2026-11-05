package com.ttsstudio.ftopforge.core;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.util.Map;

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
     * Scan priced blocks over the whole chunk, then spawners and containers from its tile entities.
     * The chunk MUST be loaded by the caller before invoking this method.
     */
    public void scan(Chunk chunk, FactionSnapshot.Builder snapshot) {
        if (chunk == null || !chunk.isLoaded()) return;

        // Tile entities carry spawner and container value.
        BlockState[] tiles;
        try {
            tiles = chunk.getTileEntities();
        } catch (Throwable t) {
            return;
        }

        // Priced blocks are counted over the whole chunk: most of them (diamond blocks, obsidian...)
        // are not tile entities. The tile loop below only falls back to them if the snapshot fails.
        boolean blocksDone = false;
        if (includeBlocks) {
            Map<Material, Integer> counts = BlockTally.count(chunk, blockValuator::valueOf);
            if (counts != null) {
                blocksDone = true;
                for (Map.Entry<Material, Integer> e : counts.entrySet()) {
                    snapshot.addBlocksValue(blockValuator.valueOf(e.getKey()) * e.getValue());
                    snapshot.addBlockCount(e.getKey().name(), e.getValue());
                }
            }
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

            if (includeBlocks && !blocksDone) {
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
