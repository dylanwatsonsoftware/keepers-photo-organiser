package com.keepers.photoorganiser;

public record PhotoFeatures(String id, long takenAtMillis, long perceptualHash,
        double quality) {}
