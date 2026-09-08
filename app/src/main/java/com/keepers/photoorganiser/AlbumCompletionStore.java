package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public final class AlbumCompletionStore {
    private static final String COMPLETED = "completed_photo_albums";
    private final SharedPreferences preferences;

    public AlbumCompletionStore(Context context) {
        preferences = context.getSharedPreferences("album_completions", Context.MODE_PRIVATE);
    }

    public boolean contains(String photoId, String albumName) {
        return load().contains(key(photoId, albumName));
    }

    public void mark(String photoId, String albumName) {
        Set<String> completed = load();
        completed.add(key(photoId, albumName));
        preferences.edit().putStringSet(COMPLETED, completed).apply();
    }

    public void clear() { preferences.edit().clear().apply(); }

    private Set<String> load() {
        return new HashSet<>(preferences.getStringSet(COMPLETED, Set.of()));
    }

    private static String key(String photoId, String albumName) {
        return photoId + "\n" + albumName.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
