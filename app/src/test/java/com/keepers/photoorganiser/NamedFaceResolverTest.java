package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class NamedFaceResolverTest {
    @Test public void mapsAssignedAndCorrectedPeopleToTheirPhotos() {
        FaceObservation first = face("photo-a", 0, "1,0");
        FaceObservation second = face("photo-b", 0, "1,0");
        FaceObservation corrected = face("photo-b", 1, "0,1");
        FaceIdentityGroup group = new FaceIdentityGroup("group-children",
                List.of(first, second));

        assertEquals(Map.of(
                "photo-a", Set.of("child-a"),
                "photo-b", Set.of("child-a", "child-b")), NamedFaceResolver.resolve(
                List.of(group, new FaceIdentityGroup("other", List.of(corrected))),
                Map.of("group-children", "child-a"),
                Map.of(FaceCorrectionStore.key(corrected), "child-b")));
    }

    @Test public void ignoresUnknownFacesFromUnassignedGroups() {
        FaceIdentityGroup firstPerson = new FaceIdentityGroup("face-group-a", List.of(
                face("photo-a", 0, "1,0"), face("photo-b", 0, "1,0")));
        FaceIdentityGroup secondPerson = new FaceIdentityGroup("face-group-b", List.of(
                face("photo-c", 0, "0,1")));

        assertEquals(Map.of(),
                NamedFaceResolver.resolve(List.of(firstPerson, secondPerson), Map.of(), Map.of()));
    }

    @Test public void includesOnlyConfirmedPersonWhenAnotherFaceIsUnknown() {
        FaceObservation confirmed = face("photo-a", 0, "1,0");
        FaceObservation unknown = face("photo-a", 1, "0,1");

        assertEquals(Map.of("photo-a", Set.of("child-a")), NamedFaceResolver.resolve(
                List.of(new FaceIdentityGroup("unassigned", List.of(confirmed, unknown))),
                Map.of(), Map.of(FaceCorrectionStore.key(confirmed), "child-a")));
    }

    private static FaceObservation face(String photo, int index, String descriptor) {
        return new FaceObservation(photo, index, 0, 0, 1, 1,
                .5, .8, .8, 0, 0, descriptor);
    }
}
