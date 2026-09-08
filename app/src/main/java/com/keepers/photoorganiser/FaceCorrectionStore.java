package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.Map;

public final class FaceCorrectionStore {
    public static final String IGNORE = "__ignore__";
    private final SharedPreferences preferences;

    public FaceCorrectionStore(Context context) {
        preferences = context.getSharedPreferences("face_corrections", Context.MODE_PRIVATE);
    }

    public void save(Map<String, String> corrections) {
        SharedPreferences.Editor editor = preferences.edit().clear();
        for (Map.Entry<String, String> entry : corrections.entrySet())
            if (entry.getValue() != null && !entry.getValue().isBlank())
                editor.putString(entry.getKey(), entry.getValue());
        editor.apply();
    }

    public Map<String, String> load() {
        HashMap<String, String> result = new HashMap<>();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet())
            if (entry.getValue() instanceof String value) result.put(entry.getKey(), value);
        return Map.copyOf(result);
    }

    public static String key(FaceObservation face) {
        return face.photoId() + "#" + face.faceIndex();
    }
}
