package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
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
}
