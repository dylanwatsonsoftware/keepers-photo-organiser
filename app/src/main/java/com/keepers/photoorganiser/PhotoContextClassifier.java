package com.keepers.photoorganiser;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PhotoContextClassifier {
    private static final Map<PhotoContextType, Set<String>> LABELS = Map.of(
            PhotoContextType.PORTRAIT, Set.of("portrait", "selfie", "person", "smile"),
            PhotoContextType.GROUP, Set.of("crowd", "team", "community"),
            PhotoContextType.PET, Set.of("animal", "bird", "cat", "dog", "mammal", "pet", "wildlife"),
            PhotoContextType.LANDSCAPE, Set.of("beach", "forest", "lake", "landscape", "mountain", "nature", "sea", "sky", "sunset"),
            PhotoContextType.FOOD, Set.of("cuisine", "dessert", "dish", "food", "fruit", "meal", "vegetable"),
            PhotoContextType.ACTION, Set.of("dancing", "running", "sport", "sports", "surfing", "swimming"),
            PhotoContextType.DOCUMENT, Set.of("document", "paper", "receipt", "screenshot", "text"),
            PhotoContextType.LOW_LIGHT, Set.of("darkness", "night", "night sky")
    );

    private PhotoContextClassifier() {}

    public static PhotoContext infer(PhotoFeatures photo, List<ImageLabelSignal> labels) {
        EnumMap<PhotoContextType, Double> probabilities = new EnumMap<>(PhotoContextType.class);
        if (photo.faceCount() == 1) probabilities.put(PhotoContextType.PORTRAIT, .85);
        if (photo.faceCount() > 1) {
            probabilities.put(PhotoContextType.GROUP, .9);
            probabilities.put(PhotoContextType.PORTRAIT, .4);
        }
        for (ImageLabelSignal label : labels) {
            String normalized = label.label().trim().toLowerCase(Locale.ROOT);
            for (Map.Entry<PhotoContextType, Set<String>> category : LABELS.entrySet()) {
                if (category.getValue().contains(normalized)) probabilities.merge(
                        category.getKey(), clamp(label.confidence()), Math::max);
            }
        }
        return new PhotoContext(probabilities);
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
