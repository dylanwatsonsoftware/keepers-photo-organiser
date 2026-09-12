package com.keepers.photoorganiser;

public final class StackCarouselSwipe {
    private StackCarouselSwipe() {}

    public static int targetIndex(int currentIndex, int itemCount, float deltaX,
            float threshold, float itemWidth) {
        if (itemCount <= 0 || Math.abs(deltaX) < threshold) return currentIndex;
        int steps = Math.max(1, Math.round(Math.abs(deltaX) / Math.max(1, itemWidth)));
        int direction = deltaX < 0 ? 1 : -1;
        return Math.max(0, Math.min(itemCount - 1, currentIndex + direction * steps));
    }
}
