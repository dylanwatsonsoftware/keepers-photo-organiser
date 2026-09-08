package com.keepers.photoorganiser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ReviewedPhotoStoreTest {
    @Test public void tracksAndReopensReviewedPhotos() {
        ReviewedPhotoStore store = new ReviewedPhotoStore(RuntimeEnvironment.getApplication());
        store.clear();
        store.mark("photo-a");
        assertTrue(store.contains("photo-a"));
        store.unmark("photo-a");
        assertFalse(store.contains("photo-a"));
    }
}
