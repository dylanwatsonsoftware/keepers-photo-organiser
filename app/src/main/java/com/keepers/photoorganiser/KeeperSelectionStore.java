package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import java.util.HashSet;
import java.util.Set;

public final class KeeperSelectionStore {
    private static final String PREFS = "keeper_selections";
    private static final String SELECTED_URIS = "selected_uris";
    private final SharedPreferences preferences;

    public KeeperSelectionStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public Set<String> load() {
        return new HashSet<>(preferences.getStringSet(SELECTED_URIS, Set.of()));
    }

    public boolean toggle(Uri photo) {
        Set<String> selected = load();
        boolean nowSelected = selected.add(photo.toString());
        if (!nowSelected) selected.remove(photo.toString());
        preferences.edit().putStringSet(SELECTED_URIS, selected).apply();
        return nowSelected;
    }

    public void clear() {
        preferences.edit().remove(SELECTED_URIS).apply();
    }

    void replace(Set<String> selected) {
        preferences.edit().putStringSet(SELECTED_URIS, new HashSet<>(selected)).apply();
    }
}
