package com.keepers.photoorganiser;

final class PhotoZoomState {
    static final float MIN_SCALE = 1f;
    static final float MAX_SCALE = 4f;

    private float scale = MIN_SCALE;
    private float translationX;
    private float translationY;

    void scaleBy(float factor) {
        scale = clamp(scale * factor, MIN_SCALE, MAX_SCALE);
        if (!isZoomed()) {
            translationX = 0f;
            translationY = 0f;
        }
    }

    void panBy(float deltaX, float deltaY, int viewportWidth, int viewportHeight) {
        if (!isZoomed()) return;
        float maxX = Math.max(0f, viewportWidth * (scale - 1f) / 2f);
        float maxY = Math.max(0f, viewportHeight * (scale - 1f) / 2f);
        translationX = clamp(translationX + deltaX, -maxX, maxX);
        translationY = clamp(translationY + deltaY, -maxY, maxY);
    }

    boolean isZoomed() { return scale > MIN_SCALE + 0.001f; }

    boolean allowsPageGesture() { return !isZoomed(); }

    float scale() { return scale; }

    float translationX() { return translationX; }

    float translationY() { return translationY; }

    void reset() {
        scale = MIN_SCALE;
        translationX = 0f;
        translationY = 0f;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
