package com.keepers.photoorganiser;

public final class AlbumControlMatcher {
    private AlbumControlMatcher() {}
    public static boolean isAddToAlbum(CharSequence label) {
        return label != null && "add to album".equalsIgnoreCase(label.toString().trim());
    }
    public static boolean isAlbum(CharSequence label, String albumName) {
        return label != null && albumName != null && !albumName.trim().isEmpty()
                && albumName.trim().equalsIgnoreCase(label.toString().trim());
    }
}
