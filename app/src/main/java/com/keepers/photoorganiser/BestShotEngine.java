package com.keepers.photoorganiser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BestShotEngine {
    static final long SCENE_WINDOW_MILLIS = 120_000;
    static final long NAMED_FACE_WINDOW_MILLIS = 60_000;
    static final int MAX_WITHIN_STACK_HASH_DISTANCE = 24;
    static final double MIN_USABLE_STACK_QUALITY = 0.03;
    static final int NEAR_IDENTICAL_HASH_DISTANCE = 6;

    private BestShotEngine() {}

    public static Set<String> recommend(List<PhotoFeatures> photos) {
        return classify(photos).recommended();
    }

    public static BestShotResult classify(List<PhotoFeatures> photos) {
        return classify(photos, RecommendationPreferenceProfile.learn(List.of()));
    }

    public static BestShotResult classify(List<PhotoFeatures> photos,
            Map<String, Set<String>> namedFaces) {
        return classify(photos, RecommendationPreferenceProfile.learn(List.of()), namedFaces);
    }

    public static BestShotResult classify(List<PhotoFeatures> photos,
            RecommendationPreferenceProfile profile) {
        return classify(photos, profile, Map.of());
    }

    public static BestShotResult classify(List<PhotoFeatures> photos,
            RecommendationPreferenceProfile profile, Map<String, Set<String>> namedFaces) {
        Set<String> initial = recommendInitial(photos, profile, namedFaces);
        List<PhotoFeatures> ranked = new ArrayList<>(photos);
        ranked.sort(java.util.Comparator.comparingDouble(profile::score).reversed()
                .thenComparing(PhotoFeatures::id));
        Set<String> recommended = new java.util.LinkedHashSet<>();
        for (PhotoFeatures photo : ranked) {
            if (!initial.contains(photo.id())) continue;
            boolean duplicate = ranked.stream().filter(candidate -> recommended.contains(candidate.id()))
                    .anyMatch(candidate -> nearIdentical(photo, candidate));
            if (!duplicate) recommended.add(photo.id());
        }
        Set<String> alternatives = new java.util.LinkedHashSet<>();
        for (PhotoFeatures photo : ranked) {
            if (recommended.contains(photo.id())) continue;
            for (PhotoFeatures selected : ranked) {
                if (!recommended.contains(selected.id()) || !nearIdentical(photo, selected)) continue;
                if (profile.score(photo) >= MIN_USABLE_STACK_QUALITY
                        && profile.score(photo) >= profile.score(selected) * 0.75) alternatives.add(photo.id());
                break;
            }
        }
        return new BestShotResult(recommended, alternatives);
    }

    private static boolean nearIdentical(PhotoFeatures first, PhotoFeatures second) {
        return Long.bitCount(first.perceptualHash() ^ second.perceptualHash())
                <= NEAR_IDENTICAL_HASH_DISTANCE;
    }

    private static Set<String> recommendInitial(List<PhotoFeatures> photos,
            RecommendationPreferenceProfile profile, Map<String, Set<String>> namedFaces) {
        List<List<PhotoFeatures>> groups = sceneGroups(photos, namedFaces);
        List<PhotoFeatures> candidates = new ArrayList<>();
        for (List<PhotoFeatures> group : groups) candidates.add(best(group, profile));
        candidates.sort(java.util.Comparator.comparingDouble(profile::score).reversed()
                .thenComparing(PhotoFeatures::id));
        int limit = (candidates.size() + 2) / 3;
        Set<String> recommendations = new HashSet<>();
        for (int index = 0; index < limit; index++) recommendations.add(candidates.get(index).id());
        for (List<PhotoFeatures> group : groups) {
            if (group.size() < 2) continue;
            PhotoFeatures stackBest = best(group, profile);
            if (stackBest.quality() >= MIN_USABLE_STACK_QUALITY) {
                recommendations.add(stackBest.id());
            }
        }
        return recommendations;
    }

    public static Map<String, PhotoStackPosition> stacks(List<PhotoFeatures> photos) {
        return stacks(photos, Map.of());
    }

    public static Map<String, PhotoStackPosition> stacks(List<PhotoFeatures> photos,
            Map<String, Set<String>> namedFaces) {
        Map<String, PhotoStackPosition> result = new LinkedHashMap<>();
        for (List<PhotoFeatures> group : sceneGroups(photos, namedFaces)) {
            if (group.size() < 2) continue;
            for (int index = 0; index < group.size(); index++) {
                result.put(group.get(index).id(),
                        new PhotoStackPosition(index + 1, group.size()));
            }
        }
        return result;
    }

    public static Map<String, List<String>> stackMembers(List<PhotoFeatures> photos) {
        return stackMembers(photos, Map.of());
    }

    public static Map<String, List<String>> stackMembers(List<PhotoFeatures> photos,
            Map<String, Set<String>> namedFaces) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (List<PhotoFeatures> group : sceneGroups(photos, namedFaces)) {
            if (group.size() < 2) continue;
            List<String> ids = group.stream().map(PhotoFeatures::id).toList();
            for (String id : ids) result.put(id, ids);
        }
        return result;
    }

    private static List<List<PhotoFeatures>> sceneGroups(List<PhotoFeatures> photos,
            Map<String, Set<String>> namedFaces) {
        List<PhotoFeatures> ordered = ordered(photos);
        List<List<PhotoFeatures>> groups = new ArrayList<>();
        for (PhotoFeatures photo : ordered) {
            List<PhotoFeatures> latest = groups.isEmpty() ? null : groups.get(groups.size() - 1);
            PhotoFeatures previous = latest == null ? null : latest.get(latest.size() - 1);
            boolean timeBreak = previous != null && photo.takenAtMillis()
                    - previous.takenAtMillis() > SCENE_WINDOW_MILLIS;
            boolean sameNamedMoment = previous != null && photo.takenAtMillis()
                    - previous.takenAtMillis() <= NAMED_FACE_WINDOW_MILLIS
                    && sharesNamedFace(previous.id(), photo.id(), namedFaces);
            boolean visualBreak = previous != null && Long.bitCount(previous.perceptualHash()
                    ^ photo.perceptualHash()) > MAX_WITHIN_STACK_HASH_DISTANCE
                    && !sameNamedMoment;
            if (previous == null || timeBreak || visualBreak) {
                latest = new ArrayList<>();
                groups.add(latest);
            }
            latest.add(photo);
        }
        return groups;
    }

    private static boolean sharesNamedFace(String first, String second,
            Map<String, Set<String>> namedFaces) {
        Set<String> firstFaces = namedFaces.getOrDefault(first, Set.of());
        Set<String> secondFaces = namedFaces.getOrDefault(second, Set.of());
        return !firstFaces.isEmpty() && firstFaces.stream().anyMatch(secondFaces::contains);
    }

    private static List<PhotoFeatures> ordered(List<PhotoFeatures> photos) {
        List<PhotoFeatures> ordered = new ArrayList<>(photos);
        ordered.sort(java.util.Comparator.comparingLong(PhotoFeatures::takenAtMillis)
                .thenComparing(PhotoFeatures::id));
        return ordered;
    }

    private static PhotoFeatures best(List<PhotoFeatures> group,
            RecommendationPreferenceProfile profile) {
        PhotoFeatures best = group.get(0);
        for (int index = 1; index < group.size(); index++) {
            PhotoFeatures candidate = group.get(index);
            if (profile.score(candidate) > profile.score(best)
                    || profile.score(candidate) == profile.score(best)
                    && candidate.id().compareTo(best.id()) < 0) best = candidate;
        }
        return best;
    }
}
