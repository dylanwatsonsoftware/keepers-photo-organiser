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

    private static FaceObservation face(String photo, int index, String descriptor) {
        return new FaceObservation(photo, index, 0, 0, 1, 1,
                .5, .8, .8, 0, 0, descriptor);
    }
}
