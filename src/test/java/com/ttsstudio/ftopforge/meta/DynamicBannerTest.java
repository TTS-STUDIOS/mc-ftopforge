package com.ttsstudio.ftopforge.meta;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DynamicBannerTest {

    @Test
    void allEnabled_listsAllModulesInFixedOrder() {
        RuntimeState s = new RuntimeState();
        s.guiEnabled = true; s.holoEngineName = "DH"; s.discordEnabled = true; s.papiRegistered = true;
        s.historyEnabled = true; s.forensicsEnabled = true; s.rewardsEnabled = true; s.bstatsEnabled = true;
        assertEquals("modules: GUI | Holo(DH) | Discord | PAPI | History | Forensics | Rewards | bStats",
            DynamicBanner.modulesLine(s));
    }

    @Test
    void onlyGui_showsOnlyGui() {
        RuntimeState s = new RuntimeState();
        s.guiEnabled = true;
        assertEquals("modules: GUI", DynamicBanner.modulesLine(s));
    }

    @Test
    void holoEngineNull_omitsHoloEntry() {
        RuntimeState s = new RuntimeState();
        s.guiEnabled = true; s.holoEngineName = null; s.discordEnabled = true;
        assertEquals("modules: GUI | Discord", DynamicBanner.modulesLine(s));
    }

    @Test
    void preservesFixedOrder_evenWhenSubsetEnabled() {
        RuntimeState s = new RuntimeState();
        s.guiEnabled = true; s.forensicsEnabled = true; s.discordEnabled = true;
        String line = DynamicBanner.modulesLine(s);
        assertTrue(line.indexOf("GUI") < line.indexOf("Discord"));
        assertTrue(line.indexOf("Discord") < line.indexOf("Forensics"));
    }
}
