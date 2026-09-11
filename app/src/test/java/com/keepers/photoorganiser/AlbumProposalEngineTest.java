package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class AlbumProposalEngineTest {
    @Test public void proposesAlbumsForEveryAddedPersonSeenInKeeperPhotos() {
        FaceIdentityGroup adaFaces = group("ada-group", "keeper-a", "other");
        FaceIdentityGroup benFaces = group("ben-group", "keeper-a");
        List<TrackedPerson> people = List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", true),
                new TrackedPerson("ben", "Ben", "Ben Photos", false));

        List<AlbumAssignment> result = AlbumProposalEngine.propose(Set.of("keeper-a"),
                List.of(adaFaces, benFaces), Map.of("ada-group", "ada", "ben-group", "ben"),
                people);

        assertEquals(List.of(
                new AlbumAssignment("keeper-a", "ada", "Ada", "Ada Photos", true),
                new AlbumAssignment("keeper-a", "ben", "Ben", "Ben Photos", true)), result);
    }

    @Test public void oneGroupCanProposeTheSameChildAcrossSeveralKeepers() {
        List<AlbumAssignment> result = AlbumProposalEngine.propose(Set.of("one", "two"),
                List.of(group("ada-group", "one", "two")), Map.of("ada-group", "ada"),
                List.of(new TrackedPerson("ada", "Ada", "Ada Photos", true)));

        assertEquals(List.of("one", "two"), result.stream().map(AlbumAssignment::photoId).toList());
    }

    @Test public void ignoresUnmappedFacesAndPeopleWithoutAlbums() {
        assertEquals(List.of(), AlbumProposalEngine.propose(Set.of("keeper"),
                List.of(group("unknown", "keeper")), Map.of(),
                List.of(new TrackedPerson("ada", "Ada", "", true))));
    }

    @Test public void everyAddedPersonWithAnAlbumIsEligibleWithoutATrackToggle() {
        FaceIdentityGroup ada = group("ada-group", "keeper");

        List<AlbumAssignment> result = AlbumProposalEngine.propose(Set.of("keeper"),
                List.of(ada), Map.of("ada-group", "ada"),
                List.of(new TrackedPerson("ada", "Ada", "Ada Photos", false)));

        assertEquals(1, result.size());
        assertEquals("ada", result.get(0).personId());
    }

    @Test public void perFaceCorrectionOverridesItsGroupForAlbumSafety() {
        FaceIdentityGroup group = group("ada-group", "keeper-a");
        List<TrackedPerson> people = List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", true),
                new TrackedPerson("ben", "Ben", "Ben Photos", true));

        List<AlbumAssignment> result = AlbumProposalEngine.propose(Set.of("keeper-a"),
                List.of(group), Map.of("ada-group", "ada"),
                Map.of("keeper-a#0", "ben"), people);

        assertEquals(List.of(new AlbumAssignment("keeper-a", "ben", "Ben",
                "Ben Photos", true)), result);
    }

    @Test public void explicitlyIgnoredFaceNeverCreatesAnAlbumProposal() {
        assertEquals(List.of(), AlbumProposalEngine.propose(Set.of("keeper-a"),
                List.of(group("ada-group", "keeper-a")), Map.of("ada-group", "ada"),
                Map.of("keeper-a#0", FaceCorrectionStore.IGNORE),
                List.of(new TrackedPerson("ada", "Ada", "Ada Photos", true))));
    }

    @Test public void aCorrectionDoesNotAutoApproveAnUnmappedSimilarFace() {
        FaceIdentityGroup taught = group("old-group", "old");
        FaceIdentityGroup newFace = new FaceIdentityGroup("new-group", List.of(
                new FaceObservation("keeper-a", 0, 0, 0, 1, 1,
                        -1, -1, -1, 0, 0, ".99,.01")));
        List<AlbumAssignment> result = AlbumProposalEngine.propose(Set.of("keeper-a"),
                List.of(taught, newFace), Map.of(), Map.of("old#0", "ada"),
                List.of(new TrackedPerson("ada", "Ada", "Ada Photos", true)));

        assertEquals(List.of(), result);
    }

    @Test public void aConfirmedGroupDoesNotAutoApproveFutureAlbumSuggestions() {
        FaceIdentityGroup confirmed = group("confirmed-ada", "older-photo");
        FaceIdentityGroup newFace = new FaceIdentityGroup("unconfirmed-new", List.of(
                new FaceObservation("keeper-a", 0, 0, 0, 1, 1,
                        -1, -1, -1, 0, 0, ".99,.01")));

        List<AlbumAssignment> result = AlbumProposalEngine.propose(Set.of("keeper-a"),
                List.of(confirmed, newFace), Map.of("confirmed-ada", "ada"), Map.of(),
                List.of(new TrackedPerson("ada", "Ada", "Ada Photos", true)));

        assertEquals(List.of(), result);
    }

    private static FaceIdentityGroup group(String id, String... photos) {
        return new FaceIdentityGroup(id, java.util.Arrays.stream(photos).map(photo ->
                new FaceObservation(photo, 0, 0, 0, 1, 1, -1, -1, -1, 0, 0, "1,0"))
                .toList());
    }
}
