package com.keepers.photoorganiser;

public final class AlbumControlMatcher {
    private AlbumControlMatcher() {}
    public static boolean isAddToAlbum(CharSequence label) {
        if (label == null) return false;
        String normalized = label.toString().trim();
        return "add to".equalsIgnoreCase(normalized)
                || "add to album".equalsIgnoreCase(normalized);
    }
    public static boolean isAlbumPickerOption(CharSequence label) {
        return label != null && "album".equalsIgnoreCase(label.toString().trim());
    }
    public static boolean isAlbum(CharSequence label, String albumName) {
        return label != null && albumName != null && !albumName.trim().isEmpty()
                && albumName.trim().equalsIgnoreCase(label.toString().trim());
    }
}
