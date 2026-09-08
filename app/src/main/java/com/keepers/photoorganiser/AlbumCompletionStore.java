package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class AlbumCompletionStore {
    private static final String COMPLETED = "completed_photo_albums";
    private static final String COMPLETED_LABELS = "completed_photo_album_labels";
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
        Set<String> labels = loadLabels();
        labels.removeIf(label -> sameAlbumKey(label, photoId, albumName));
        labels.add(photoId + "\n" + albumName.trim());
        preferences.edit().putStringSet(COMPLETED, completed)
                .putStringSet(COMPLETED_LABELS, labels).apply();
    }

    public void remove(String photoId, String albumName) {
        Set<String> completed = load();
        completed.remove(key(photoId, albumName));
        Set<String> labels = loadLabels();
        labels.removeIf(label -> sameAlbumKey(label, photoId, albumName));
        preferences.edit().putStringSet(COMPLETED, completed)
                .putStringSet(COMPLETED_LABELS, labels).apply();
    }

    public boolean hasAny(String photoId) {
        String prefix = photoId + "\n";
        for (String completed : load()) if (completed.startsWith(prefix)) return true;
        return false;
    }

    public List<String> albumNames(String photoId) {
        String prefix = photoId + "\n";
        ArrayList<String> names = new ArrayList<>();
        for (String label : loadLabels()) {
            if (label.startsWith(prefix)) names.add(label.substring(prefix.length()));
        }
        if (names.isEmpty()) {
            for (String completed : load()) {
                if (completed.startsWith(prefix)) names.add(completed.substring(prefix.length()));
            }
        }
        names.sort(Comparator.naturalOrder());
        return List.copyOf(names);
    }

    public void clear() { preferences.edit().clear().apply(); }

    private Set<String> load() {
        return new HashSet<>(preferences.getStringSet(COMPLETED, Set.of()));
    }

    private Set<String> loadLabels() {
        return new HashSet<>(preferences.getStringSet(COMPLETED_LABELS, Set.of()));
    }

    private static boolean sameAlbumKey(String label, String photoId, String albumName) {
        int separator = label.lastIndexOf('\n');
        return separator >= 0 && label.substring(0, separator).equals(photoId)
                && label.substring(separator + 1).trim().equalsIgnoreCase(albumName.trim());
    }

    private static String key(String photoId, String albumName) {
        return photoId + "\n" + albumName.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
