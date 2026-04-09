package com.hezaerd.memoryleak;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;

import java.util.ArrayList;
import java.util.List;

public final class LeakManager {
    public static final long BYTES_PER_MEGABYTE = 1024L * 1024L;
    private static final int BOSS_BAR_UPDATE_INTERVAL_TICKS = 10;
    private static final Identifier BOSS_BAR_ID = CommonClass.id("memory_leak");

    public static final LeakManager INSTANCE = new LeakManager();

    private final List<byte[]> leakedChunks = new ArrayList<>();

    private boolean leaking;
    private long targetBytes;
    private long allocatedBytes;
    private int totalTicks;
    private int elapsedTicks;
    private MinecraftServer server;
    private CustomBossEvent bossBar;

    private LeakManager() {
    }

    public synchronized void start(int megabytes, int seconds, MinecraftServer server) {
        if (leaking) {
            throw new IllegalStateException("A leak is already in progress. Stop it first.");
        }
        if (megabytes <= 0) {
            throw new IllegalArgumentException("Megabytes must be greater than 0.");
        }
        if (seconds <= 0) {
            throw new IllegalArgumentException("Seconds must be greater than 0.");
        }
        this.server = server;
        this.leaking = true;
        this.targetBytes = megabytes * BYTES_PER_MEGABYTE;
        this.allocatedBytes = 0L;
        this.totalTicks = seconds * 20;
        this.elapsedTicks = 0;
        createBossBar();
        updateBossBar();
    }

    public synchronized void tick(MinecraftServer server) {
        if (!leaking) {
            return;
        }
        if (this.server == null) {
            this.server = server;
        }
        elapsedTicks++;
        long expectedAllocatedBytes =
                Math.min(targetBytes, Math.round((targetBytes * (double) elapsedTicks) / totalTicks));
        long bytesToAllocate = expectedAllocatedBytes - allocatedBytes;
        allocate(bytesToAllocate);
        if (elapsedTicks % BOSS_BAR_UPDATE_INTERVAL_TICKS == 0 || allocatedBytes >= targetBytes) {
            updateBossBar();
        }
        if (elapsedTicks >= totalTicks || allocatedBytes >= targetBytes) {
            leaking = false;
            updateBossBar();
            removeBossBar();
        }
    }
    public synchronized void stop() {
        if (!leaking) {
            return;
        }
        leaking = false;
        totalTicks = 0;
        elapsedTicks = 0;
        targetBytes = allocatedBytes;
        removeBossBar();
    }
    public synchronized void clear() {
        stop();
        leakedChunks.clear();
        allocatedBytes = 0L;
        targetBytes = 0L;
    }
    public synchronized LeakStatus getStatus() {
        int remainingTicks = leaking ? Math.max(0, totalTicks - elapsedTicks) : 0;
        return new LeakStatus(leaking, allocatedBytes, targetBytes, remainingTicks);
    }
    private void allocate(long bytesToAllocate) {
        while (bytesToAllocate > 0) {
            int chunkSize = (int) Math.min(BYTES_PER_MEGABYTE, bytesToAllocate);
            leakedChunks.add(new byte[chunkSize]);
            allocatedBytes += chunkSize;
            bytesToAllocate -= chunkSize;
        }
    }
    private void createBossBar() {
        if (server == null) {
            return;
        }
        CustomBossEvents bossEvents = server.getCustomBossEvents();
        CustomBossEvent existing = bossEvents.get(BOSS_BAR_ID);
        if (existing != null) {
            existing.removeAllPlayers();
            bossEvents.remove(existing);
        }
        bossBar = bossEvents.create(RandomSource.create(), BOSS_BAR_ID, Component.literal("Memory Leak"));
        bossBar.setColor(BossEvent.BossBarColor.RED);
        bossBar.setOverlay(BossEvent.BossBarOverlay.NOTCHED_10);
        bossBar.setVisible(true);
        refreshBossBarPlayers();
    }
    private void updateBossBar() {
        if (bossBar == null) {
            return;
        }
        LeakStatus status = getStatus();
        int currentMb = status.allocatedMegabytes();
        int targetMb = Math.max(1, status.targetMegabytes());
        bossBar.setMax(targetMb);
        bossBar.setValue(Math.min(currentMb, targetMb));
        bossBar.setName(Component.literal(
                String.format("Memory Leak: %d/%d MB (%.1f%%)", currentMb, targetMb, status.progress() * 100.0)
        ));
        double progress = status.progress();
        if (progress < 0.33) {
            bossBar.setColor(BossEvent.BossBarColor.GREEN);
        } else if (progress < 0.66) {
            bossBar.setColor(BossEvent.BossBarColor.YELLOW);
        } else {
            bossBar.setColor(BossEvent.BossBarColor.RED);
        }
        refreshBossBarPlayers();
    }
    private void refreshBossBarPlayers() {
        if (bossBar == null || server == null) {
            return;
        }
        List<ServerPlayer> admins = server.getPlayerList()
                .getPlayers()
                .stream()
                .filter(player -> player.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .toList();
        bossBar.setPlayers(admins);
    }
    private void removeBossBar() {
        if (bossBar == null || server == null) {
            return;
        }
        CustomBossEvents bossEvents = server.getCustomBossEvents();
        bossBar.removeAllPlayers();
        bossEvents.remove(bossBar);
        bossBar = null;
    }
}
