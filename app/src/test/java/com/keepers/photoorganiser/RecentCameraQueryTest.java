package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class RecentCameraQueryTest {
    @Test
    public void mapsMediaIdToAnItemSpecificImageUri() {
        assertEquals(
                Uri.parse("content://media/external/images/media/42"),
                RecentCameraQuery.itemUri(42));
    }

    @Test
    public void mapsVideoIdToAnItemSpecificVideoUri() {
        assertEquals(
                Uri.parse("content://media/external/video/media/42"),
                RecentCameraQuery.videoItemUri(42));
    }

    @Test
    public void loadsAUsefulRecentReviewWindow() {
        assertEquals(60, RecentCameraQuery.LIMIT);
        assertEquals("DCIM/Camera/%", RecentCameraQuery.PATH_PATTERN);
    }
}
