package com.keepers.photoorganiser;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.BaseAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.content.Intent;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.LinkedHashMap;

public final class PeopleActivity extends Activity {
    private AsyncThumbnailLoader thumbnailLoader;
    private LinearLayout profiles;
    private int nextPersonNumber = 1;
    private boolean showConfirmedFaces;
    private final Map<String, ImageView> profilePortraitViews = new HashMap<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_people);
        thumbnailLoader = AsyncThumbnailLoader.forResolver(getContentResolver());
        profiles = findViewById(R.id.people_profiles);
        List<TrackedPerson> saved = new TrackedPersonStore(this).load();
        for (TrackedPerson person : saved) {
            nextPersonNumber = Math.max(nextPersonNumber, numberAfterPrefix(person.id()) + 1);
        }
        findViewById(R.id.people_back).setOnClickListener(view -> finish());
        findViewById(R.id.add_person).setOnClickListener(view -> {
            TrackedPerson person = new TrackedPerson(nextPersonId(), "", "", true);
            ArrayList<TrackedPerson> changed = new ArrayList<>(new TrackedPersonStore(this).load());
            changed.add(person);
            new TrackedPersonStore(this).save(changed);
            AlbumApprovalInvalidator.invalidate(this);
            showPeople();
            openPerson(person.id());
        });
        findViewById(R.id.open_advanced_settings).setOnClickListener(view ->
                startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.toggle_confirmed_faces).setOnClickListener(view -> {
            showConfirmedFaces = !showConfirmedFaces;
            showDiscoveredGroups();
        });
    }

    @Override protected void onResume() {
        super.onResume();
        showPeople();
        showDiscoveryProgress();
        showDiscoveredGroups();
    }

    private void showPeople() {
        profiles.removeAllViews();
        profilePortraitViews.clear();
        for (TrackedPerson person : new TrackedPersonStore(this).load()) addPersonCard(person);
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
        Map<String, String> corrections = new FaceCorrectionStore(this).load();
        groups = groups.stream().sorted(Comparator
                .comparingInt((FaceIdentityGroup group) -> FaceReviewInbox.needsReview(
                        group, assignments, corrections) ? 0 : 1)
                .thenComparing(Comparator.comparingLong((FaceIdentityGroup group) ->
                        group.photoIds().stream().distinct().count()).reversed())
                .thenComparing(FaceIdentityGroup::id)).toList();
        long needsReview = groups.stream().filter(group -> FaceReviewInbox.needsReview(
                group, assignments, corrections)).count();
        int confirmed = groups.size() - (int) needsReview;
        TextView toggle = findViewById(R.id.toggle_confirmed_faces);
        toggle.setVisibility(confirmed > 0 ? View.VISIBLE : View.GONE);
        toggle.setSelected(showConfirmedFaces);
        toggle.setText(showConfirmedFaces ? "Hide confirmed" : "Show confirmed (" + confirmed + ")");
        TextView summary = findViewById(R.id.discovered_faces_summary);
        summary.setText(needsReview == 0
                ? groups.isEmpty() ? "No discovered face groups yet."
                        : "All discovered face groups have feedback."
                : needsReview + (needsReview == 1 ? " group needs" : " groups need")
                        + " your feedback.");
        Map<String, String> predictions = FaceIdentityLearner.predict(
                new FaceObservationStore(this).loadAll(), groups, assignments,
                corrections, FaceGroupSuggestion.MAXIMUM_DISTANCE);
        Map<String, FaceObservation> portraits = portraits(groups, assignments);
        for (FaceIdentityGroup group : groups) {
            if (!showConfirmedFaces && !FaceReviewInbox.needsReview(
                    group, assignments, corrections)) continue;
            container.addView(groupCard(group, people, assignments, predictions, portraits));
        }
        refreshProfilePortraits(portraits);
    }

    private LinearLayout groupCard(FaceIdentityGroup group, List<TrackedPerson> people,
            Map<String, String> assignments, Map<String, String> predictions,
            Map<String, FaceObservation> portraits) {
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

        String currentAssignment = FaceGroupAssignmentResolver.personFor(group, assignments);
        String predictedId = FaceGroupSuggestion.personId(group, predictions);
        if (currentAssignment.isBlank() && !predictedId.isBlank()) {
            TextView suggestion = new TextView(this);
            String predictedName = personName(predictedId, people);
            suggestion.setText("Likely " + predictedName);
            suggestion.setTextColor(0xFFB06000);
            suggestion.setTextSize(14);
            suggestion.setTypeface(null, android.graphics.Typeface.BOLD);
            suggestion.setGravity(Gravity.CENTER);
            suggestion.setBackgroundResource(R.drawable.face_suggestion_action);
            suggestion.setClickable(true);
            suggestion.setFocusable(true);
            suggestion.setContentDescription("Confirm this face group as " + predictedName);
            suggestion.setOnClickListener(view -> applyGroupChoice(
                    group, currentAssignment, predictedId));
            LinearLayout.LayoutParams suggestionParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
            suggestionParams.setMargins(0, dp(6), 0, dp(3));
            details.addView(suggestion, suggestionParams);
        }

        ArrayList<PersonChoice> choices = new ArrayList<>();
        choices.add(new PersonChoice("", "Ignore for now", null));
        for (TrackedPerson person : people) choices.add(new PersonChoice(person.id(),
                person.name().isBlank() ? "Unnamed person" : person.name(), portraits.get(person.id())));
        Spinner chooser = new Spinner(this);
        chooser.setAdapter(new PersonChoiceAdapter(choices));
        chooser.setSelection(choiceIndex(choices, currentAssignment));
        chooser.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                    android.view.View view, int position, long id) {
                String selectedId = choices.get(position).id();
                if (selectedId.equals(currentAssignment)) return;
                applyGroupChoice(group, currentAssignment, selectedId);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        details.addView(chooser, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        return card;
    }

    private void applyGroupChoice(FaceIdentityGroup group, String currentAssignment,
            String selectedId) {
        HashMap<String, String> changed = new HashMap<>(
                new FaceGroupAssignmentStore(this).load());
        if (selectedId.isBlank()) changed.remove(group.id());
        else changed.put(group.id(), selectedId);
        new FaceGroupAssignmentStore(this).save(changed);
        FaceCorrectionStore faceCorrections = new FaceCorrectionStore(this);
        faceCorrections.save(FaceGroupEvidence.applyChoice(group,
                faceCorrections.load(), currentAssignment, selectedId));
        AlbumApprovalInvalidator.invalidate(this);
        containerForGroups().post(this::showDiscoveredGroups);
    }

    private static void showFaceCrop(ImageView view, Bitmap bitmap, FaceObservation face) {
        if (bitmap == null) return;
        double horizontalMargin = (face.right() - face.left()) * .20;
        double verticalMargin = (face.bottom() - face.top()) * .20;
        int left = Math.max(0, (int) ((face.left() - horizontalMargin) * bitmap.getWidth()));
        int top = Math.max(0, (int) ((face.top() - verticalMargin) * bitmap.getHeight()));
        int right = Math.min(bitmap.getWidth(), Math.max(left + 1,
                (int) ((face.right() + horizontalMargin) * bitmap.getWidth())));
        int bottom = Math.min(bitmap.getHeight(), Math.max(top + 1,
                (int) ((face.bottom() + verticalMargin) * bitmap.getHeight())));
        view.setImageBitmap(Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top));
    }

    private List<TrackedPerson> currentPeople() {
        return new TrackedPersonStore(this).load();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void addPersonCard(TrackedPerson person) {
        LinearLayout card = new LinearLayout(this);
        card.setTag(person.id());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundResource(R.drawable.person_setup_card);
        card.setClickable(true);
        card.setFocusable(true);
        String name = person.name().isBlank() ? "Unnamed person" : person.name();
        card.setContentDescription("Edit " + name);
        card.setOnClickListener(view -> openPerson(person.id()));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(10));
        profiles.addView(card, cardParams);

        ImageView portrait = new ImageView(this);
        portrait.setScaleType(ImageView.ScaleType.CENTER_CROP);
        portrait.setBackgroundResource(R.drawable.preview_face_crop);
        portrait.setClipToOutline(true);
        portrait.setContentDescription("Face photo for " + name);
        profilePortraitViews.put(person.id(), portrait);
        card.addView(portrait, new LinearLayout.LayoutParams(dp(64), dp(64)));
        TextView label = new TextView(this);
        label.setText(name);
        label.setTextColor(0xFF202124);
        label.setTextSize(18);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        labelParams.setMargins(dp(14), 0, 0, 0);
        card.addView(label, labelParams);
        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextColor(0xFF5F6368);
        arrow.setTextSize(28);
        card.addView(arrow, new LinearLayout.LayoutParams(dp(28),
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void openPerson(String personId) {
        startActivity(new Intent(this, PersonDetailActivity.class)
                .putExtra(PersonDetailActivity.EXTRA_PERSON_ID, personId));
    }

    private String nextPersonId() { return "person-" + nextPersonNumber++; }

    private static int numberAfterPrefix(String id) {
        try { return Integer.parseInt(id.replace("person-", "")); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private Map<String, FaceObservation> portraits(List<FaceIdentityGroup> groups,
            Map<String, String> assignments) {
        LinkedHashMap<String, FaceObservation> result = new LinkedHashMap<>();
        Map<String, FaceObservation> byKey = new HashMap<>();
        for (FaceIdentityGroup group : groups) for (FaceObservation face : group.members())
            byKey.put(FaceCorrectionStore.key(face), face);
        PersonFeatureFaceStore featureFaces = new PersonFeatureFaceStore(this);
        for (TrackedPerson person : currentPeople()) {
            FaceObservation preferred = byKey.get(featureFaces.load(person.id()));
            if (preferred != null) result.put(person.id(), preferred);
        }
        Map<String, String> corrections = new FaceCorrectionStore(this).load();
        for (FaceIdentityGroup group : groups) for (FaceObservation face : group.members()) {
            String person = corrections.get(FaceCorrectionStore.key(face));
            if (person != null && !FaceCorrectionStore.IGNORE.equals(person))
                result.putIfAbsent(person, face);
        }
        for (FaceIdentityGroup group : groups) {
            String person = FaceGroupAssignmentResolver.personFor(group, assignments);
            if (!person.isBlank() && !group.members().isEmpty()) result.putIfAbsent(person,
                    group.members().get(0));
        }
        return result;
    }

    private void refreshProfilePortraits(Map<String, FaceObservation> portraits) {
        for (int index = 0; index < profiles.getChildCount(); index++) {
            LinearLayout card = (LinearLayout) profiles.getChildAt(index);
            FaceObservation face = portraits.get(card.getTag().toString());
            if (face == null) continue;
            ImageView portrait = profilePortraitViews.get(card.getTag().toString());
            String featureKey = new PersonFeatureFaceStore(this).load(card.getTag().toString());
            if (FaceCorrectionStore.key(face).equals(featureKey)) {
                thumbnailLoader.loadProgressive(portrait, Uri.parse(face.photoId()),
                        FeaturePortrait.PREVIEW_PIXELS, FeaturePortrait.FULL_PIXELS,
                        bitmap -> portrait.setImageBitmap(FeaturePortrait.crop(bitmap, face)));
            } else {
                thumbnailLoader.load(portrait, Uri.parse(face.photoId()), 256,
                        bitmap -> showPortraitCrop(portrait, bitmap, face));
            }
        }
    }

    private static void showPortraitCrop(ImageView view, Bitmap bitmap, FaceObservation face) {
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

    private static String personName(String id, List<TrackedPerson> people) {
        for (TrackedPerson person : people) if (person.id().equals(id))
            return person.name().isBlank() ? "Unnamed person" : person.name();
        return "Unknown person";
    }

    private static int choiceIndex(List<PersonChoice> choices, String id) {
        for (int index = 0; index < choices.size(); index++)
            if (choices.get(index).id().equals(id)) return index;
        return 0;
    }

    private LinearLayout containerForGroups() { return findViewById(R.id.discovered_face_groups); }

    private record PersonChoice(String id, String label, FaceObservation portrait) {}

    private final class PersonChoiceAdapter extends BaseAdapter {
        private final List<PersonChoice> choices;
        PersonChoiceAdapter(List<PersonChoice> choices) { this.choices = choices; }
        @Override public int getCount() { return choices.size(); }
        @Override public Object getItem(int position) { return choices.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View reuse, ViewGroup parent) {
            return row(position);
        }
        @Override public View getDropDownView(int position, View reuse, ViewGroup parent) {
            return row(position);
        }
        private View row(int position) {
            PersonChoice choice = choices.get(position);
            LinearLayout row = new LinearLayout(PeopleActivity.this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(6), dp(8), dp(6));
            ImageView image = new ImageView(PeopleActivity.this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundColor(0xFFE8EAED);
            row.addView(image, new LinearLayout.LayoutParams(dp(38), dp(38)));
            if (choice.portrait() != null) thumbnailLoader.load(image,
                    Uri.parse(choice.portrait().photoId()), 160,
                    bitmap -> showFaceCrop(image, bitmap, choice.portrait()));
            TextView label = new TextView(PeopleActivity.this);
            label.setText(choice.label());
            label.setTextColor(0xFF202124);
            label.setTextSize(16);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            labelParams.setMargins(dp(10), 0, 0, 0);
            row.addView(label, labelParams);
            return row;
        }
    }

    @Override protected void onDestroy() {
        if (thumbnailLoader != null) thumbnailLoader.close();
        super.onDestroy();
    }
}
