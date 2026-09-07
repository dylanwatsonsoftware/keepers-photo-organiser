package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.ImageView;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;
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

    @Test public void progressiveLoadShowsPreviewThenScreenSizedImage() {
        Context context = RuntimeEnvironment.getApplication();
        ImageView target = new ImageView(context);
        List<Integer> requestedSizes = new ArrayList<>();
        AsyncThumbnailLoader loader = new AsyncThumbnailLoader(Runnable::run, Runnable::run,
                (uri, size) -> {
                    requestedSizes.add(size);
                    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                });

        loader.loadProgressive(target, Uri.parse("content://media/photo/1"), 480, 2400,
                bitmap -> {});

        assertEquals(List.of(480, 2400), requestedSizes);
        assertEquals(2400, ((android.graphics.drawable.BitmapDrawable)
                target.getDrawable()).getBitmap().getWidth());
    }

    @Test public void progressiveLoadUsesOriginalSourceForFullStage() {
        Context context = RuntimeEnvironment.getApplication();
        ImageView target = new ImageView(context);
        List<String> requests = new ArrayList<>();
        AsyncThumbnailLoader loader = new AsyncThumbnailLoader(Runnable::run, Runnable::run,
                (uri, size) -> {
                    requests.add("thumbnail:" + size);
                    return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
                }, (uri, size) -> {
                    requests.add("original:" + size);
                    return Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888);
                });

        loader.loadProgressive(target, Uri.parse("content://media/photo/1"), 480, 2400,
                bitmap -> {});

        assertEquals(List.of("thumbnail:480", "original:2400"), requests);
        assertEquals(20, ((android.graphics.drawable.BitmapDrawable)
                target.getDrawable()).getBitmap().getWidth());
    }
}
