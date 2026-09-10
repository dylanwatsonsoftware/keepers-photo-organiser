package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class HiddenPhotoStoreTest {
    @Test public void hidesRestoresAndClearsPhotos() {
        HiddenPhotoStore store = new HiddenPhotoStore(RuntimeEnvironment.getApplication());
        store.hide(Set.of("a", "b"));
        store.restore("a");
        assertEquals(Set.of("b"), store.load());
        store.clear();
        assertEquals(Set.of(), store.load());
    }
}
