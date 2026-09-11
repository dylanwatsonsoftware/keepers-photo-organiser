package com.keepers.photoorganiser;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public final class AlbumAutomationCoordinator {
    private AlbumAutomationCoordinator() {}
    public static Intent arm(Context context, AlbumAction action) {
        context.getSharedPreferences(KeepersAccessibilityService.PREFS, Context.MODE_PRIVATE).edit()
                .putLong(KeepersAccessibilityService.ALBUM_ARMED_UNTIL,
                        System.currentTimeMillis() + 120_000)
                .putString(KeepersAccessibilityService.ALBUM_NAME, action.albumName())
                .putInt(KeepersAccessibilityService.ALBUM_PHASE,
                        KeepersAccessibilityService.PHASE_ADD_TO)
                .putLong(KeepersAccessibilityService.ALBUM_PHASE_STARTED_AT,
                        System.currentTimeMillis())
                .putInt(KeepersAccessibilityService.ALBUM_ADD_TO_RETRY_COUNT, 0).apply();
        Uri media = Uri.parse(action.photoId());
        return GooglePhotosIntentFactory.openExisting(media,
                MediaType.from(media, context.getContentResolver().getType(media)));
    }

    public static void disarm(Context context) {
        context.getSharedPreferences(KeepersAccessibilityService.PREFS, Context.MODE_PRIVATE).edit()
                .remove(KeepersAccessibilityService.ALBUM_ARMED_UNTIL)
                .remove(KeepersAccessibilityService.ALBUM_NAME)
                .remove(KeepersAccessibilityService.ALBUM_PHASE)
                .remove(KeepersAccessibilityService.ALBUM_PHASE_STARTED_AT)
                .remove(KeepersAccessibilityService.ALBUM_ADD_TO_RETRY_COUNT).apply();
    }
}
