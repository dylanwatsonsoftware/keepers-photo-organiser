package com.keepers.photoorganiser;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FaceGroupReviewActivity extends Activity {
    public static final String EXTRA_GROUP_ID = "face_group_id";
    private AsyncThumbnailLoader thumbnailLoader;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_face_group_review);
        thumbnailLoader = AsyncThumbnailLoader.forResolver(getContentResolver());
        findViewById(R.id.face_group_back).setOnClickListener(view -> finish());
        render();
    }

    private void render() {
        String requestedId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        FaceIdentityGroup group = FaceClusterer.cluster(
                new FaceObservationStore(this).loadAll(), .30).stream()
                .filter(candidate -> candidate.id().equals(requestedId)).findFirst().orElse(null);
        LinearLayout container = findViewById(R.id.face_group_members);
        if (group == null) {
            ((TextView) findViewById(R.id.face_group_summary)).setText(
                    "This face group changed after analysis. Return and choose it again.");
            return;
        }
        List<TrackedPerson> people = new TrackedPersonStore(this).load();
        String groupPersonId = new FaceGroupAssignmentStore(this).load()
                .getOrDefault(group.id(), "");
        for (FaceObservation face : group.members())
            container.addView(faceRow(face, groupPersonId, people));
        ((TextView) findViewById(R.id.face_group_summary)).setText(group.members().size()
                + (group.members().size() == 1 ? " face to check" : " faces to check")
                + " · corrections apply immediately");
    }

    private LinearLayout faceRow(FaceObservation face, String groupPersonId,
            List<TrackedPerson> people) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.setBackgroundResource(R.drawable.person_setup_card);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowParams);
        ImageView crop = new ImageView(this);
        crop.setScaleType(ImageView.ScaleType.CENTER_CROP);
        crop.setBackgroundColor(0xFFE8EAED);
        crop.setContentDescription("Detected face");
        row.addView(crop, new LinearLayout.LayoutParams(dp(84), dp(84)));
        thumbnailLoader.load(crop, Uri.parse(face.photoId()), 320,
                bitmap -> showCrop(crop, bitmap, face));

        ArrayList<String> labels = new ArrayList<>();
        ArrayList<String> ids = new ArrayList<>();
        String groupName = personName(groupPersonId, people);
        labels.add("Use group choice" + (groupName.isBlank() ? "" : " · " + groupName));
        ids.add("");
        for (TrackedPerson person : people) {
            labels.add(person.name().isBlank() ? person.id() : person.name());
            ids.add(person.id());
        }
        labels.add("Ignore this face"); ids.add(FaceCorrectionStore.IGNORE);
        Spinner choice = new Spinner(this);
        choice.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                labels));
        String key = FaceCorrectionStore.key(face);
        String correction = new FaceCorrectionStore(this).load().getOrDefault(key, "");
        choice.setSelection(Math.max(0, ids.indexOf(correction)));
        choice.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                    android.view.View view, int position, long id) {
                FaceCorrectionStore store = new FaceCorrectionStore(FaceGroupReviewActivity.this);
                HashMap<String, String> changes = new HashMap<>(store.load());
                if (ids.get(position).isBlank()) changes.remove(key);
                else changes.put(key, ids.get(position));
                store.save(changes);
                AlbumApprovalInvalidator.invalidate(FaceGroupReviewActivity.this);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        LinearLayout.LayoutParams choiceParams = new LinearLayout.LayoutParams(0, dp(56), 1);
        choiceParams.setMargins(dp(12), 0, 0, 0);
        row.addView(choice, choiceParams);
        return row;
    }

    private static String personName(String id, List<TrackedPerson> people) {
        for (TrackedPerson person : people) if (person.id().equals(id)) return person.name();
        return "";
    }

    private static void showCrop(ImageView view, Bitmap bitmap, FaceObservation face) {
        if (bitmap == null) return;
        int left = Math.max(0, (int) (face.left() * bitmap.getWidth()));
        int top = Math.max(0, (int) (face.top() * bitmap.getHeight()));
        int right = Math.min(bitmap.getWidth(), Math.max(left + 1,
                (int) (face.right() * bitmap.getWidth())));
        int bottom = Math.min(bitmap.getHeight(), Math.max(top + 1,
                (int) (face.bottom() * bitmap.getHeight())));
        view.setImageBitmap(Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        if (thumbnailLoader != null) thumbnailLoader.close();
        super.onDestroy();
    }
}
