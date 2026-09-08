package com.keepers.photoorganiser;

import java.util.HashMap;
import java.util.Map;

final class FaceGroupSuggestion {
    static final double MAXIMUM_DISTANCE = .38;
    private static final double REQUIRED_AGREEMENT = .75;

    private FaceGroupSuggestion() {}

    static String personId(FaceIdentityGroup group, Map<String, String> predictions) {
        HashMap<String, Integer> counts = new HashMap<>();
        for (FaceObservation face : group.members()) {
            String person = predictions.get(FaceCorrectionStore.key(face));
            if (person != null && !FaceCorrectionStore.IGNORE.equals(person))
                counts.merge(person, 1, Integer::sum);
        }
        int required = (int) Math.ceil(group.members().size() * REQUIRED_AGREEMENT);
        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() >= required)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey).findFirst().orElse("");
    }
}
