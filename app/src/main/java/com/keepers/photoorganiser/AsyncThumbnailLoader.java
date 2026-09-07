package com.keepers.photoorganiser;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
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
        background.execute(() -> {
            try {
                Bitmap thumbnail = source.load(uri, size);
                main.execute(() -> {
                    target.setImageBitmap(thumbnail);
                    onLoaded.accept(thumbnail);
                });
            } catch (Exception unavailablePhoto) {
                main.execute(() -> onLoaded.accept(null));
            }
        });
    }

    @Override public void close() {
        if (ownedExecutor != null) ownedExecutor.shutdownNow();
    }
}
