package com.keepers.photoorganiser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BestShotEngine {
    static final long SCENE_WINDOW_MILLIS = 120_000;
    static final int MAX_WITHIN_STACK_HASH_DISTANCE = 24;
    static final double MIN_USABLE_STACK_QUALITY = 0.03;

    private BestShotEngine() {}

    public static Set<String> recommend(List<PhotoFeatures> photos) {
        List<List<PhotoFeatures>> groups = sceneGroups(photos);
        List<PhotoFeatures> candidates = new ArrayList<>();
        for (List<PhotoFeatures> group : groups) candidates.add(best(group));
        candidates.sort(java.util.Comparator.comparingDouble(PhotoFeatures::quality).reversed()
                .thenComparing(PhotoFeatures::id));
        int limit = (candidates.size() + 2) / 3;
        Set<String> recommendations = new HashSet<>();
        for (int index = 0; index < limit; index++) recommendations.add(candidates.get(index).id());
        for (List<PhotoFeatures> group : groups) {
            if (group.size() < 2) continue;
            PhotoFeatures stackBest = best(group);
            if (stackBest.quality() >= MIN_USABLE_STACK_QUALITY) {
                recommendations.add(stackBest.id());
            }
        }
        return recommendations;
    }

    public static Map<String, PhotoStackPosition> stacks(List<PhotoFeatures> photos) {
        Map<String, PhotoStackPosition> result = new LinkedHashMap<>();
        for (List<PhotoFeatures> group : sceneGroups(photos)) {
            if (group.size() < 2) continue;
            for (int index = 0; index < group.size(); index++) {
                result.put(group.get(index).id(),
                        new PhotoStackPosition(index + 1, group.size()));
            }
        }
        return result;
    }

    private static List<List<PhotoFeatures>> sceneGroups(List<PhotoFeatures> photos) {
        List<PhotoFeatures> ordered = ordered(photos);
        List<List<PhotoFeatures>> groups = new ArrayList<>();
        for (PhotoFeatures photo : ordered) {
            List<PhotoFeatures> latest = groups.isEmpty() ? null : groups.get(groups.size() - 1);
            PhotoFeatures previous = latest == null ? null : latest.get(latest.size() - 1);
            boolean timeBreak = previous != null && photo.takenAtMillis()
                    - previous.takenAtMillis() > SCENE_WINDOW_MILLIS;
            boolean visualBreak = previous != null && Long.bitCount(previous.perceptualHash()
                    ^ photo.perceptualHash()) > MAX_WITHIN_STACK_HASH_DISTANCE;
            if (previous == null || timeBreak || visualBreak) {
                latest = new ArrayList<>();
                groups.add(latest);
            }
            latest.add(photo);
        }
        return groups;
    }

    private static List<PhotoFeatures> ordered(List<PhotoFeatures> photos) {
        List<PhotoFeatures> ordered = new ArrayList<>(photos);
        ordered.sort(java.util.Comparator.comparingLong(PhotoFeatures::takenAtMillis)
                .thenComparing(PhotoFeatures::id));
        return ordered;
    }

    private static PhotoFeatures best(List<PhotoFeatures> group) {
        PhotoFeatures best = group.get(0);
        for (int index = 1; index < group.size(); index++) {
            PhotoFeatures candidate = group.get(index);
            if (candidate.quality() > best.quality()
                    || candidate.quality() == best.quality()
                    && candidate.id().compareTo(best.id()) < 0) best = candidate;
        }
        return best;
    }
}
