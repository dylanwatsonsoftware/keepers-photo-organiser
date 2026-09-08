package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AlbumAutomationCoordinatorTest {
    @Test public void approvedAlbumActionStartsDirectlyAtAddTo() {
        Context context = RuntimeEnvironment.getApplication();

        AlbumAutomationCoordinator.arm(context,
                new AlbumAction("content://photos/a", "Ada", "Ada Photos"));

        assertEquals(KeepersAccessibilityService.PHASE_ADD_TO,
                context.getSharedPreferences(KeepersAccessibilityService.PREFS,
                        Context.MODE_PRIVATE).getInt(
                                KeepersAccessibilityService.ALBUM_PHASE, -1));
    }
}
