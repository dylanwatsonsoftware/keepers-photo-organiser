package com.keepers.photoorganiser;

enum AlbumPickerStepDecision {
    SELECT_VISIBLE_ALBUM,
    OPEN_ALBUM_PICKER,
    RETRY_ADD_TO,
    WAIT;

    static final long ADD_TO_RETRY_AFTER_MS = 1_200;
    static final int MAX_ADD_TO_RETRIES = 2;

    static AlbumPickerStepDecision decide(boolean albumVisible, boolean pickerVisible,
            boolean addToVisible, long elapsedMillis, int addToRetryCount) {
        if (albumVisible) return SELECT_VISIBLE_ALBUM;
        if (pickerVisible) return OPEN_ALBUM_PICKER;
        if (addToVisible && elapsedMillis >= ADD_TO_RETRY_AFTER_MS
                && addToRetryCount < MAX_ADD_TO_RETRIES) return RETRY_ADD_TO;
        return WAIT;
    }
}
