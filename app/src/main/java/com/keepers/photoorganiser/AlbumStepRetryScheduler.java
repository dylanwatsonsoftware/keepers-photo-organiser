package com.keepers.photoorganiser;

import android.os.Handler;

final class AlbumStepRetryScheduler {
    static final long RETRY_DELAY_MS = 600;
    private final Handler handler;
    private final Runnable retry;
    private final Runnable dispatch;
    private boolean scheduled;

    AlbumStepRetryScheduler(Handler handler, Runnable retry) {
        this.handler = handler;
        this.retry = retry;
        dispatch = () -> {
            scheduled = false;
            this.retry.run();
        };
    }

    void ensureScheduled(boolean armed) {
        if (!armed) {
            cancel();
            return;
        }
        if (scheduled) return;
        scheduled = true;
        handler.postDelayed(dispatch, RETRY_DELAY_MS);
    }

    void cancel() {
        if (!scheduled) return;
        handler.removeCallbacks(dispatch);
        scheduled = false;
    }
}
