package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AlbumReviewSelectionStoreTest {
    @Test public void distinguishesUntouchedSuggestionsFromAnExplicitlyEmptyReview() {
        AlbumReviewSelectionStore store = new AlbumReviewSelectionStore(
                RuntimeEnvironment.getApplication());
        assertTrue(!store.hasReview());

        store.save(Set.of());

        assertTrue(store.hasReview());
        assertEquals(Set.of(), store.load());
    }
}
