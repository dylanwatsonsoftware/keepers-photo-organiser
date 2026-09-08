package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class FaceObservationStoreTest {
    @Test public void persistsNormalizedFaceEvidenceForLaterIdentityLearning() {
        FaceObservationStore store = new FaceObservationStore(RuntimeEnvironment.getApplication());
        FaceObservation face = new FaceObservation("photo-a", 0, 0.1, 0.2, 0.4, 0.6,
                0.8, 0.9, 0.95, -4, 2);

        store.save("photo-a", List.of(face));

        assertEquals(List.of(face), store.load("photo-a"));
        assertEquals(1, store.observationCount());
    }

    @Test public void preservesTheDescriptorUsedToLearnRecurringFaces() {
        FaceObservationStore store = new FaceObservationStore(RuntimeEnvironment.getApplication());
        FaceObservation face = new FaceObservation("photo", 0, .1, .2, .3, .4,
                .5, .6, .7, 1, 2, "0.1,-0.2,0.3");

        store.save("photo", List.of(face));

        assertEquals("0.1,-0.2,0.3", store.load("photo").get(0).descriptor());
    }

    @Test public void loadsAllObservationsForRecurringFaceDiscovery() {
        FaceObservationStore store = new FaceObservationStore(RuntimeEnvironment.getApplication());
        store.save("b", List.of(new FaceObservation("b", 0, 0, 0, 1, 1,
                -1, -1, -1, 0, 0, "0,1")));
        store.save("a", List.of(new FaceObservation("a", 0, 0, 0, 1, 1,
                -1, -1, -1, 0, 0, "1,0")));

        assertEquals(List.of("a", "b"), store.loadAll().stream()
                .map(FaceObservation::photoId).toList());
    }
}
