package com.keepers.photoorganiser;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PersonDetailActivity extends Activity {
    public static final String EXTRA_PERSON_ID = "person_id";
    private AsyncThumbnailLoader thumbnailLoader;
    private TrackedPerson person;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_person_detail);
        thumbnailLoader = AsyncThumbnailLoader.forResolver(getContentResolver());
        String requested = getIntent().getStringExtra(EXTRA_PERSON_ID);
        person = new TrackedPersonStore(this).load().stream()
                .filter(candidate -> candidate.id().equals(requested)).findFirst().orElse(null);
        if (person == null) { finish(); return; }
        findViewById(R.id.person_detail_back).setOnClickListener(view -> finish());
        EditText name = findViewById(R.id.person_detail_name);
        EditText album = findViewById(R.id.person_detail_album);
        name.setText(person.name());
        album.setText(person.albumName());
        name.addTextChangedListener(watcher(() -> save(name, album)));
        album.addTextChangedListener(watcher(() -> save(name, album)));
        renderFaces();
    }

    private TextWatcher watcher(Runnable changed) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count,
                    int after) {}
            @Override public void onTextChanged(CharSequence value, int start, int before,
                    int count) {}
            @Override public void afterTextChanged(Editable value) { changed.run(); }
        };
    }

    private void save(EditText name, EditText album) {
        person = new TrackedPerson(person.id(), name.getText().toString().trim(),
                album.getText().toString().trim(), true);
        ArrayList<TrackedPerson> people = new ArrayList<>(new TrackedPersonStore(this).load());
        for (int index = 0; index < people.size(); index++)
            if (people.get(index).id().equals(person.id())) people.set(index, person);
        new TrackedPersonStore(this).save(people);
        AlbumApprovalInvalidator.invalidate(this);
    }

    private void renderFaces() {
        List<FaceObservation> faces = associatedFaces();
        LinearLayout container = findViewById(R.id.person_detail_faces);
        container.removeAllViews();
        String name = displayName();
        ((TextView) findViewById(R.id.person_detail_faces_summary)).setText(faces.isEmpty()
                ? "No confirmed or suggested faces yet. Assign a discovered group first."
                : faces.size() + (faces.size() == 1 ? " associated face" : " associated faces")
                + " · remove any incorrect matches");
        for (FaceObservation face : faces) container.addView(faceCard(face, name));
        showFeatureFace(faces);
    }

    private LinearLayout faceCard(FaceObservation face, String name) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackgroundResource(R.drawable.person_setup_card);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardParams);
        ImageView crop = new ImageView(this);
        crop.setScaleType(ImageView.ScaleType.CENTER_CROP);
        crop.setBackgroundResource(R.drawable.preview_face_crop);
        crop.setClipToOutline(true);
        card.addView(crop, new LinearLayout.LayoutParams(dp(86), dp(86)));
        thumbnailLoader.load(crop, Uri.parse(face.photoId()), 420,
                bitmap -> showLooseCrop(crop, bitmap, face));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        actionParams.setMargins(dp(12), 0, 0, 0);
        card.addView(actions, actionParams);
        String key = FaceCorrectionStore.key(face);
        TextView feature = action("Use as feature");
        feature.setOnClickListener(view -> {
            new PersonFeatureFaceStore(this).save(person.id(), key);
            showFeatureFace(associatedFaces());
        });
        actions.addView(feature);
        TextView remove = action("Remove from " + name);
        remove.setTextColor(0xFFB3261E);
        remove.setOnClickListener(view -> {
            FaceCorrectionStore store = new FaceCorrectionStore(this);
            HashMap<String, String> corrections = new HashMap<>(store.load());
            corrections.put(key, FaceCorrectionStore.IGNORE);
            store.save(corrections);
            PersonFeatureFaceStore features = new PersonFeatureFaceStore(this);
            if (key.equals(features.load(person.id()))) features.clear(person.id());
            AlbumApprovalInvalidator.invalidate(this);
            renderFaces();
        });
        actions.addView(remove);
        return card;
    }

    private TextView action(String text) {
        TextView action = new TextView(this);
        action.setText(text);
        action.setTextColor(0xFF1A73E8);
        action.setTextSize(15);
        action.setGravity(Gravity.CENTER_VERTICAL);
        action.setMinHeight(dp(40));
        action.setClickable(true);
        action.setFocusable(true);
        return action;
    }

    private List<FaceObservation> associatedFaces() {
        FaceObservationStore observations = new FaceObservationStore(this);
        List<FaceObservation> all = observations.loadAll();
        List<FaceIdentityGroup> groups = FaceClusterer.cluster(all, .30);
        Map<String, String> assignments = new FaceGroupAssignmentStore(this).load();
        Map<String, String> corrections = new FaceCorrectionStore(this).load();
        Map<String, String> learned = FaceIdentityLearner.predict(all, groups, assignments,
                corrections, .15);
        return all.stream().filter(face -> belongsToPerson(face, groups, assignments,
                        corrections, learned))
                .sorted(Comparator.comparing(FaceObservation::photoId)
                        .thenComparingInt(FaceObservation::faceIndex)).toList();
    }

    private boolean belongsToPerson(FaceObservation face, List<FaceIdentityGroup> groups,
            Map<String, String> assignments, Map<String, String> corrections,
            Map<String, String> learned) {
        String key = FaceCorrectionStore.key(face);
        String correction = corrections.get(key);
        if (correction != null) return person.id().equals(correction);
        for (FaceIdentityGroup group : groups) if (group.members().stream()
                .anyMatch(member -> FaceCorrectionStore.key(member).equals(key))) {
            String assigned = assignments.get(group.id());
            if (assigned != null) return person.id().equals(assigned);
            break;
        }
        return person.id().equals(learned.get(key));
    }

    private void showFeatureFace(List<FaceObservation> faces) {
        ImageView feature = findViewById(R.id.person_detail_feature);
        String selected = new PersonFeatureFaceStore(this).load(person.id());
        FaceObservation face = faces.stream()
                .filter(candidate -> FaceCorrectionStore.key(candidate).equals(selected))
                .findFirst().orElse(faces.isEmpty() ? null : faces.get(0));
        if (face == null) { feature.setImageDrawable(null); return; }
        thumbnailLoader.load(feature, Uri.parse(face.photoId()), 520,
                bitmap -> showLooseCrop(feature, bitmap, face));
    }

    private static void showLooseCrop(ImageView view, Bitmap bitmap, FaceObservation face) {
        if (bitmap == null) return;
        double marginX = (face.right() - face.left()) * .38;
        double marginY = (face.bottom() - face.top()) * .38;
        int left = Math.max(0, (int) ((face.left() - marginX) * bitmap.getWidth()));
        int top = Math.max(0, (int) ((face.top() - marginY) * bitmap.getHeight()));
        int right = Math.min(bitmap.getWidth(), Math.max(left + 1,
                (int) ((face.right() + marginX) * bitmap.getWidth())));
        int bottom = Math.min(bitmap.getHeight(), Math.max(top + 1,
                (int) ((face.bottom() + marginY) * bitmap.getHeight())));
        view.setImageBitmap(Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top));
    }

    private String displayName() { return person.name().isBlank() ? "this person" : person.name(); }
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
    @Override protected void onDestroy() {
        if (thumbnailLoader != null) thumbnailLoader.close();
        super.onDestroy();
    }
}
