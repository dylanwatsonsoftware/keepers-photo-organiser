package com.keepers.photoorganiser;

import java.util.HashMap;
import java.util.Map;

public final class FaceGroupEvidence {
    private FaceGroupEvidence() {}

    public static Map<String, String> applyChoice(FaceIdentityGroup group,
            Map<String, String> existing, String previousPersonId, String selectedPersonId) {
        HashMap<String, String> result = new HashMap<>(existing);
        String previous = previousPersonId == null ? "" : previousPersonId;
        String selected = selectedPersonId == null ? "" : selectedPersonId;
        for (FaceObservation face : group.members()) {
            String key = FaceCorrectionStore.key(face);
            String current = result.get(key);
            boolean inheritedOrUnset = current == null || current.equals(previous);
            if (!inheritedOrUnset) continue;
            if (selected.isBlank()) result.remove(key); else result.put(key, selected);
        }
        return Map.copyOf(result);
    }
}
