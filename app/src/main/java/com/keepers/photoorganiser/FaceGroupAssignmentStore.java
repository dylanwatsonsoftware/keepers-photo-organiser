package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.Map;

public final class FaceGroupAssignmentStore {
    private final SharedPreferences preferences;

    public FaceGroupAssignmentStore(Context context) {
        preferences = context.getSharedPreferences("face_group_assignments", Context.MODE_PRIVATE);
    }

    public void save(Map<String, String> assignments) {
        SharedPreferences.Editor editor = preferences.edit().clear();
        for (Map.Entry<String, String> entry : assignments.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isBlank())
                editor.putString(entry.getKey(), entry.getValue());
        }
        editor.apply();
    }

    public Map<String, String> load() {
        HashMap<String, String> result = new HashMap<>();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet())
            if (entry.getValue() instanceof String value && !value.isBlank())
                result.put(entry.getKey(), value);
        return Map.copyOf(result);
    }
}
