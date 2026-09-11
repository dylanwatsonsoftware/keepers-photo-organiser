package com.keepers.photoorganiser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record StackPreferenceComparison(PhotoFeatures preferred, PhotoFeatures alternative) {
    public static List<StackPreferenceComparison> from(List<PhotoFeatures> features,
            Map<String, List<String>> stacks, Set<String> keepers) {
        HashMap<String, PhotoFeatures> featuresById = new HashMap<>();
        for (PhotoFeatures feature : features) featuresById.put(feature.id(), feature);
        ArrayList<StackPreferenceComparison> comparisons = new ArrayList<>();
        Set<List<String>> handledStacks = new HashSet<>();
        Set<String> handledPairs = new LinkedHashSet<>();
        for (PhotoFeatures feature : features) {
            List<String> stack = stacks.get(feature.id());
            if (stack == null || !handledStacks.add(stack)) continue;
            for (String preferredId : stack) {
                PhotoFeatures preferred = featuresById.get(preferredId);
                if (preferred == null || !keepers.contains(preferredId)) continue;
                for (String alternativeId : stack) {
                    PhotoFeatures alternative = featuresById.get(alternativeId);
                    if (alternative == null || keepers.contains(alternativeId)) continue;
                    if (handledPairs.add(preferredId + "\n" + alternativeId))
                        comparisons.add(new StackPreferenceComparison(preferred, alternative));
                }
            }
        }
        return List.copyOf(comparisons);
    }
}
