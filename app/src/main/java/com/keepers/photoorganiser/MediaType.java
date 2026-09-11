package com.keepers.photoorganiser;

import android.net.Uri;

public enum MediaType {
    PHOTO("image/*"), VIDEO("video/*");

    private final String mimePattern;

    MediaType(String mimePattern) { this.mimePattern = mimePattern; }

    public String mimePattern() { return mimePattern; }

    public static MediaType fromMimeType(String mimeType) {
        return mimeType != null && mimeType.toLowerCase(java.util.Locale.ROOT)
                .startsWith("video/") ? VIDEO : PHOTO;
    }

    public static MediaType from(Uri uri, String mimeType) {
        if (fromMimeType(mimeType) == VIDEO) return VIDEO;
        String value = uri == null ? "" : uri.toString().toLowerCase(java.util.Locale.ROOT);
        return value.contains("/video/") ? VIDEO : PHOTO;
    }
}
