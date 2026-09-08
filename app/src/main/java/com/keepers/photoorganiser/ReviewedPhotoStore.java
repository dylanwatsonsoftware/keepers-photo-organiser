package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public final class ReviewedPhotoStore {
    private static final String REVIEWED = "reviewed_photos";
    private final SharedPreferences preferences;

    public ReviewedPhotoStore(Context context) {
        preferences = context.getSharedPreferences("album_reviewed_photos", Context.MODE_PRIVATE);
    }

    public Set<String> load() {
        return new HashSet<>(preferences.getStringSet(REVIEWED, Set.of()));
    }

    public boolean contains(String photoId) { return load().contains(photoId); }

    public void mark(String photoId) { change(photoId, true); }

    public void unmark(String photoId) { change(photoId, false); }

    private void change(String photoId, boolean add) {
        Set<String> reviewed = load();
        if (add) reviewed.add(photoId); else reviewed.remove(photoId);
        preferences.edit().putStringSet(REVIEWED, reviewed).apply();
    }

    public void clear() { preferences.edit().clear().apply(); }
}
