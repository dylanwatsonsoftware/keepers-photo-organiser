package com.keepers.photoorganiser;

public record AnalysisSheetTransform(float translationY, float alpha) {
    public static AnalysisSheetTransform from(float dragY, float sheetHeight) {
        float height = Math.max(1, sheetHeight);
        float progress = Math.min(1, Math.max(0, -dragY / height));
        return new AnalysisSheetTransform(height * (1 - progress), progress);
    }
}
