package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.List;

public final class AlbumActionQueueStore {
    private final SharedPreferences preferences;
    private final AlbumCompletionStore completions;
    private final ReviewedPhotoStore reviewedPhotos;
    public AlbumActionQueueStore(Context context) {
        preferences = context.getSharedPreferences("album_action_queue", Context.MODE_PRIVATE);
        completions = new AlbumCompletionStore(context);
        reviewedPhotos = new ReviewedPhotoStore(context);
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
    public int totalCount() { return preferences.getInt("count", 0); }
    public int completedCount() {
        int count = preferences.getInt("count", 0);
        if (count == 0) return 0;
        return isActive() ? preferences.getInt("index", 0) : count;
    }
    public AlbumAction current() {
        if (!isActive()) return null;
        int index = preferences.getInt("index", 0);
        return read(index);
    }
    public AlbumAction completeCurrent() {
        if (!isActive()) return null;
        AlbumAction completed = current();
        if (completed != null) completions.mark(completed.photoId(), completed.albumName());
        int next = preferences.getInt("index", 0) + 1;
        AlbumAction following = next < preferences.getInt("count", 0) ? read(next) : null;
        if (completed != null && (following == null
                || !completed.photoId().equals(following.photoId())))
            reviewedPhotos.mark(completed.photoId());
        if (following == null) {
            preferences.edit().putBoolean("active", false).apply();
            return null;
        }
        preferences.edit().putInt("index", next).apply();
        return following;
    }
    public void cancel() { preferences.edit().putBoolean("active", false).apply(); }
    private AlbumAction read(int index) {
        return new AlbumAction(preferences.getString("photo_" + index, ""),
                preferences.getString("person_" + index, ""),
                preferences.getString("album_" + index, ""));
    }
}
