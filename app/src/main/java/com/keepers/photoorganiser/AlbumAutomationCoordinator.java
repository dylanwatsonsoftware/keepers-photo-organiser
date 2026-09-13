package com.keepers.photoorganiser;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public final class AlbumAutomationCoordinator {
    private AlbumAutomationCoordinator() {}
    public static Intent arm(Context context, AlbumAction action) {
        AlbumAutomationWakeLock.acquire(context);
        Uri media = Uri.parse(action.photoId());
        MediaType mediaType = MediaType.from(media,
                context.getContentResolver().getType(media));
        context.getSharedPreferences(KeepersAccessibilityService.PREFS, Context.MODE_PRIVATE).edit()
                .putLong(KeepersAccessibilityService.ALBUM_ARMED_UNTIL,
                        System.currentTimeMillis() + 120_000)
                .putString(KeepersAccessibilityService.ALBUM_NAME, action.albumName())
                .putInt(KeepersAccessibilityService.ALBUM_PHASE,
                        mediaType == MediaType.VIDEO
                                ? KeepersAccessibilityService.PHASE_REVEAL_VIDEO_CONTROLS
                                : KeepersAccessibilityService.PHASE_ADD_TO)
                .putLong(KeepersAccessibilityService.ALBUM_PHASE_STARTED_AT,
                        System.currentTimeMillis())
                .putInt(KeepersAccessibilityService.ALBUM_ADD_TO_RETRY_COUNT, 0).apply();
        return GooglePhotosIntentFactory.openExisting(media, mediaType);
    }

    public static void disarm(Context context) {
        AlbumAutomationWakeLock.release();
        context.getSharedPreferences(KeepersAccessibilityService.PREFS, Context.MODE_PRIVATE).edit()
                .remove(KeepersAccessibilityService.ALBUM_ARMED_UNTIL)
                .remove(KeepersAccessibilityService.ALBUM_NAME)
                .remove(KeepersAccessibilityService.ALBUM_PHASE)
                .remove(KeepersAccessibilityService.ALBUM_PHASE_STARTED_AT)
                .remove(KeepersAccessibilityService.ALBUM_ADD_TO_RETRY_COUNT).apply();
    }
}
