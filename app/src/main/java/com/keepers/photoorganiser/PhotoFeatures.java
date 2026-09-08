package com.keepers.photoorganiser;

public record PhotoFeatures(String id, long takenAtMillis, long perceptualHash,
        double quality, double focus, double exposure, double composition,
        double motionStability, int faceCount, double smile, double eyesOpen) {
    public PhotoFeatures(String id, long takenAtMillis, long perceptualHash, double quality) {
        this(id, takenAtMillis, perceptualHash, quality, quality, 0.5, 0.5, quality, 0, -1, -1);
    }
    public PhotoFeatures(String id, long takenAtMillis, long perceptualHash, double quality,
            double focus, double exposure, double composition, double motionStability) {
        this(id, takenAtMillis, perceptualHash, quality, focus, exposure, composition,
                motionStability, 0, -1, -1);
    }
}
