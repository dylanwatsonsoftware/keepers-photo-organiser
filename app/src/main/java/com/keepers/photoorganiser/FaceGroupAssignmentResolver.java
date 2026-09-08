package com.keepers.photoorganiser;

import java.util.Map;

public final class FaceGroupAssignmentResolver {
    private FaceGroupAssignmentResolver() {}

    public static String personFor(FaceIdentityGroup group, Map<String, String> assignments) {
        String recovered = "";
        String current = assignments.get(group.id());
        if (current != null && !current.isBlank()) recovered = current;
        for (FaceObservation face : group.members()) {
            String previous = assignments.get(FaceClusterer.identityId(face));
            if (previous == null || previous.isBlank()) continue;
            if (!recovered.isBlank() && !recovered.equals(previous)) return "";
            recovered = previous;
        }
        return recovered;
    }
}
