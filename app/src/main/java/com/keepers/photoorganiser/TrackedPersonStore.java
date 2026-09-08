package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

public final class TrackedPersonStore {
    private final SharedPreferences preferences;

    public TrackedPersonStore(Context context) {
        preferences = context.getSharedPreferences("tracked_people", Context.MODE_PRIVATE);
    }

    public void save(List<TrackedPerson> people) {
        SharedPreferences.Editor editor = preferences.edit().clear().putInt("count", people.size());
        for (int index = 0; index < people.size(); index++) {
            TrackedPerson person = people.get(index);
            editor.putString("id_" + index, person.id());
            editor.putString("name_" + index, person.name());
            editor.putString("album_" + index, person.albumName());
            editor.putBoolean("tracked_" + index, person.tracked());
        }
        editor.apply();
    }

    public List<TrackedPerson> load() {
        int count = preferences.getInt("count", 0);
        ArrayList<TrackedPerson> people = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            people.add(new TrackedPerson(preferences.getString("id_" + index, "person-" + (index + 1)),
                    preferences.getString("name_" + index, ""),
                    preferences.getString("album_" + index, ""),
                    preferences.getBoolean("tracked_" + index, false)));
        }
        return people;
    }
}
