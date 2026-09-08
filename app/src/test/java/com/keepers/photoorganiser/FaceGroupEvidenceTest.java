package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.Test;

public class FaceGroupEvidenceTest {
    @Test public void groupConfirmationBecomesDurablePerFaceEvidence() {
        FaceIdentityGroup group = group();

        assertEquals(Map.of("photo-a#0", "ada", "photo-b#0", "ada"),
                FaceGroupEvidence.applyChoice(group, Map.of(), "", "ada"));
    }

    @Test public void reassignmentUpdatesInheritedEvidenceButPreservesManualCorrections() {
        FaceIdentityGroup group = group();

        assertEquals(Map.of("photo-a#0", "ben", "photo-b#0", "cam"),
                FaceGroupEvidence.applyChoice(group,
                        Map.of("photo-a#0", "ada", "photo-b#0", "cam"), "ada", "ben"));
    }

    private static FaceIdentityGroup group() {
        return new FaceIdentityGroup("changing-group-id", List.of(
                face("photo-a"), face("photo-b")));
    }

    private static FaceObservation face(String photo) {
        return new FaceObservation(photo, 0, 0, 0, 1, 1,
                -1, -1, -1, 0, 0, "1,0");
    }
}
