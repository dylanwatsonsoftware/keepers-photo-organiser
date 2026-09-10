package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public final class HiddenPhotoStore {
    private static final String VALUES = "hidden_photo_ids";
    private final SharedPreferences preferences;
    private final PhotoStackStore stacks;

    public HiddenPhotoStore(Context context) {
        preferences = context.getSharedPreferences("hidden_photos", Context.MODE_PRIVATE);
        stacks = new PhotoStackStore(context);
    }

    public Set<String> load() {
        return new HashSet<>(preferences.getStringSet(VALUES, Set.of()));
    }

    public void hide(Set<String> photoIds) {
        HashSet<String> hidden = new HashSet<>(load());
        for (String photoId : photoIds) {
            hidden.add(photoId);
            hidden.addAll(stacks.load(photoId));
        }
        preferences.edit().putStringSet(VALUES, hidden).apply();
    }

    public void restore(String photoId) {
        HashSet<String> hidden = new HashSet<>(load());
        if (hidden.remove(photoId)) preferences.edit().putStringSet(VALUES, hidden).apply();
    }

    public void clear() { preferences.edit().remove(VALUES).apply(); }
}
