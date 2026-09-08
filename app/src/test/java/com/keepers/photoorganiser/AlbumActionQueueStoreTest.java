package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AlbumActionQueueStoreTest {
    @Test public void approvedQueueAdvancesInOrderAndStopsAfterTheLastAction() {
        AlbumActionQueueStore store = new AlbumActionQueueStore(RuntimeEnvironment.getApplication());
        AlbumAction first = new AlbumAction("photo-a", "Ada", "Ada Photos");
        AlbumAction second = new AlbumAction("photo-b", "Ben", "Ben Photos");

        store.begin(List.of(first, second));

        assertTrue(store.isActive());
        assertEquals(first, store.current());
        assertEquals(second, store.completeCurrent());
        assertEquals(second, store.current());
        assertEquals(null, store.completeCurrent());
        assertFalse(store.isActive());
    }
}
