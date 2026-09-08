package com.keepers.photoorganiser;

import java.util.Map;

public final class FaceReviewInbox {
    private FaceReviewInbox() {}

    public static boolean needsReview(FaceIdentityGroup group,
            Map<String, String> groupAssignments, Map<String, String> faceCorrections) {
        String assigned = FaceGroupAssignmentResolver.personFor(group, groupAssignments);
        if (!assigned.isBlank()) return false;
        if (group.members().isEmpty()) return true;
        for (FaceObservation face : group.members())
            if (!faceCorrections.containsKey(FaceCorrectionStore.key(face))) return true;
        return false;
    }
}
