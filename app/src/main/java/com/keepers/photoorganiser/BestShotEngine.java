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
        Set<String> recommendations = new HashSet<>();
        List<PhotoFeatures> group = new ArrayList<>();
        for (PhotoFeatures photo : photos) {
            if (!group.isEmpty() && !isNearDuplicate(group.get(group.size() - 1), photo)) {
                addBestIfDuplicateGroup(group, recommendations);
                group.clear();
            }
            group.add(photo);
        }
        addBestIfDuplicateGroup(group, recommendations);
        return recommendations;
    }

    private static boolean isNearDuplicate(PhotoFeatures first, PhotoFeatures second) {
        return Math.abs(second.takenAtMillis() - first.takenAtMillis()) <= SCENE_WINDOW_MILLIS
                && Long.bitCount(first.perceptualHash() ^ second.perceptualHash())
                <= MAX_HASH_DISTANCE;
    }

    private static void addBestIfDuplicateGroup(List<PhotoFeatures> group, Set<String> result) {
        if (group.size() < 2) return;
        PhotoFeatures best = group.get(0);
        for (int index = 1; index < group.size(); index++) {
            if (group.get(index).quality() > best.quality()) best = group.get(index);
        }
        result.add(best.id());
    }
}
