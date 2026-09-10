package com.keepers.photoorganiser;

import android.content.Context;

public final class AlbumApprovalInvalidator {
    private AlbumApprovalInvalidator() {}

    public static void invalidate(Context context) {
        new AlbumReviewSelectionStore(context).clear();
        new AlbumActionQueueStore(context).cancel();
        context.getSharedPreferences(KeepersAccessibilityService.PREFS, Context.MODE_PRIVATE)
                .edit().remove(KeepersAccessibilityService.ALBUM_ARMED_UNTIL)
                .remove(KeepersAccessibilityService.ALBUM_NAME)
                .remove(KeepersAccessibilityService.ALBUM_PHASE)
                .remove(KeepersAccessibilityService.ALBUM_PHASE_STARTED_AT)
                .remove(KeepersAccessibilityService.ALBUM_ADD_TO_RETRY_COUNT).apply();
    }
}
