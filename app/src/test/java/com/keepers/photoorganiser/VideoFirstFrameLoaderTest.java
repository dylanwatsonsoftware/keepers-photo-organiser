package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.ImageView;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class VideoFirstFrameLoaderTest {
    @Test public void extractsOnceAndReusesTheCachedFirstFrame() {
        Context context = RuntimeEnvironment.getApplication();
        AtomicInteger extractions = new AtomicInteger();
        VideoFirstFrameLoader loader = new VideoFirstFrameLoader(
                Runnable::run, Runnable::run, (uri, size) -> {
                    extractions.incrementAndGet();
                    return Bitmap.createBitmap(size, size / 2, Bitmap.Config.ARGB_8888);
                });
        Uri video = Uri.parse("content://media/video/media/first-frame");
        ImageView first = new ImageView(context);
        ImageView second = new ImageView(context);

        loader.load(first, video, 640, bitmap -> {});
        loader.load(second, video, 640, bitmap -> {});

        assertEquals(1, extractions.get());
        assertNotNull(first.getDrawable());
        assertNotNull(second.getDrawable());
    }

    @Test public void staleExtractionCannotReplaceANewerVideoPoster() {
        Context context = RuntimeEnvironment.getApplication();
        ImageView target = new ImageView(context);
        VideoFirstFrameLoader loader = new VideoFirstFrameLoader(
                Runnable::run, Runnable::run,
                (uri, size) -> Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888));
        Uri first = Uri.parse("content://video/first");
        Uri second = Uri.parse("content://video/second");

        loader.load(target, first, 640, bitmap -> {});
        loader.load(target, second, 640, bitmap -> {});

        assertEquals(second, target.getTag());
    }
}
