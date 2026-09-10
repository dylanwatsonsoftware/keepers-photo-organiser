package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public final class FaceSuggestionRejectionStore {
    private final SharedPreferences preferences;

    public FaceSuggestionRejectionStore(Context context) {
        preferences = context.getSharedPreferences("face_suggestion_rejections",
                Context.MODE_PRIVATE);
    }

    public void reject(String faceKey, String personId) {
        HashSet<String> rejected = new HashSet<>(preferences.getStringSet(faceKey, Set.of()));
        rejected.add(personId);
        preferences.edit().putStringSet(faceKey, rejected).apply();
    }

    public boolean isRejected(String faceKey, String personId) {
        return preferences.getStringSet(faceKey, Set.of()).contains(personId);
    }

    public void allow(String faceKey, String personId) {
        HashSet<String> rejected = new HashSet<>(preferences.getStringSet(faceKey, Set.of()));
        if (!rejected.remove(personId)) return;
        preferences.edit().putStringSet(faceKey, rejected).apply();
    }
}
