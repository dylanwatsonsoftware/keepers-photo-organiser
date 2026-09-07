package com.keepers.photoorganiser;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.util.Log;
import android.widget.ImageView;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class AsyncThumbnailLoader implements AutoCloseable {
    interface Source {
        Bitmap load(Uri uri, int size) throws Exception;
    }

    private final Executor background;
    private final Executor main;
    private final Source source;
    private final ExecutorService ownedExecutor;

    AsyncThumbnailLoader(Executor background, Executor main, Source source) {
        this(background, main, source, null);
    }

    private AsyncThumbnailLoader(Executor background, Executor main, Source source,
            ExecutorService ownedExecutor) {
        this.background = background;
        this.main = main;
        this.source = source;
        this.ownedExecutor = ownedExecutor;
    }

    static AsyncThumbnailLoader forResolver(ContentResolver resolver) {
        ExecutorService workers = Executors.newFixedThreadPool(3, runnable -> {
            Thread thread = new Thread(runnable, "keepers-thumbnail");
            thread.setDaemon(true);
            return thread;
        });
        Handler handler = new Handler(Looper.getMainLooper());
        return new AsyncThumbnailLoader(workers, handler::post,
                (uri, size) -> resolver.loadThumbnail(uri, new Size(size, size), null), workers);
    }

    void load(ImageView target, Uri uri, int size) {
        load(target, uri, size, bitmap -> {});
    }

    void load(ImageView target, Uri uri, int size, Consumer<Bitmap> onLoaded) {
        target.setTag(uri);
        background.execute(() -> {
            try {
                Bitmap thumbnail = source.load(uri, size);
                main.execute(() -> {
                    if (!uri.equals(target.getTag())) return;
                    target.setImageBitmap(thumbnail);
                    onLoaded.accept(thumbnail);
                });
            } catch (Exception unavailablePhoto) {
                Log.w("KeepersThumbnail", "Unable to load " + uri, unavailablePhoto);
                main.execute(() -> onLoaded.accept(null));
            }
        });
    }

    void loadProgressive(ImageView target, Uri uri, int previewSize, int fullSize,
            Consumer<Bitmap> onLoaded) {
        target.setTag(uri);
        background.execute(() -> {
            deliver(target, uri, loadSafely(uri, previewSize), false, onLoaded);
            if (fullSize > previewSize) {
                deliver(target, uri, loadSafely(uri, fullSize), true, onLoaded);
            }
        });
    }

    private Bitmap loadSafely(Uri uri, int size) {
        try {
            return source.load(uri, size);
        } catch (Exception unavailablePhoto) {
            Log.w("KeepersThumbnail", "Unable to load " + uri + " at " + size + "px",
                    unavailablePhoto);
            return null;
        }
    }

    private void deliver(ImageView target, Uri uri, Bitmap bitmap, boolean crossfade,
            Consumer<Bitmap> onLoaded) {
        main.execute(() -> {
            if (!uri.equals(target.getTag())) return;
            if (bitmap != null) {
                if (crossfade) target.setAlpha(0.82f);
                target.setImageBitmap(bitmap);
                if (crossfade) target.animate().alpha(1f).setDuration(180).start();
            }
            onLoaded.accept(bitmap);
        });
    }

    @Override public void close() {
        if (ownedExecutor != null) ownedExecutor.shutdownNow();
    }
}
