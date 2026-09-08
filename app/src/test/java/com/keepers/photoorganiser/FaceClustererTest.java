package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
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

    @Test public void groupIdentityDoesNotChangeWhenAnEarlierDistinctFaceIsAdded() {
        String original = FaceClusterer.cluster(List.of(sample("b", "1,0,0")), .15).get(0).id();
        String afterDiscovery = FaceClusterer.cluster(List.of(
                sample("a", "0,0,1"), sample("b", "1,0,0")), .15).get(1).id();

        assertEquals(original, afterDiscovery);
    }

    @Test public void olderAssignmentCanBeRecoveredAfterAnEarlierSimilarFaceRegroupsIt() {
        FaceIdentityGroup original = FaceClusterer.cluster(
                List.of(sample("b", "1,0,0")), .15).get(0);
        FaceIdentityGroup regrouped = FaceClusterer.cluster(List.of(
                sample("a", ".99,.01,0"), sample("b", "1,0,0")), .15).get(0);

        assertEquals("ada", FaceGroupAssignmentResolver.personFor(regrouped,
                Map.of(original.id(), "ada")));
    }

    @Test public void conflictingHistoricalAnchorsDoNotGuessAGroupIdentity() {
        FaceObservation first = sample("a", "1,0,0");
        FaceObservation second = sample("b", ".99,.01,0");
        FaceIdentityGroup merged = FaceClusterer.cluster(List.of(first, second), .15).get(0);

        assertEquals("", FaceGroupAssignmentResolver.personFor(merged, Map.of(
                FaceClusterer.identityId(first), "ada",
                FaceClusterer.identityId(second), "ben")));
    }

    private static FaceObservation sample(String photo, String descriptor) {
        return new FaceObservation(photo, 0, 0, 0, 1, 1,
                -1, -1, -1, 0, 0, descriptor);
    }
}
