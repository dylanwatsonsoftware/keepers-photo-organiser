package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MediaTypeTest {
    @Test public void recognisesVideoMimeTypesWithoutTreatingUnknownContentAsVideo() {
        assertEquals(MediaType.VIDEO, MediaType.fromMimeType("video/mp4"));
        assertEquals(MediaType.PHOTO, MediaType.fromMimeType("image/jpeg"));
        assertEquals(MediaType.PHOTO, MediaType.fromMimeType(null));
    }

    @Test public void recentVideoRetainsItsDurationAndMediaType() {
        RecentPhoto video = new RecentPhoto(Uri.parse("content://media/video/7"),
                1234, MediaType.VIDEO, 65_000);

        assertEquals(MediaType.VIDEO, video.mediaType());
        assertEquals(65_000, video.durationMillis());
        assertEquals("1:05", MediaDuration.format(video.durationMillis()));
    }
}
