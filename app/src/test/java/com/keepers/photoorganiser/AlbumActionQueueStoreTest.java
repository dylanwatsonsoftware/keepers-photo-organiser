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
        android.content.Context context = RuntimeEnvironment.getApplication();
        AlbumCompletionStore completions = new AlbumCompletionStore(context);
        completions.clear();
        AlbumActionQueueStore store = new AlbumActionQueueStore(context);
        AlbumAction first = new AlbumAction("photo-a", "Ada", "Ada Photos");
        AlbumAction second = new AlbumAction("photo-b", "Ben", "Ben Photos");

        store.begin(List.of(first, second));

        assertTrue(store.isActive());
        assertEquals(2, store.totalCount());
        assertEquals(0, store.completedCount());
        assertEquals(first, store.current());
        assertEquals(second, store.completeCurrent());
        assertEquals(1, store.completedCount());
        assertTrue(completions.contains("photo-a", "Ada Photos"));
        assertTrue(new ReviewedPhotoStore(context).contains("photo-a"));
        assertEquals(second, store.current());
        assertEquals(null, store.completeCurrent());
        assertTrue(completions.contains("photo-b", "Ben Photos"));
        assertTrue(new ReviewedPhotoStore(context).contains("photo-b"));
        assertFalse(store.isActive());
        assertEquals(2, store.completedCount());
    }

    @Test public void severalAlbumsForOnePhotoCompleteBeforeThePhotoIsReviewed() {
        android.content.Context context = RuntimeEnvironment.getApplication();
        new ReviewedPhotoStore(context).clear();
        AlbumActionQueueStore store = new AlbumActionQueueStore(context);
        store.begin(List.of(
                new AlbumAction("photo-a", "Ada", "Ada Photos"),
                new AlbumAction("photo-a", "Family", "Family Photos")));

        store.completeCurrent();
        assertFalse(new ReviewedPhotoStore(context).contains("photo-a"));

        store.completeCurrent();
        assertTrue(new ReviewedPhotoStore(context).contains("photo-a"));
    }
}
