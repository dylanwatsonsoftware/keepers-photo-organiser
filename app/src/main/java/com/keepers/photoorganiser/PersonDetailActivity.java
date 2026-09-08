package com.keepers.photoorganiser;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.GridLayout;
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
        GridLayout container = findViewById(R.id.person_detail_faces);
        container.removeAllViews();
        String name = displayName();
        ((TextView) findViewById(R.id.person_detail_faces_summary)).setText(faces.isEmpty()
                ? "No confirmed or suggested faces yet. Assign a discovered group first."
                : faces.size() + (faces.size() == 1 ? " associated face" : " associated faces")
                + " · choose a feature or remove incorrect matches");
        String featureKey = effectiveFeatureKey(faces);
        for (FaceObservation face : faces) container.addView(faceTile(face, name,
                FaceCorrectionStore.key(face).equals(featureKey)));
        showFeatureFace(faces, featureKey);
    }

    private LinearLayout faceTile(FaceObservation face, String name, boolean featureFace) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        GridLayout.LayoutParams tileParams = new GridLayout.LayoutParams();
        tileParams.width = dp(104);
        tileParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        tileParams.setMargins(0, 0, dp(6), dp(12));
        tile.setLayoutParams(tileParams);

        FrameLayout frame = new FrameLayout(this);
        frame.setSelected(featureFace);
        frame.setPadding(dp(3), dp(3), dp(3), dp(3));
        frame.setBackgroundResource(R.drawable.album_person_choice);
        tile.addView(frame, new LinearLayout.LayoutParams(dp(96), dp(96)));
        ImageView crop = new ImageView(this);
        crop.setScaleType(ImageView.ScaleType.CENTER_CROP);
        crop.setBackgroundResource(R.drawable.preview_face_crop);
        crop.setClipToOutline(true);
        crop.setContentDescription(featureFace ? "Feature face for " + name
                : "Face for " + name + ". Tap to use as feature.");
        frame.addView(crop, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        thumbnailLoader.load(crop, Uri.parse(face.photoId()), 520,
                bitmap -> showLooseCrop(crop, bitmap, face));
        String key = FaceCorrectionStore.key(face);
        Runnable selectFeature = () -> {
            new PersonFeatureFaceStore(this).save(person.id(), key);
            renderFaces();
        };
        if (!featureFace) crop.setOnClickListener(view -> selectFeature.run());

        TextView feature = compactAction(featureFace ? "Feature photo" : "Use as feature");
        feature.setTextColor(featureFace ? 0xFF174EA6 : 0xFF3C4043);
        feature.setBackgroundResource(featureFace ? R.drawable.keeper_summary_chip
                : R.drawable.gallery_filter_chip);
        feature.setClickable(!featureFace);
        feature.setFocusable(!featureFace);
        if (!featureFace) feature.setOnClickListener(view -> selectFeature.run());
        tile.addView(feature, actionLayoutParams());

        TextView remove = compactAction("Remove");
        remove.setTextColor(0xFFB3261E);
        remove.setBackgroundResource(R.drawable.person_remove_action);
        remove.setOnClickListener(view -> confirmRemoval(face, name));
        tile.addView(remove, actionLayoutParams());
        return tile;
    }

    private void confirmRemoval(FaceObservation face, String name) {
        new AlertDialog.Builder(this)
                .setTitle("Remove this face?")
                .setMessage("Keepers will stop treating this face as " + name
                        + ". You can identify it again later.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove", (dialog, which) -> removeFace(face))
                .show();
    }

    private void removeFace(FaceObservation face) {
        FaceCorrectionStore store = new FaceCorrectionStore(this);
        HashMap<String, String> corrections = new HashMap<>(store.load());
        String key = FaceCorrectionStore.key(face);
        corrections.put(key, FaceCorrectionStore.IGNORE);
        store.save(corrections);
        PersonFeatureFaceStore features = new PersonFeatureFaceStore(this);
        if (key.equals(features.load(person.id()))) features.clear(person.id());
        AlbumApprovalInvalidator.invalidate(this);
        renderFaces();
    }

    private TextView compactAction(String text) {
        TextView action = new TextView(this);
        action.setText(text);
        action.setTextSize(12);
        action.setGravity(Gravity.CENTER);
        action.setMinHeight(dp(32));
        action.setClickable(true);
        action.setFocusable(true);
        return action;
    }

    private LinearLayout.LayoutParams actionLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(32));
        params.setMargins(dp(2), dp(5), dp(2), 0);
        return params;
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
            String assigned = FaceGroupAssignmentResolver.personFor(group, assignments);
            if (!assigned.isBlank()) return person.id().equals(assigned);
            break;
        }
        return person.id().equals(learned.get(key));
    }

    private String effectiveFeatureKey(List<FaceObservation> faces) {
        String selected = new PersonFeatureFaceStore(this).load(person.id());
        boolean stillAssociated = faces.stream().anyMatch(face ->
                FaceCorrectionStore.key(face).equals(selected));
        return stillAssociated ? selected : faces.isEmpty()
                ? "" : FaceCorrectionStore.key(faces.get(0));
    }

    private void showFeatureFace(List<FaceObservation> faces, String selected) {
        ImageView feature = findViewById(R.id.person_detail_feature);
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
