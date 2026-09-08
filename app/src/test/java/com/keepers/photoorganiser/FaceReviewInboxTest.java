package com.keepers.photoorganiser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Test;

public class FaceReviewInboxTest {
    @Test public void assignedGroupsAndFullyCorrectedGroupsAreAlreadyReviewed() {
        FaceIdentityGroup assigned = group("assigned", "a", "b");
        FaceIdentityGroup corrected = group("corrected", "c", "d");

        assertFalse(FaceReviewInbox.needsReview(assigned, Map.of("assigned", "ada"), Map.of()));
        assertFalse(FaceReviewInbox.needsReview(corrected, Map.of(), Map.of(
                "c#0", "ada", "d#0", FaceCorrectionStore.IGNORE)));
    }

    @Test public void untouchedAndPartlyCorrectedGroupsRemainInTheInbox() {
        FaceIdentityGroup group = group("new", "a", "b");

        assertTrue(FaceReviewInbox.needsReview(group, Map.of(), Map.of()));
        assertTrue(FaceReviewInbox.needsReview(group, Map.of(), Map.of("a#0", "ada")));
    }

    private static FaceIdentityGroup group(String id, String... photos) {
        return new FaceIdentityGroup(id, java.util.Arrays.stream(photos).map(photo ->
                new FaceObservation(photo, 0, 0, 0, 1, 1,
                        -1, -1, -1, 0, 0, "1,0")).toList());
    }
}
