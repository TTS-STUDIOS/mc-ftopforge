package com.ttsstudio.ftopforge.core;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BlockTallyTest {

    @Test
    void countsPricedBlocksAndSkipsEmptySections() {
        final AtomicInteger reads = new AtomicInteger();
        BlockTally.Blocks blocks = new BlockTally.Blocks() {
            public Material typeAt(int x, int y, int z) {
                reads.incrementAndGet();
                if (y == 5 && x < 3 && z == 0) return Material.DIAMOND_BLOCK;
                if (y == 6 && x == 0 && z == 0) return Material.OBSIDIAN;
                if (y == 7) return Material.STONE;
                return Material.AIR;
            }
            public boolean sectionEmpty(int i) { return i != 0; }
        };
        Map<Material, Integer> c = BlockTally.count(blocks, 0, 256,
            m -> m == Material.DIAMOND_BLOCK ? 2500 : m == Material.OBSIDIAN ? 15 : 0);
        assertEquals(Integer.valueOf(3), c.get(Material.DIAMOND_BLOCK));
        assertEquals(Integer.valueOf(1), c.get(Material.OBSIDIAN));
        assertFalse(c.containsKey(Material.STONE));
        assertEquals(16 * 16 * 16, reads.get());
    }

    @Test
    void bedrockFloorIsNotCountedEvenWithAPrice() {
        BlockTally.Blocks blocks = new BlockTally.Blocks() {
            public Material typeAt(int x, int y, int z) {
                if (y == 0) return Material.BEDROCK;
                return y == 1 && x == 0 && z == 0 ? Material.DIAMOND_BLOCK : Material.AIR;
            }
            public boolean sectionEmpty(int i) { return i != 0; }
        };
        Map<Material, Integer> c = BlockTally.count(blocks, 0, 256,
            m -> m == Material.BEDROCK ? 1500 : m == Material.DIAMOND_BLOCK ? 2500 : 0);
        assertFalse(c.containsKey(Material.BEDROCK));
        assertEquals(Integer.valueOf(1), c.get(Material.DIAMOND_BLOCK));
    }

    @Test
    void sectionIndexStartsAtTheWorldBottom() {
        final StringBuilder asked = new StringBuilder();
        BlockTally.Blocks blocks = new BlockTally.Blocks() {
            public Material typeAt(int x, int y, int z) { return y == -60 ? Material.GOLD_BLOCK : Material.AIR; }
            public boolean sectionEmpty(int i) { asked.append(i).append(','); return i != 0; }
        };
        Map<Material, Integer> c = BlockTally.count(blocks, -64, -16, m -> m == Material.GOLD_BLOCK ? 800 : 0);
        assertEquals(Integer.valueOf(256), c.get(Material.GOLD_BLOCK));
        assertEquals("0,1,2,", asked.toString());
    }
}
