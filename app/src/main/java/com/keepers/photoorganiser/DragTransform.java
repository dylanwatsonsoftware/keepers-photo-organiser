package com.keepers.photoorganiser;

public record DragTransform(float x, float y, float alpha, float backgroundAlpha) {
    public static DragTransform from(float dx, float dy, float height) {
        if (Math.abs(dx) >= Math.abs(dy)) return new DragTransform(dx, 0, 1, 1);
        if (dy <= 0) return new DragTransform(0, 0, 1, 1);
        float alpha = Math.max(0.5f, 1f - dy / Math.max(1f, height) * 0.5f);
        float backgroundAlpha = Math.max(0f,
                1f - dy / Math.max(1f, height * 0.35f));
        return new DragTransform(0, dy, alpha, backgroundAlpha);
    }
}
