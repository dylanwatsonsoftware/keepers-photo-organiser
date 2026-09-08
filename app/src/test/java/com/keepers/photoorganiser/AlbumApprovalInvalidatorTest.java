package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AlbumApprovalInvalidatorTest {
    @Test public void upstreamChangeClearsReviewQueueAndArmedGooglePhotosAction() {
        android.content.Context context = RuntimeEnvironment.getApplication();
        new AlbumReviewSelectionStore(context).save(Set.of("photo\nperson"));
        new AlbumActionQueueStore(context).begin(List.of(
                new AlbumAction("photo", "Ada", "Ada Photos")));
        context.getSharedPreferences(KeepersAccessibilityService.PREFS, 0).edit()
                .putLong(KeepersAccessibilityService.ALBUM_ARMED_UNTIL, Long.MAX_VALUE)
                .putString(KeepersAccessibilityService.ALBUM_NAME, "Ada Photos")
                .putInt(KeepersAccessibilityService.ALBUM_PHASE, 1).apply();

        AlbumApprovalInvalidator.invalidate(context);

        assertFalse(new AlbumReviewSelectionStore(context).hasReview());
        assertFalse(new AlbumActionQueueStore(context).isActive());
        assertEquals(0, context.getSharedPreferences(KeepersAccessibilityService.PREFS, 0)
                .getLong(KeepersAccessibilityService.ALBUM_ARMED_UNTIL, 0));
        assertEquals("", context.getSharedPreferences(KeepersAccessibilityService.PREFS, 0)
                .getString(KeepersAccessibilityService.ALBUM_NAME, ""));
    }
}
