package com.keepers.photoorganiser;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class VideoFirstFrameLoader implements AutoCloseable {
    interface Source {
        Bitmap load(Uri uri, int maximumPixels) throws Exception;
    }

    private static final int CACHE_KILOBYTES = 24 * 1024;
    private final Executor background;
    private final Executor main;
    private final Source source;
    private final ExecutorService ownedExecutor;
    private final LruCache<String, Bitmap> cache = new LruCache<>(CACHE_KILOBYTES) {
        @Override protected int sizeOf(String key, Bitmap bitmap) {
            return Math.max(1, bitmap.getAllocationByteCount() / 1024);
        }
    };

    public VideoFirstFrameLoader(Context context) {
        ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "keepers-video-poster");
            thread.setDaemon(true);
            return thread;
        });
        Handler handler = new Handler(Looper.getMainLooper());
        Context application = context.getApplicationContext();
        background = worker;
        main = handler::post;
        source = (uri, size) -> extract(application, uri, size);
        ownedExecutor = worker;
    }

    VideoFirstFrameLoader(Executor background, Executor main, Source source) {
        this.background = background;
        this.main = main;
        this.source = source;
        ownedExecutor = null;
    }

    public void load(ImageView target, Uri uri, int maximumPixels,
            Consumer<Bitmap> onLoaded) {
        target.setTag(uri);
        String key = uri + "|" + maximumPixels;
        Bitmap cached = cache.get(key);
        if (cached != null) {
            deliver(target, uri, cached, onLoaded);
            return;
        }
        background.execute(() -> {
            Bitmap firstFrame = null;
            try {
                firstFrame = source.load(uri, maximumPixels);
                if (firstFrame != null) cache.put(key, firstFrame);
            } catch (Exception unavailableVideo) {
                Log.w("KeepersVideoPoster", "Unable to load first frame for " + uri,
                        unavailableVideo);
            }
            deliver(target, uri, firstFrame, onLoaded);
        });
    }

    private void deliver(ImageView target, Uri uri, Bitmap bitmap,
            Consumer<Bitmap> onLoaded) {
        main.execute(() -> {
            if (!uri.equals(target.getTag())) return;
            if (bitmap != null) target.setImageBitmap(bitmap);
            onLoaded.accept(bitmap);
        });
    }

    private static Bitmap extract(Context context, Uri uri, int maximumPixels) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            int width = metadataInt(retriever, MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            int height = metadataInt(retriever, MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            int[] target = targetSize(width, height, maximumPixels);
            return retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST,
                    target[0], target[1]);
        } finally {
            try { retriever.release(); } catch (Exception ignored) {}
        }
    }

    private static int[] targetSize(int width, int height, int maximumPixels) {
        if (width <= 0 || height <= 0)
            return new int[]{Math.max(1, maximumPixels), Math.max(1, maximumPixels)};
        double scale = Math.min(1, maximumPixels / (double) Math.max(width, height));
        return new int[]{Math.max(1, (int) Math.round(width * scale)),
                Math.max(1, (int) Math.round(height * scale))};
    }

    private static int metadataInt(MediaMetadataRetriever retriever, int key) {
        try {
            String value = retriever.extractMetadata(key);
            return value == null ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException invalid) {
            return 0;
        }
    }

    @Override public void close() {
        if (ownedExecutor != null) ownedExecutor.shutdownNow();
    }
}
