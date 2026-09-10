package com.keepers.photoorganiser;

public final class InfiniteScrollTrigger {
    private final int threshold;
    private boolean armed = true;
    private String lastContentState;

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

    public boolean onContentLayout(int viewportHeight, int contentHeight, boolean hasMore,
            String contentState) {
        if (!hasMore || viewportHeight <= 0
                || contentHeight > viewportHeight + threshold) return false;
        if (contentState.equals(lastContentState)) return false;
        lastContentState = contentState;
        return true;
    }
}
