package com.keepers.photoorganiser;

public final class InfiniteScrollTrigger {
    private final int threshold;
    private boolean armed = true;

    public InfiniteScrollTrigger(int threshold) {
        this.threshold = threshold;
    }

    public boolean onScroll(int scrollY, int viewportHeight, int contentHeight, boolean hasMore) {
        int remaining = contentHeight - scrollY - viewportHeight;
        if (remaining > threshold) {
            armed = true;
            return false;
        }
        if (!hasMore || !armed) return false;
        armed = false;
        return true;
    }
}
