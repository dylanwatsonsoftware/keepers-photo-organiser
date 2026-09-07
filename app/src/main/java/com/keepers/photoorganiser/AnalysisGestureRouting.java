package com.keepers.photoorganiser;

public final class AnalysisGestureRouting {
    private AnalysisGestureRouting() {}

    public static boolean handleAsPhotoGesture(boolean assessmentAlreadyOpen) {
        return !assessmentAlreadyOpen;
    }
}
