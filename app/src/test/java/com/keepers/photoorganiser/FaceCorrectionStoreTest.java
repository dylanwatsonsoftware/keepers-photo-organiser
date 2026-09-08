package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class FaceCorrectionStoreTest {
    @Test public void savesPerFaceReassignmentsAndExplicitIgnores() {
        FaceCorrectionStore store = new FaceCorrectionStore(RuntimeEnvironment.getApplication());
        store.save(Map.of("photo-a#0", "person-2", "photo-a#1", FaceCorrectionStore.IGNORE));

        assertEquals(Map.of("photo-a#0", "person-2", "photo-a#1", FaceCorrectionStore.IGNORE),
                store.load());
    }
}
