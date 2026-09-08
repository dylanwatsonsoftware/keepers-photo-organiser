package com.keepers.photoorganiser;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.view.ViewGroup;
import android.view.Gravity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.content.Intent;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public final class PeopleActivity extends Activity {
    private static final int[] NAME_IDS = {R.id.person_1_name, R.id.person_2_name, R.id.person_3_name};
    private static final int[] ALBUM_IDS = {R.id.person_1_album, R.id.person_2_album, R.id.person_3_album};
    private static final int[] TRACKED_IDS = {R.id.person_1_tracked, R.id.person_2_tracked,
            R.id.person_3_tracked};
    private AsyncThumbnailLoader thumbnailLoader;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_people);
        thumbnailLoader = AsyncThumbnailLoader.forResolver(getContentResolver());
        List<TrackedPerson> saved = new TrackedPersonStore(this).load();
        for (int index = 0; index < Math.min(3, saved.size()); index++) fill(index, saved.get(index));
        findViewById(R.id.people_back).setOnClickListener(view -> finish());
        findViewById(R.id.save_people).setOnClickListener(view -> save());
        showDiscoveryProgress();
        showDiscoveredGroups();
    }

    @Override protected void onResume() {
        super.onResume();
        showDiscoveryProgress();
    }

    private void showDiscoveryProgress() {
        int count = new FaceObservationStore(this).observationCount();
        ((TextView) findViewById(R.id.face_discovery_status)).setText(count == 0
                ? "No face observations yet — review the gallery to analyse photos"
                : count + (count == 1 ? " face observation" : " face observations")
                + " ready for grouping");
    }

    private void showDiscoveredGroups() {
        LinearLayout container = findViewById(R.id.discovered_face_groups);
        container.removeAllViews();
        List<FaceIdentityGroup> groups = FaceClusterer.cluster(
                new FaceObservationStore(this).loadAll(), .30);
        List<TrackedPerson> people = currentPeople();
        Map<String, String> assignments = new FaceGroupAssignmentStore(this).load();
        for (FaceIdentityGroup group : groups) container.addView(groupCard(group, people, assignments));
    }

    private LinearLayout groupCard(FaceIdentityGroup group, List<TrackedPerson> people,
            Map<String, String> assignments) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackgroundResource(R.drawable.person_setup_card);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardParams);
        int photos = (int) group.photoIds().stream().distinct().count();
        card.setContentDescription("Seen in " + photos + (photos == 1 ? " photo" : " photos"));
        card.setClickable(true);
        card.setOnClickListener(view -> startActivity(new Intent(this, FaceGroupReviewActivity.class)
                .putExtra(FaceGroupReviewActivity.EXTRA_GROUP_ID, group.id())));

        FaceObservation representative = group.members().get(0);
        ImageView face = new ImageView(this);
        face.setScaleType(ImageView.ScaleType.CENTER_CROP);
        face.setBackgroundColor(0xFFE8EAED);
        card.addView(face, new LinearLayout.LayoutParams(dp(68), dp(68)));
        thumbnailLoader.load(face, Uri.parse(representative.photoId()), 256,
                bitmap -> showFaceCrop(face, bitmap, representative));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        detailsParams.setMargins(dp(12), 0, 0, 0);
        card.addView(details, detailsParams);
        TextView count = new TextView(this);
        count.setText("Seen in " + photos + (photos == 1 ? " photo" : " photos"));
        count.setTextColor(0xFF3C4043);
        count.setTextSize(14);
        details.addView(count);

        ArrayList<String> labels = new ArrayList<>();
        ArrayList<String> ids = new ArrayList<>();
        labels.add("Ignore for now"); ids.add("");
        for (int i = 0; i < people.size(); i++) {
            TrackedPerson person = people.get(i);
            labels.add(person.name().isBlank() ? "Person " + (i + 1) : person.name());
            ids.add(person.id());
        }
        Spinner chooser = new Spinner(this);
        chooser.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                labels));
        chooser.setSelection(Math.max(0, ids.indexOf(assignments.getOrDefault(group.id(), ""))));
        chooser.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                    android.view.View view, int position, long id) {
                HashMap<String, String> changed = new HashMap<>(
                        new FaceGroupAssignmentStore(PeopleActivity.this).load());
                if (ids.get(position).isBlank()) changed.remove(group.id());
                else changed.put(group.id(), ids.get(position));
                new FaceGroupAssignmentStore(PeopleActivity.this).save(changed);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        details.addView(chooser, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        return card;
    }

    private static void showFaceCrop(ImageView view, Bitmap bitmap, FaceObservation face) {
        if (bitmap == null) return;
        int left = Math.max(0, (int) (face.left() * bitmap.getWidth()));
        int top = Math.max(0, (int) (face.top() * bitmap.getHeight()));
        int right = Math.min(bitmap.getWidth(), Math.max(left + 1,
                (int) (face.right() * bitmap.getWidth())));
        int bottom = Math.min(bitmap.getHeight(), Math.max(top + 1,
                (int) (face.bottom() * bitmap.getHeight())));
        view.setImageBitmap(Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top));
    }

    private List<TrackedPerson> currentPeople() {
        ArrayList<TrackedPerson> people = new ArrayList<>();
        for (int index = 0; index < 3; index++) people.add(new TrackedPerson("person-" + (index + 1),
                text(NAME_IDS[index]), text(ALBUM_IDS[index]),
                ((Switch) findViewById(TRACKED_IDS[index])).isChecked()));
        return people;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
        showDiscoveredGroups();
        ((TextView) findViewById(R.id.people_status)).setText(
                "Saved. Face groups will be linked to these profiles next.");
    }

    private String text(int id) {
        return ((EditText) findViewById(id)).getText().toString().trim();
    }

    @Override protected void onDestroy() {
        if (thumbnailLoader != null) thumbnailLoader.close();
        super.onDestroy();
    }
}
