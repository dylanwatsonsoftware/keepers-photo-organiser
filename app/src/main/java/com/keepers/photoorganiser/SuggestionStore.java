package com.keepers.photoorganiser;

import android.content.Context;
import java.util.HashSet;
import java.util.Set;

public final class SuggestionStore {
    private final android.content.SharedPreferences preferences;
    public SuggestionStore(Context context) {
        preferences = context.getSharedPreferences("suggestions", Context.MODE_PRIVATE);
    }
    public void save(Set<String> ids) {
        preferences.edit().putStringSet("photo_ids", new HashSet<>(ids)).apply();
    }
    public Set<String> load() {
        return new HashSet<>(preferences.getStringSet("photo_ids", Set.of()));
    }
    public void saveAlternatives(Set<String> ids) {
        preferences.edit().putStringSet("alternative_ids", new HashSet<>(ids)).apply();
    }
    public Set<String> loadAlternatives() {
        return new HashSet<>(preferences.getStringSet("alternative_ids", Set.of()));
    }
}
