package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.net.Uri;
import java.util.Set;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class KeeperSelectionStoreTest {
    @Test public void togglePersistsExactPhotoUri() {
        Context context = RuntimeEnvironment.getApplication();
        KeeperSelectionStore store = new KeeperSelectionStore(context);
        Uri photo = Uri.parse("content://media/external/images/media/42");

        assertTrue(store.toggle(photo));
        assertEquals(Set.of(photo.toString()), new KeeperSelectionStore(context).load());
        assertFalse(store.toggle(photo));
        assertTrue(new KeeperSelectionStore(context).load().isEmpty());
    }

    @Test public void clearRemovesAllSelections() {
        Context context = RuntimeEnvironment.getApplication();
        KeeperSelectionStore store = new KeeperSelectionStore(context);
        store.toggle(Uri.parse("content://media/photo/1"));
        store.toggle(Uri.parse("content://media/photo/2"));

        store.clear();

        assertTrue(store.load().isEmpty());
    }

    @Test public void changingAKeeperInvalidatesReviewedAndArmedAlbumChanges() {
        Context context = RuntimeEnvironment.getApplication();
        KeeperSelectionStore keepers = new KeeperSelectionStore(context);
        AlbumReviewSelectionStore review = new AlbumReviewSelectionStore(context);
        review.save(Set.of("content://media/photo/1\nperson-1"));
        AlbumActionQueueStore queue = new AlbumActionQueueStore(context);
        queue.begin(List.of(new AlbumAction("content://media/photo/1", "Ada", "Ada Photos")));
        context.getSharedPreferences(KeepersAccessibilityService.PREFS, Context.MODE_PRIVATE)
                .edit().putLong(KeepersAccessibilityService.ALBUM_ARMED_UNTIL, Long.MAX_VALUE)
                .apply();

        keepers.toggle(Uri.parse("content://media/photo/1"));

        assertFalse(review.hasReview());
        assertFalse(queue.isActive());
        assertEquals(0, context.getSharedPreferences(KeepersAccessibilityService.PREFS,
                Context.MODE_PRIVATE).getLong(
                        KeepersAccessibilityService.ALBUM_ARMED_UNTIL, 0));
    }
}
