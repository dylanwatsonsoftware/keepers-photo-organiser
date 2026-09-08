package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.Test;

public class FaceGroupSuggestionTest {
    @Test public void requiresThreeQuartersOfAGroupToAgree() {
        FaceObservation one = face("one");
        FaceObservation two = face("two");
        FaceObservation three = face("three");
        FaceObservation four = face("four");
        FaceIdentityGroup group = new FaceIdentityGroup("group", List.of(one, two, three, four));

        assertEquals("ada", FaceGroupSuggestion.personId(group, Map.of(
                "one#0", "ada", "two#0", "ada", "three#0", "ada")));
        assertEquals("", FaceGroupSuggestion.personId(group, Map.of(
                "one#0", "ada", "two#0", "ada")));
    }

    private static FaceObservation face(String photo) {
        return new FaceObservation(photo, 0, 0, 0, 1, 1,
                -1, -1, -1, 0, 0, "1,0");
    }
}
