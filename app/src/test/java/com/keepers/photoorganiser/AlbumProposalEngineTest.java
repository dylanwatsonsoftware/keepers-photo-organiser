package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class AlbumProposalEngineTest {
    @Test public void proposesAlbumsOnlyForTrackedPeopleSeenInKeeperPhotos() {
        FaceIdentityGroup adaFaces = group("ada-group", "keeper-a", "other");
        FaceIdentityGroup benFaces = group("ben-group", "keeper-a");
        List<TrackedPerson> people = List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", true),
                new TrackedPerson("ben", "Ben", "Ben Photos", false));

        List<AlbumAssignment> result = AlbumProposalEngine.propose(Set.of("keeper-a"),
                List.of(adaFaces, benFaces), Map.of("ada-group", "ada", "ben-group", "ben"),
                people);

        assertEquals(List.of(new AlbumAssignment("keeper-a", "ada", "Ada",
                "Ada Photos", true)), result);
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

    private static FaceIdentityGroup group(String id, String... photos) {
        return new FaceIdentityGroup(id, java.util.Arrays.stream(photos).map(photo ->
                new FaceObservation(photo, 0, 0, 0, 1, 1, -1, -1, -1, 0, 0, "1,0"))
                .toList());
    }
}
