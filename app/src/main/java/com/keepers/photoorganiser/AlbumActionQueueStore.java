package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.List;

public final class AlbumActionQueueStore {
    private final SharedPreferences preferences;
    public AlbumActionQueueStore(Context context) {
        preferences = context.getSharedPreferences("album_action_queue", Context.MODE_PRIVATE);
    }
    public void begin(List<AlbumAction> actions) {
        SharedPreferences.Editor editor = preferences.edit().clear()
                .putInt("count", actions.size()).putInt("index", 0)
                .putBoolean("active", !actions.isEmpty());
        for (int i = 0; i < actions.size(); i++) {
            AlbumAction action = actions.get(i);
            editor.putString("photo_" + i, action.photoId())
                    .putString("person_" + i, action.personName())
                    .putString("album_" + i, action.albumName());
        }
        editor.apply();
    }
    public boolean isActive() { return preferences.getBoolean("active", false); }
    public AlbumAction current() {
        if (!isActive()) return null;
        int index = preferences.getInt("index", 0);
        return read(index);
    }
    public AlbumAction completeCurrent() {
        if (!isActive()) return null;
        int next = preferences.getInt("index", 0) + 1;
        if (next >= preferences.getInt("count", 0)) {
            preferences.edit().putBoolean("active", false).apply();
            return null;
        }
        preferences.edit().putInt("index", next).apply();
        return read(next);
    }
    public void cancel() { preferences.edit().putBoolean("active", false).apply(); }
    private AlbumAction read(int index) {
        return new AlbumAction(preferences.getString("photo_" + index, ""),
                preferences.getString("person_" + index, ""),
                preferences.getString("album_" + index, ""));
    }
}
