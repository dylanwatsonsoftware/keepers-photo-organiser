package com.keepers.photoorganiser;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public final class PeopleActivity extends Activity {
    private static final int[] NAME_IDS = {R.id.person_1_name, R.id.person_2_name, R.id.person_3_name};
    private static final int[] ALBUM_IDS = {R.id.person_1_album, R.id.person_2_album, R.id.person_3_album};
    private static final int[] TRACKED_IDS = {R.id.person_1_tracked, R.id.person_2_tracked,
            R.id.person_3_tracked};

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_people);
        List<TrackedPerson> saved = new TrackedPersonStore(this).load();
        for (int index = 0; index < Math.min(3, saved.size()); index++) fill(index, saved.get(index));
        findViewById(R.id.people_back).setOnClickListener(view -> finish());
        findViewById(R.id.save_people).setOnClickListener(view -> save());
    }

    private void fill(int index, TrackedPerson person) {
        ((EditText) findViewById(NAME_IDS[index])).setText(person.name());
        ((EditText) findViewById(ALBUM_IDS[index])).setText(person.albumName());
        ((Switch) findViewById(TRACKED_IDS[index])).setChecked(person.tracked());
    }

    private void save() {
        ArrayList<TrackedPerson> people = new ArrayList<>();
        for (int index = 0; index < 3; index++) {
            people.add(new TrackedPerson("person-" + (index + 1),
                    text(NAME_IDS[index]), text(ALBUM_IDS[index]),
                    ((Switch) findViewById(TRACKED_IDS[index])).isChecked()));
        }
        new TrackedPersonStore(this).save(people);
        ((TextView) findViewById(R.id.people_status)).setText(
                "Saved. Face groups will be linked to these profiles next.");
    }

    private String text(int id) {
        return ((EditText) findViewById(id)).getText().toString().trim();
    }
}
