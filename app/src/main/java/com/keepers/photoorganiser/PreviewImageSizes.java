package com.keepers.photoorganiser;

public record PreviewImageSizes(int previewPixels, int fullPixels) {
    private static final int FAST_PREVIEW_PIXELS = 480;
    private static final int MAX_FULL_PIXELS = 3072;

    public static PreviewImageSizes forScreen(int largestScreenDimension) {
        int full = Math.min(MAX_FULL_PIXELS, Math.max(1, largestScreenDimension));
        return new PreviewImageSizes(Math.min(FAST_PREVIEW_PIXELS, full), full);
    }
}
