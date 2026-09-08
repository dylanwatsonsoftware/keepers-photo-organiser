package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

public class FaceClustererTest {
    @Test public void groupsSimilarFacesAndKeepsDistinctFacesSeparate() {
        List<FaceIdentityGroup> groups = FaceClusterer.cluster(List.of(
                sample("a", "1,0,0"), sample("b", ".99,.01,0"),
                sample("c", "0,0,1")), .15);

        assertEquals(2, groups.size());
        assertEquals(List.of("a", "b"), groups.get(0).photoIds());
        assertEquals(List.of("c"), groups.get(1).photoIds());
    }

    @Test public void ignoresObservationsWithoutDescriptors() {
        assertEquals(List.of(), FaceClusterer.cluster(List.of(sample("a", "")), .15));
    }

    private static FaceObservation sample(String photo, String descriptor) {
        return new FaceObservation(photo, 0, 0, 0, 1, 1,
                -1, -1, -1, 0, 0, descriptor);
    }
}
