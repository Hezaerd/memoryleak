package com.hezaerd.memoryleak.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeakManagerTest {
    private final LeakManager leak = new LeakManager();

    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) {
            leak.tick();
        }
    }

    @Test
    void leaksTheRequestedAmountOverTheRequestedTime() {
        leak.start(10, 2);
        tick(20);
        assertEquals(5, leak.status().allocatedMegabytes());
        assertTrue(leak.status().leaking());
        tick(20);
        assertEquals(10, leak.status().allocatedMegabytes());
        assertFalse(leak.status().leaking());
    }

    @Test
    void stopKeepsMemoryAndClearFreesIt() {
        leak.start(10, 1);
        tick(10);
        leak.stop();
        tick(10);
        assertEquals(5, leak.status().allocatedMegabytes());
        assertFalse(leak.status().leaking());
        leak.clear();
        assertEquals(0, leak.status().allocatedMegabytes());
    }

    @Test
    void secondLeakAddsToMemoryStillHeld() {
        leak.start(4, 1);
        tick(20);
        leak.start(6, 1);
        tick(10);
        assertEquals(0.5, leak.status().progress(), 0.001);
        tick(10);
        assertEquals(10, leak.status().allocatedMegabytes());
    }

    @Test
    void rejectsOutOfRangeAndOverlappingLeaks() {
        assertFalse(LeakCommands.start(leak, 0, 1).succeeded());
        assertFalse(LeakCommands.start(leak, 1, LeakManager.MAX_SECONDS + 1).succeeded());
        assertTrue(LeakCommands.start(leak, 1, 1).succeeded());
        assertFalse(LeakCommands.start(leak, 1, 1).succeeded());
    }

    @Test
    void parsesStringArguments() {
        assertTrue(LeakCommands.execute(leak, new String[]{"START", "3", "1"}).succeeded());
        tick(20);
        assertEquals("No active leak. Total leaked: 3 MB", LeakCommands.execute(leak, new String[]{"status"}).text());
        assertFalse(LeakCommands.execute(leak, new String[]{"start", "x", "1"}).succeeded());
        assertFalse(LeakCommands.execute(leak, new String[0]).succeeded());
        assertEquals(Arrays.asList("start", "stop", "status"), LeakCommands.complete(new String[]{"st"}));
    }
}
