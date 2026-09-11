package com.keepers.photoorganiser;

import android.net.Uri;

public record RecentPhoto(Uri uri, long takenAtMillis, MediaType mediaType,
        long durationMillis) {
    public RecentPhoto(Uri uri, long takenAtMillis) {
        this(uri, takenAtMillis, MediaType.PHOTO, 0);
    }
}
