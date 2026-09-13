package com.keepers.photoorganiser;

import java.util.EnumMap;
import java.util.Map;

public final class PhotoContext {
    private static final double[][] SIGNAL_MULTIPLIERS = {
            {1, 1, 1, 1, 1, 1, 1},
            {1, 1.3, .9, 1, .8, 1.6, 1.4},
            {1, 1.2, .9, 1, .85, 1.8, 1.2},
            {1, 1.3, 1, 1.1, .8, 1, 1},
            {1.35, 1.05, 1.25, 1.5, 1, 1, 1},
            {1.2, 1.1, 1.15, 1.35, 1, 1, 1},
            {1, 1.25, 1, .95, .4, .7, .7},
            {1.25, 1.8, 1.35, .7, 1.2, 1, 1},
            {1, 1.2, .45, 1, .9, 1, 1}
    };
    private final Map<PhotoContextType, Double> probabilities;

    public PhotoContext(Map<PhotoContextType, Double> probabilities) {
        EnumMap<PhotoContextType, Double> values = new EnumMap<>(PhotoContextType.class);
        probabilities.forEach((type, confidence) -> values.put(type, clamp(confidence)));
        this.probabilities = Map.copyOf(values);
    }

    public static PhotoContext general() { return new PhotoContext(Map.of()); }

    public static PhotoContext of(PhotoContextType type, double confidence) {
        return new PhotoContext(Map.of(type, confidence));
    }

    public double probability(PhotoContextType type) {
        return probabilities.getOrDefault(type, 0.0);
    }

    public Map<PhotoContextType, Double> probabilities() { return probabilities; }

    public PhotoContextType dominantSubject() {
        PhotoContextType best = PhotoContextType.GENERAL;
        double confidence = 0;
        for (Map.Entry<PhotoContextType, Double> entry : probabilities.entrySet()) {
            if (entry.getKey() == PhotoContextType.GENERAL
                    || entry.getKey() == PhotoContextType.LOW_LIGHT) continue;
            if (entry.getValue() > confidence) {
                best = entry.getKey();
                confidence = entry.getValue();
            }
        }
        return confidence >= .35 ? best : PhotoContextType.GENERAL;
    }

    public double signalMultiplier(int signalIndex) {
        double multiplier = 1;
        for (Map.Entry<PhotoContextType, Double> entry : probabilities.entrySet()) {
            double target = SIGNAL_MULTIPLIERS[entry.getKey().ordinal()][signalIndex];
            multiplier += entry.getValue() * (target - 1);
        }
        return Math.max(.25, Math.min(2.5, multiplier));
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
