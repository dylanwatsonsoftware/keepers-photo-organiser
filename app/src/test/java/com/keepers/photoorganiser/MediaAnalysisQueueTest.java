package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.net.Uri;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MediaAnalysisQueueTest {
    @Test public void videosWaitUntilEveryStillPhotoInTheSetHasFinished() {
        RecentPhoto videoOne = video("content://video/1");
        RecentPhoto videoTwo = video("content://video/2");
        MediaAnalysisQueue queue = new MediaAnalysisQueue();
        queue.add(List.of(videoOne, photo("content://photo/1"), videoTwo,
                photo("content://photo/2")));

        assertNull(queue.pollVideo());
        queue.photoCompleted();
        assertNull(queue.pollVideo());
        queue.photoCompleted();

        assertEquals(videoOne, queue.pollVideo());
        assertEquals(videoTwo, queue.pollVideo());
        assertNull(queue.pollVideo());
    }

    private static RecentPhoto photo(String uri) {
        return new RecentPhoto(Uri.parse(uri), 1);
    }

    private static RecentPhoto video(String uri) {
        return new RecentPhoto(Uri.parse(uri), 1, MediaType.VIDEO, 10_000);
    }
}
