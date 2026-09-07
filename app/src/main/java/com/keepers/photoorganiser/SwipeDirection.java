package com.keepers.photoorganiser;

public enum SwipeDirection {
    NONE, NEXT, PREVIOUS, BACK;
    public static SwipeDirection classify(float dx, float dy, float threshold) {
        if (Math.max(Math.abs(dx), Math.abs(dy)) < threshold) return NONE;
        if (Math.abs(dy) > Math.abs(dx)) return dy > 0 ? BACK : NONE;
        return dx < 0 ? NEXT : PREVIOUS;
    }
}
