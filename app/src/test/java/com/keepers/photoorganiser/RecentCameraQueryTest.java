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
    public void limitsTheExperimentToFiveCameraImages() {
        assertEquals(5, RecentCameraQuery.LIMIT);
        assertEquals("DCIM/Camera/%", RecentCameraQuery.PATH_PATTERN);
    }
}
