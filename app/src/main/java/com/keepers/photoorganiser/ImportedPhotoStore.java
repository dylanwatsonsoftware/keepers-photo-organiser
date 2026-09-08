package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ImportedPhotoStore {
    private static final String ITEMS = "items";
    private final SharedPreferences preferences;

    public ImportedPhotoStore(Context context) {
        preferences = context.getSharedPreferences("imported_photos", Context.MODE_PRIVATE);
    }

    public void add(ImportedPhoto photo) {
        Map<String, ImportedPhoto> photos = byId(load());
        photos.put(photo.uri().toString(), photo);
        save(photos.values());
    }

    public List<ImportedPhoto> load() {
        ArrayList<ImportedPhoto> photos = new ArrayList<>();
        for (String item : preferences.getStringSet(ITEMS, Set.of())) {
            String[] parts = item.split("\t", 3);
            if (parts.length != 3) continue;
            try {
                photos.add(new ImportedPhoto(Uri.parse(Uri.decode(parts[2])),
                        Long.parseLong(parts[0]), PhotoOrigin.valueOf(parts[1])));
            } catch (IllegalArgumentException ignored) {}
        }
        photos.sort(Comparator.comparingLong(ImportedPhoto::takenAtMillis).reversed());
        return List.copyOf(photos);
    }

    public void clear() { preferences.edit().clear().apply(); }

    private void save(java.util.Collection<ImportedPhoto> photos) {
        java.util.HashSet<String> items = new java.util.HashSet<>();
        for (ImportedPhoto photo : photos) items.add(photo.takenAtMillis() + "\t"
                + photo.origin().name() + "\t" + Uri.encode(photo.uri().toString()));
        preferences.edit().putStringSet(ITEMS, items).apply();
    }

    private static Map<String, ImportedPhoto> byId(List<ImportedPhoto> photos) {
        LinkedHashMap<String, ImportedPhoto> byId = new LinkedHashMap<>();
        for (ImportedPhoto photo : photos) byId.put(photo.uri().toString(), photo);
        return byId;
    }
}
