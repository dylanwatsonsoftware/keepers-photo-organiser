package com.keepers.photoorganiser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BestShotEngine {
    static final long SCENE_WINDOW_MILLIS = 120_000;
    static final int MAX_HASH_DISTANCE = 10;

    private BestShotEngine() {}

    public static Set<String> recommend(List<PhotoFeatures> photos) {
        List<PhotoFeatures> ordered = new ArrayList<>(photos);
        ordered.sort(java.util.Comparator.comparingLong(PhotoFeatures::takenAtMillis)
                .thenComparing(PhotoFeatures::id));
        List<List<PhotoFeatures>> groups = new ArrayList<>();
        for (PhotoFeatures photo : ordered) {
            List<PhotoFeatures> match = null;
            for (List<PhotoFeatures> group : groups) {
                if (Long.bitCount(group.get(0).perceptualHash() ^ photo.perceptualHash())
                        <= MAX_HASH_DISTANCE) {
                    match = group;
                    break;
                }
            }
            if (match == null) { match = new ArrayList<>(); groups.add(match); }
            match.add(photo);
        }
        List<PhotoFeatures> candidates = new ArrayList<>();
        for (List<PhotoFeatures> group : groups) candidates.add(best(group));
        candidates.sort(java.util.Comparator.comparingDouble(PhotoFeatures::quality).reversed()
                .thenComparing(PhotoFeatures::id));
        int limit = (candidates.size() + 2) / 3;
        Set<String> recommendations = new HashSet<>();
        for (int index = 0; index < limit; index++) recommendations.add(candidates.get(index).id());
        return recommendations;
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
