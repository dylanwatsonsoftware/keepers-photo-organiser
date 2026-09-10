package com.keepers.photoorganiser;

import java.util.Comparator;
import java.util.List;
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

    public static List<FaceIdentityGroup> order(List<FaceIdentityGroup> groups,
            Map<String, String> groupAssignments, Map<String, String> faceCorrections,
            Map<String, String> predictions) {
        return groups.stream().sorted(Comparator
                .comparingInt((FaceIdentityGroup group) -> needsReview(
                        group, groupAssignments, faceCorrections) ? 0 : 1)
                .thenComparingInt(group -> FaceGroupSuggestion.personId(group, predictions)
                        .isBlank() ? 1 : 0)
                .thenComparing(Comparator.comparingLong((FaceIdentityGroup group) ->
                        group.photoIds().stream().distinct().count()).reversed())
                .thenComparing(FaceIdentityGroup::id)).toList();
    }
}
