package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public final class AlbumReviewSelectionStore {
    private static final String VALUES = "selected_assignments";
    private static final String REVIEWED = "reviewed";
    private final SharedPreferences preferences;

    public AlbumReviewSelectionStore(Context context) {
        preferences = context.getSharedPreferences("album_review", Context.MODE_PRIVATE);
    }

    public boolean hasReview() { return preferences.getBoolean(REVIEWED, false); }
    public Set<String> load() {
        return new HashSet<>(preferences.getStringSet(VALUES, Set.of()));
    }
    public void save(Set<String> selections) {
        preferences.edit().putStringSet(VALUES, new HashSet<>(selections))
                .putBoolean(REVIEWED, true).apply();
    }
    public void clear() { preferences.edit().clear().apply(); }
    public static String key(String photoId, String personId) { return photoId + "\n" + personId; }
}
