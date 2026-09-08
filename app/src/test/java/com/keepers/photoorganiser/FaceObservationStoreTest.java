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
}
