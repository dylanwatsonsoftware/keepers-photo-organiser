package com.keepers.photoorganiser;

public final class AnalysisGestureRouting {
    private AnalysisGestureRouting() {}

    public static boolean handleAsPhotoGesture(boolean assessmentAlreadyOpen) {
        return !assessmentAlreadyOpen;
    }

    public static boolean isHorizontalPageSwipe(float deltaX, float deltaY, float threshold) {
        return Math.abs(deltaX) >= threshold && Math.abs(deltaX) > Math.abs(deltaY);
    }
}
