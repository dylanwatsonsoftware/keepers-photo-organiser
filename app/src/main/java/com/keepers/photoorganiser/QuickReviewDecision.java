package com.keepers.photoorganiser;

public enum QuickReviewDecision {
    NONE, KEEP, REJECT;

    public static QuickReviewDecision fromSwipe(float deltaX, float threshold) {
        if (Math.abs(deltaX) < threshold) return NONE;
        return deltaX > 0 ? KEEP : REJECT;
    }
}
