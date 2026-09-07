package com.keepers.photoorganiser;

public record PhotoFeatures(String id, long takenAtMillis, long perceptualHash,
        double quality, double focus, double exposure, double composition,
        double motionStability) {
    public PhotoFeatures(String id, long takenAtMillis, long perceptualHash, double quality) {
        this(id, takenAtMillis, perceptualHash, quality, quality, 0.5, 0.5, quality);
    }
}
