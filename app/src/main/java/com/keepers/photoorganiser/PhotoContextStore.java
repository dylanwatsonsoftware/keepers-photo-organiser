package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.EnumMap;
import java.util.Map;
import java.util.StringJoiner;

public final class PhotoContextStore {
    private static final int SCHEMA_VERSION = 1;
    private final SharedPreferences preferences;

    public PhotoContextStore(Context context) {
        preferences = context.getSharedPreferences("photo_contexts", Context.MODE_PRIVATE);
    }

    public void save(String photoId, PhotoContext context) {
        StringJoiner values = new StringJoiner(",");
        context.probabilities().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> values.add(entry.getKey().name() + "=" + entry.getValue()));
        preferences.edit().putString(photoId, SCHEMA_VERSION + "|" + values).apply();
    }

    public PhotoContext load(String photoId) {
        String encoded = preferences.getString(photoId, null);
        if (encoded == null) return null;
        String[] versioned = encoded.split("\\|", 2);
        if (versioned.length != 2 || !Integer.toString(SCHEMA_VERSION).equals(versioned[0]))
            return null;
        EnumMap<PhotoContextType, Double> values = new EnumMap<>(PhotoContextType.class);
        if (versioned[1].isEmpty()) return new PhotoContext(values);
        try {
            for (String item : versioned[1].split(",")) {
                String[] pair = item.split("=", 2);
                values.put(PhotoContextType.valueOf(pair[0]), Double.parseDouble(pair[1]));
            }
            return new PhotoContext(values);
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException invalid) {
            return null;
        }
    }

    public void clear() { preferences.edit().clear().apply(); }
}
