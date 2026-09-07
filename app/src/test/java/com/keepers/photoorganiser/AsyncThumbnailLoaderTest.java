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
public class AsyncThumbnailLoaderTest {
    @Test public void requestsOnlyATileSizedThumbnail() {
        Context context = RuntimeEnvironment.getApplication();
        ImageView target = new ImageView(context);
        AtomicInteger requestedSize = new AtomicInteger();
        AsyncThumbnailLoader loader = new AsyncThumbnailLoader(Runnable::run, Runnable::run,
                (uri, size) -> {
                    requestedSize.set(size);
                    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                });

        loader.load(target, Uri.parse("content://media/photo/1"), 360);

        assertEquals(360, requestedSize.get());
        assertNotNull(target.getDrawable());
    }

    @Test public void unavailableThumbnailLeavesPlaceholderWithoutCrashing() {
        Context context = RuntimeEnvironment.getApplication();
        ImageView target = new ImageView(context);
        AsyncThumbnailLoader loader = new AsyncThumbnailLoader(Runnable::run, Runnable::run,
                (uri, size) -> { throw new IllegalStateException("gone"); });

        loader.load(target, Uri.parse("content://media/photo/gone"), 360);

        assertEquals(null, target.getDrawable());
    }
}
