package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AlbumAutomationOverlayTest {
    @Test public void overlayCannotTakeFocusOrInterceptGooglePhotosTouches() {
        WindowManager.LayoutParams params = AlbumAutomationOverlay.layoutParams();

        assertEquals(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, params.type);
        assertTrue((params.flags & WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0);
        assertTrue((params.flags & WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0);
    }

    @Test public void graphicShowsQueueProgressAndCurrentDestination() {
        View view = AlbumAutomationOverlay.createView(RuntimeEnvironment.getApplication(),
                new AlbumAutomationProgress("Keepers · 3 of 10",
                        "Finding album · Ada Photos", 3, 10));

        assertEquals("Keepers · 3 of 10", ((TextView) view.findViewWithTag(
                "album_overlay_title")).getText().toString());
        assertEquals("Finding album · Ada Photos", ((TextView) view.findViewWithTag(
                "album_overlay_detail")).getText().toString());
        ProgressBar bar = view.findViewWithTag("album_overlay_progress");
        assertEquals(10, bar.getMax());
        assertEquals(3, bar.getProgress());
    }

    @Test public void overlayRenderingFailureIsContained() {
        assertFalse(AlbumAutomationOverlay.performSafely(() -> {
            throw new SecurityException("overlay unavailable");
        }));
    }
}
