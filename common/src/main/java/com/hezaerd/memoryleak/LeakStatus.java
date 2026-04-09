package com.hezaerd.memoryleak;

public record LeakStatus(
        boolean leaking,
        long allocatedBytes,
        long targetBytes,
        int remainingTicks
) {
    public int allocatedMegabytes() {
        return (int) (allocatedBytes / LeakManager.BYTES_PER_MEGABYTE);
    }
    public int targetMegabytes() {
        return (int) (targetBytes / LeakManager.BYTES_PER_MEGABYTE);
    }
    public double progress() {
        if (targetBytes <= 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) allocatedBytes / (double) targetBytes);
    }
    public double remainingSeconds() {
        return remainingTicks / 20.0;
    }
}
