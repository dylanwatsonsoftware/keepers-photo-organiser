package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.Test;

public class ConfirmedPersonFacesTest {
    @Test public void includesOnlyHumanConfirmedFaceCorrections() {
        FaceObservation confirmed = face("confirmed", 0);
        FaceObservation guessed = face("guessed", 0);

        List<FaceObservation> result = ConfirmedPersonFaces.forPerson("ada",
                List.of(confirmed, guessed), Map.of(
                        FaceCorrectionStore.key(confirmed), "ada"));

        assertEquals(List.of(confirmed), result);
    }

    private static FaceObservation face(String photo, int index) {
        return new FaceObservation(photo, index, .2, .2, .6, .7,
                -1, -1, -1, 0, 0, "1,0,0");
    }
}
