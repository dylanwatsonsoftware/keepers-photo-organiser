package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;

public final class PersonFeatureFaceStore {
    private final SharedPreferences preferences;

    public PersonFeatureFaceStore(Context context) {
        preferences = context.getSharedPreferences("person_feature_faces", Context.MODE_PRIVATE);
    }

    public String load(String personId) { return preferences.getString(personId, ""); }

    public void save(String personId, String faceKey) {
        preferences.edit().putString(personId, faceKey).apply();
    }

    public void clear(String personId) { preferences.edit().remove(personId).apply(); }
}
