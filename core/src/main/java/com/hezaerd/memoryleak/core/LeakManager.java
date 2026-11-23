package com.hezaerd.memoryleak.core;

import java.util.ArrayList;
import java.util.List;

public final class LeakManager {
    public static final long BYTES_PER_MEGABYTE = 1024L * 1024L;
    public static final int TICKS_PER_SECOND = 20;
    public static final int MAX_MEGABYTES = 10_000;
    public static final int MAX_SECONDS = 3_600;

    public static final LeakManager INSTANCE = new LeakManager();

    private final List<byte[]> leakedChunks = new ArrayList<>();

    private boolean leaking;
    private long startBytes;
    private long targetBytes;
    private long allocatedBytes;
    private int totalTicks;
    private int elapsedTicks;

    LeakManager() {
    }

    public synchronized void start(int megabytes, int seconds) {
        if (leaking) {
            throw new IllegalStateException("A leak is already in progress. Stop it first.");
        }
        if (megabytes < 1 || megabytes > MAX_MEGABYTES) {
            throw new IllegalArgumentException("Megabytes must be between 1 and " + MAX_MEGABYTES + ".");
        }
        if (seconds < 1 || seconds > MAX_SECONDS) {
            throw new IllegalArgumentException("Seconds must be between 1 and " + MAX_SECONDS + ".");
        }
        leaking = true;
        startBytes = allocatedBytes;
        targetBytes = allocatedBytes + megabytes * BYTES_PER_MEGABYTE;
        totalTicks = seconds * TICKS_PER_SECOND;
        elapsedTicks = 0;
    }

    public synchronized LeakStatus tick() {
        if (leaking) {
            elapsedTicks++;
            long leakBytes = targetBytes - startBytes;
            long expectedBytes = startBytes + Math.round(leakBytes * (double) elapsedTicks / totalTicks);
            allocate(Math.min(targetBytes, expectedBytes) - allocatedBytes);
            if (elapsedTicks >= totalTicks) {
                leaking = false;
            }
        }
        return status();
    }

    public synchronized void stop() {
        if (!leaking) {
            return;
        }
        leaking = false;
        targetBytes = allocatedBytes;
    }

    public synchronized void clear() {
        leaking = false;
        leakedChunks.clear();
        startBytes = 0L;
        targetBytes = 0L;
        allocatedBytes = 0L;
    }

    public synchronized LeakStatus status() {
        int remainingTicks = leaking ? totalTicks - elapsedTicks : 0;
        return new LeakStatus(leaking, startBytes, allocatedBytes, targetBytes, remainingTicks);
    }

    private void allocate(long bytesToAllocate) {
        while (bytesToAllocate > 0) {
            int chunkSize = (int) Math.min(BYTES_PER_MEGABYTE, bytesToAllocate);
            leakedChunks.add(new byte[chunkSize]);
            allocatedBytes += chunkSize;
            bytesToAllocate -= chunkSize;
        }
    }
}
