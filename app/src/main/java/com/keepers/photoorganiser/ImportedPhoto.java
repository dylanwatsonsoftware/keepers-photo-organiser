package com.keepers.photoorganiser;

import android.net.Uri;

public record ImportedPhoto(Uri uri, long takenAtMillis, PhotoOrigin origin,
        MediaType mediaType, long durationMillis) {
    public ImportedPhoto(Uri uri, long takenAtMillis, PhotoOrigin origin) {
        this(uri, takenAtMillis, origin, MediaType.PHOTO, 0);
    }
}
