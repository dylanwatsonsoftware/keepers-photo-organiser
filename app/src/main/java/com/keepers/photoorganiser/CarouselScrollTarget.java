package com.keepers.photoorganiser;

public final class CarouselScrollTarget {
    private CarouselScrollTarget() {}
    public static int centered(int selectedIndex, int itemWidth, int viewportWidth,
            int contentWidth) {
        int desired = selectedIndex * itemWidth + itemWidth / 2 - viewportWidth / 2;
        return Math.max(0, Math.min(desired, Math.max(0, contentWidth - viewportWidth)));
    }
}
