package com.keepers.photoorganiser;

import android.content.Context;
import android.os.PowerManager;

public final class AlbumAutomationWakeLock {
    private static final long ACTION_TIMEOUT_MS = 5 * 60_000L;
    private static PowerManager.WakeLock wakeLock;

    private AlbumAutomationWakeLock() {}

    @SuppressWarnings("deprecation")
    public static synchronized void acquire(Context context) {
        if (wakeLock == null) {
            PowerManager power = (PowerManager) context.getApplicationContext()
                    .getSystemService(Context.POWER_SERVICE);
            wakeLock = power.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
                    "Keepers:AlbumAutomation");
            wakeLock.setReferenceCounted(false);
        }
        if (wakeLock.isHeld()) wakeLock.release();
        wakeLock.acquire(ACTION_TIMEOUT_MS);
    }

    public static synchronized void release() {
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }

    static synchronized boolean isHeld() {
        return wakeLock != null && wakeLock.isHeld();
    }
}
