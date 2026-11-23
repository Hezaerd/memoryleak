package com.hezaerd.memoryleak.core;

public final class LeakStatus {
    public enum BarColor { GREEN, YELLOW, RED }

    private final boolean leaking;
    private final long startBytes;
    private final long allocatedBytes;
    private final long targetBytes;
    private final int remainingTicks;

    LeakStatus(boolean leaking, long startBytes, long allocatedBytes, long targetBytes, int remainingTicks) {
        this.leaking = leaking;
        this.startBytes = startBytes;
        this.allocatedBytes = allocatedBytes;
        this.targetBytes = targetBytes;
        this.remainingTicks = remainingTicks;
    }

    public boolean leaking() {
        return leaking;
    }

    public int allocatedMegabytes() {
        return (int) (allocatedBytes / LeakManager.BYTES_PER_MEGABYTE);
    }

    public int targetMegabytes() {
        return (int) (targetBytes / LeakManager.BYTES_PER_MEGABYTE);
    }

    public double progress() {
        long leakBytes = targetBytes - startBytes;
        if (leakBytes <= 0) {
            return leaking ? 0.0 : 1.0;
        }
        return Math.min(1.0, (double) (allocatedBytes - startBytes) / leakBytes);
    }

    public double remainingSeconds() {
        return remainingTicks / (double) LeakManager.TICKS_PER_SECOND;
    }

    public BarColor barColor() {
        double progress = progress();
        if (progress < 0.33) {
            return BarColor.GREEN;
        }
        return progress < 0.66 ? BarColor.YELLOW : BarColor.RED;
    }

    public String barTitle() {
        return String.format("Memory Leak: %d/%d MB (%.1f%%)", allocatedMegabytes(), targetMegabytes(), progress() * 100.0);
    }
}
