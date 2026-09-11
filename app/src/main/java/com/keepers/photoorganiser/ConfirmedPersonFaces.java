package com.keepers.photoorganiser;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ConfirmedPersonFaces {
    private ConfirmedPersonFaces() {}

    public static List<FaceObservation> forPerson(String personId,
            List<FaceObservation> observations, Map<String, String> corrections) {
        return observations.stream().filter(face -> personId.equals(
                        corrections.get(FaceCorrectionStore.key(face))))
                .sorted(Comparator.comparing(FaceObservation::photoId)
                        .thenComparingInt(FaceObservation::faceIndex)).toList();
    }
}
