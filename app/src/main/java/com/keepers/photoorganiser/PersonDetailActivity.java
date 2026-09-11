package com.keepers.photoorganiser;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PersonDetailActivity extends Activity {
    public static final String EXTRA_PERSON_ID = "person_id";
    private static final int FACE_PAGE_SIZE = 40;
    private AsyncThumbnailLoader thumbnailLoader;
    private TrackedPerson person;
    private boolean selectingFaces;
    private final Set<String> selectedFaceKeys = new HashSet<>();
    private int visibleFaceLimit = FACE_PAGE_SIZE;

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
        findViewById(R.id.person_detail_select_faces).setOnClickListener(view -> {
            selectingFaces = true;
            selectedFaceKeys.clear();
            renderFaces();
        });
        findViewById(R.id.person_detail_cancel_selection).setOnClickListener(view -> {
            selectingFaces = false;
            selectedFaceKeys.clear();
            renderFaces();
        });
        findViewById(R.id.person_detail_remove_selected).setOnClickListener(view -> {
            if (!selectedFaceKeys.isEmpty()) confirmBulkRemoval();
        });
        findViewById(R.id.person_detail_show_more_faces).setOnClickListener(view -> {
            visibleFaceLimit += FACE_PAGE_SIZE;
            renderFaces();
        });
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
        List<FaceObservation> allFaces = associatedFaces();
        FaceDisplayWindow.Result window = FaceDisplayWindow.limit(allFaces, visibleFaceLimit);
        List<FaceObservation> faces = window.faces();
        GridLayout container = findViewById(R.id.person_detail_faces);
        container.removeAllViews();
        TextView select = findViewById(R.id.person_detail_select_faces);
        select.setVisibility(faces.isEmpty() || selectingFaces ? View.GONE : View.VISIBLE);
        findViewById(R.id.person_detail_bulk_actions).setVisibility(
                selectingFaces ? View.VISIBLE : View.GONE);
        TextView bulkStatus = findViewById(R.id.person_detail_bulk_status);
        int selected = selectedFaceKeys.size();
        bulkStatus.setText(selected + (selected == 1 ? " face selected" : " faces selected"));
        TextView removeSelected = findViewById(R.id.person_detail_remove_selected);
        removeSelected.setEnabled(selected > 0);
        removeSelected.setAlpha(selected > 0 ? 1f : .45f);
        String name = displayName();
        ((TextView) findViewById(R.id.person_detail_faces_summary)).setText(allFaces.isEmpty()
                ? "No confirmed faces yet. Confirm a suggestion in Faces to review first."
                : allFaces.size() + (allFaces.size() == 1 ? " confirmed face" : " confirmed faces")
                + (window.hasMore() ? " · showing " + faces.size() : "")
                + " · choose a feature or remove incorrect matches");
        TextView showMore = findViewById(R.id.person_detail_show_more_faces);
        showMore.setVisibility(window.hasMore() ? View.VISIBLE : View.GONE);
        showMore.setText("Show " + Math.min(FACE_PAGE_SIZE, window.remaining()) + " more");
        String featureKey = effectiveFeatureKey(allFaces);
        for (FaceObservation face : faces) container.addView(faceTile(face, name,
                FaceCorrectionStore.key(face).equals(featureKey)));
        showFeatureFace(allFaces, featureKey);
    }

    private LinearLayout faceTile(FaceObservation face, String name, boolean featureFace) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setPadding(dp(8), dp(8), dp(8), dp(8));
        tile.setBackgroundResource(R.drawable.person_setup_card);
        String key = FaceCorrectionStore.key(face);
        tile.setSelected(selectedFaceKeys.contains(key));
        if (selectingFaces) tile.setOnClickListener(view -> {
            if (!selectedFaceKeys.add(key)) selectedFaceKeys.remove(key);
            renderFaces();
        });
        GridLayout.LayoutParams tileParams = new GridLayout.LayoutParams();
        tileParams.width = 0;
        tileParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        tileParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        tileParams.setMargins(dp(4), 0, dp(4), dp(10));
        tile.setLayoutParams(tileParams);

        FrameLayout frame = new FrameLayout(this);
        frame.setSelected(featureFace);
        frame.setPadding(dp(3), dp(3), dp(3), dp(3));
        frame.setBackgroundResource(R.drawable.album_person_choice);
        tile.addView(frame, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(148)));
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
        Runnable selectFeature = () -> {
            new PersonFeatureFaceStore(this).save(person.id(), key);
            renderFaces();
        };
        if (!selectingFaces && !featureFace) crop.setOnClickListener(view -> selectFeature.run());

        if (selectingFaces) {
            TextView check = new TextView(this);
            check.setText(selectedFaceKeys.contains(key) ? "✓" : "");
            check.setTextColor(0xFFFFFFFF);
            check.setTextSize(18);
            check.setGravity(Gravity.CENTER);
            check.setBackgroundResource(R.drawable.gallery_primary_action);
            FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(dp(34), dp(34),
                    Gravity.TOP | Gravity.END);
            checkParams.setMargins(0, dp(8), dp(8), 0);
            frame.addView(check, checkParams);
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        TextView feature = compactAction(featureFace
                ? "Current feature photo" : "Use as feature");
        feature.setTextColor(featureFace ? 0xFF174EA6 : 0xFF3C4043);
        feature.setBackgroundResource(R.drawable.person_feature_action);
        feature.setEnabled(!featureFace);
        feature.setClickable(!featureFace);
        feature.setFocusable(!featureFace);
        if (!featureFace) feature.setOnClickListener(view -> selectFeature.run());
        LinearLayout.LayoutParams featureParams = new LinearLayout.LayoutParams(
                0, dp(40), 1f);
        featureParams.setMargins(0, dp(8), dp(6), 0);
        actions.addView(feature, featureParams);

        ImageButton remove = new ImageButton(this);
        remove.setImageResource(R.drawable.ic_close);
        remove.setColorFilter(0xFFB3261E);
        remove.setPadding(dp(10), dp(10), dp(10), dp(10));
        remove.setBackgroundResource(R.drawable.person_remove_action);
        remove.setContentDescription("Remove face from " + name);
        remove.setOnClickListener(view -> confirmRemoval(face, name));
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        removeParams.setMargins(0, dp(8), 0, 0);
        actions.addView(remove, removeParams);
        if (!selectingFaces) tile.addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return tile;
    }

    private void confirmBulkRemoval() {
        int count = selectedFaceKeys.size();
        new AlertDialog.Builder(this)
                .setTitle("Remove selected faces?")
                .setMessage("Keepers will stop treating " + count
                        + (count == 1 ? " face" : " faces") + " as " + displayName() + ".")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove", (dialog, which) -> removeSelectedFaces())
                .show();
    }

    private void removeSelectedFaces() {
        FaceCorrectionStore store = new FaceCorrectionStore(this);
        store.save(BulkFaceRemoval.apply(person.id(), selectedFaceKeys, store.load()));
        PersonFeatureFaceStore features = new PersonFeatureFaceStore(this);
        if (selectedFaceKeys.contains(features.load(person.id()))) features.clear(person.id());
        AlbumApprovalInvalidator.invalidate(this);
        selectingFaces = false;
        selectedFaceKeys.clear();
        renderFaces();
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

    private List<FaceObservation> associatedFaces() {
        return ConfirmedPersonFaces.forPerson(person.id(),
                new FaceObservationStore(this).loadAll(),
                new FaceCorrectionStore(this).load());
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
        thumbnailLoader.loadProgressive(feature, Uri.parse(face.photoId()),
                FeaturePortrait.PREVIEW_PIXELS, FeaturePortrait.FULL_PIXELS,
                bitmap -> feature.setImageBitmap(FeaturePortrait.crop(bitmap, face)));
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
