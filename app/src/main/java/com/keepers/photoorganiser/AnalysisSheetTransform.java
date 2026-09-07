package com.keepers.photoorganiser;

public record AnalysisSheetTransform(float photoTranslationY, float sheetTranslationY) {
    public static AnalysisSheetTransform from(float dragY, float viewportHeight,
            float openPhotoEdge) {
        float revealDistance = Math.max(1, viewportHeight - openPhotoEdge);
        float photoY = Math.max(-revealDistance, Math.min(0, dragY));
        return new AnalysisSheetTransform(photoY, revealDistance + photoY);
    }
}
