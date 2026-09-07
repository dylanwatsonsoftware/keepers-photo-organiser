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
        List<List<PhotoFeatures>> groups = new ArrayList<>();
        for (PhotoFeatures photo : photos) {
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
        Set<String> recommendations = new HashSet<>();
        for (List<PhotoFeatures> group : groups) addBestIfDuplicateGroup(group, recommendations);
        return recommendations;
    }

    private static void addBestIfDuplicateGroup(List<PhotoFeatures> group, Set<String> result) {
        if (group.isEmpty()) return;
        PhotoFeatures best = group.get(0);
        for (int index = 1; index < group.size(); index++) {
            if (group.get(index).quality() > best.quality()) best = group.get(index);
        }
        result.add(best.id());
    }
}
