package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.Test;

public class FaceIdentityLearnerTest {
    @Test public void explicitCorrectionTeachesAVisuallySimilarNewFace() {
        FaceObservation taught = face("old", "1,0,0");
        FaceObservation similar = face("new", ".99,.01,0");

        Map<String, String> learned = FaceIdentityLearner.predict(List.of(taught, similar),
                Map.of("old#0", "ada"), .15);

        assertEquals("ada", learned.get("new#0"));
    }

    @Test public void doesNotGuessForDistantOrExplicitlyIgnoredFaces() {
        FaceObservation taught = face("old", "1,0,0");
        FaceObservation distant = face("far", "0,0,1");
        FaceObservation ignored = face("ignored", ".99,.01,0");

        assertEquals(Map.of(), FaceIdentityLearner.predict(List.of(taught, distant, ignored),
                Map.of("old#0", "ada", "ignored#0", FaceCorrectionStore.IGNORE), .15));
    }

    @Test public void doesNotGuessWhenTwoPeopleAreSimilarlyClose() {
        assertEquals(Map.of(), FaceIdentityLearner.predict(List.of(
                        face("ada", "1,0,0"), face("ben", ".98,.02,0"),
                        face("new", ".99,.01,0")),
                Map.of("ada#0", "ada", "ben#0", "ben"), .15));
    }

    @Test public void confirmedGroupMembersBecomePredictionEvidence() {
        FaceObservation adaOne = face("ada-one", "1,0,0");
        FaceObservation adaTwo = face("ada-two", ".98,.02,0");
        FaceObservation unknown = face("unknown", ".99,.01,0");
        FaceIdentityGroup confirmed = new FaceIdentityGroup("ada-group", List.of(adaOne, adaTwo));

        Map<String, String> learned = FaceIdentityLearner.predict(
                List.of(adaOne, adaTwo, unknown), List.of(confirmed),
                Map.of("ada-group", "ada"), Map.of(), .15);

        assertEquals("ada", learned.get("unknown#0"));
    }

    @Test public void repeatedConfirmationsFormAPersonProfileInsteadOfUsingOneNearestFace() {
        FaceObservation adaLeft = face("ada-left", ".8,.6");
        FaceObservation adaRight = face("ada-right", ".8,-.6");
        FaceObservation misleadingBen = face("ben", ".95,.31225");
        FaceObservation unknown = face("unknown", "1,0");

        Map<String, String> learned = FaceIdentityLearner.predict(
                List.of(adaLeft, adaRight, misleadingBen, unknown),
                Map.of("ada-left#0", "ada", "ada-right#0", "ada",
                        "ben#0", "ben"), .25);

        assertEquals("ada", learned.get("unknown#0"));
    }

    private static FaceObservation face(String photo, String descriptor) {
        return new FaceObservation(photo, 0, 0, 0, 1, 1,
                -1, -1, -1, 0, 0, descriptor);
    }
}
