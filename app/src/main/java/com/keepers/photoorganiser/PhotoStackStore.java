package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PhotoStackStore {
    private final SharedPreferences preferences;

    public PhotoStackStore(Context context) {
        preferences = context.getSharedPreferences("photo_stacks", Context.MODE_PRIVATE);
    }

    public void save(Map<String, List<String>> stacks) {
        SharedPreferences.Editor editor = preferences.edit().clear();
        for (Map.Entry<String, List<String>> entry : stacks.entrySet()) {
            editor.putString(entry.getKey(), String.join("\n", entry.getValue()));
        }
        editor.apply();
    }

    public List<String> load(String id) {
        String encoded = preferences.getString(id, null);
        return encoded == null || encoded.isEmpty() ? List.of() : List.of(encoded.split("\n"));
    }

    public Map<String, List<String>> loadAll() {
        HashMap<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            String encoded = String.valueOf(entry.getValue());
            if (!encoded.isEmpty()) result.put(entry.getKey(), List.of(encoded.split("\n")));
        }
        return Map.copyOf(result);
    }
}
