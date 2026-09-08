package com.keepers.photoorganiser;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Switch;
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
    private final Map<String, ImageView> profilePortraitViews = new HashMap<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_people);
        thumbnailLoader = AsyncThumbnailLoader.forResolver(getContentResolver());
        profiles = findViewById(R.id.people_profiles);
        List<TrackedPerson> saved = new TrackedPersonStore(this).load();
        if (saved.isEmpty()) for (int index = 0; index < 3; index++)
            addPersonCard(new TrackedPerson(nextPersonId(), "", "", false));
        else for (TrackedPerson person : saved) {
            addPersonCard(person);
            nextPersonNumber = Math.max(nextPersonNumber, numberAfterPrefix(person.id()) + 1);
        }
        findViewById(R.id.people_back).setOnClickListener(view -> finish());
        findViewById(R.id.save_people).setOnClickListener(view -> save());
        findViewById(R.id.add_person).setOnClickListener(view ->
                addPersonCard(new TrackedPerson(nextPersonId(), "", "", false)));
        findViewById(R.id.open_advanced_settings).setOnClickListener(view ->
                startActivity(new Intent(this, MainActivity.class)));
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
        groups = groups.stream().sorted(Comparator
                .comparingLong((FaceIdentityGroup group) -> group.photoIds().stream().distinct().count())
                .reversed().thenComparing(FaceIdentityGroup::id)).toList();
        List<TrackedPerson> people = currentPeople();
        Map<String, String> assignments = new FaceGroupAssignmentStore(this).load();
        Map<String, String> predictions = FaceIdentityLearner.predict(
                new FaceObservationStore(this).loadAll(), groups, assignments,
                new FaceCorrectionStore(this).load(), .15);
        Map<String, FaceObservation> portraits = portraits(groups, assignments);
        for (FaceIdentityGroup group : groups)
            container.addView(groupCard(group, people, assignments, predictions, portraits));
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

        String predictedId = predictedPerson(group, predictions);
        if (!assignments.containsKey(group.id()) && !predictedId.isBlank()) {
            TextView suggestion = new TextView(this);
            suggestion.setText("Suggested: " + personName(predictedId, people)
                    + " · choose below to confirm");
            suggestion.setTextColor(0xFFB06000);
            suggestion.setTextSize(13);
            details.addView(suggestion);
        }

        ArrayList<PersonChoice> choices = new ArrayList<>();
        choices.add(new PersonChoice("", "Ignore for now", null));
        for (TrackedPerson person : people) choices.add(new PersonChoice(person.id(),
                person.name().isBlank() ? "Unnamed person" : person.name(), portraits.get(person.id())));
        Spinner chooser = new Spinner(this);
        chooser.setAdapter(new PersonChoiceAdapter(choices));
        String currentAssignment = assignments.getOrDefault(group.id(), "");
        chooser.setSelection(choiceIndex(choices, currentAssignment));
        chooser.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                    android.view.View view, int position, long id) {
                HashMap<String, String> changed = new HashMap<>(
                        new FaceGroupAssignmentStore(PeopleActivity.this).load());
                String selectedId = choices.get(position).id();
                if (selectedId.equals(currentAssignment)) return;
                if (selectedId.isBlank()) changed.remove(group.id());
                else changed.put(group.id(), selectedId);
                new FaceGroupAssignmentStore(PeopleActivity.this).save(changed);
                AlbumApprovalInvalidator.invalidate(PeopleActivity.this);
                containerForGroups().post(PeopleActivity.this::showDiscoveredGroups);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        details.addView(chooser, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        return card;
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
        ArrayList<TrackedPerson> people = new ArrayList<>();
        for (int index = 0; index < profiles.getChildCount(); index++) {
            LinearLayout card = (LinearLayout) profiles.getChildAt(index);
            people.add(new TrackedPerson(card.getTag().toString(), taggedText(card, "person_name"),
                    taggedText(card, "person_album"),
                    ((Switch) card.findViewWithTag("person_tracked")).isChecked()));
        }
        return people;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void save() {
        List<TrackedPerson> people = currentPeople();
        new TrackedPersonStore(this).save(people);
        AlbumApprovalInvalidator.invalidate(this);
        showDiscoveredGroups();
        ((TextView) findViewById(R.id.people_status)).setText(
                "Saved. Review the discovered faces below.");
    }

    private void addPersonCard(TrackedPerson person) {
        LinearLayout card = new LinearLayout(this);
        card.setTag(person.id());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundResource(R.drawable.person_setup_card);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(10));
        profiles.addView(card, cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView portrait = new ImageView(this);
        portrait.setScaleType(ImageView.ScaleType.CENTER_CROP);
        portrait.setBackgroundColor(0xFFE8EAED);
        portrait.setContentDescription("Face photo for " + (person.name().isBlank()
                ? "unnamed person" : person.name()));
        profilePortraitViews.put(person.id(), portrait);
        header.addView(portrait, new LinearLayout.LayoutParams(dp(58), dp(58)));
        Switch tracked = new Switch(this);
        tracked.setTag("person_tracked");
        tracked.setText("Track this person");
        tracked.setChecked(person.tracked());
        LinearLayout.LayoutParams trackedParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        trackedParams.setMargins(dp(12), 0, 0, 0);
        header.addView(tracked, trackedParams);
        card.addView(header);
        EditText name = field("Name", "person_name", person.name());
        card.addView(name);
        card.addView(field("Exact Google Photos album name", "person_album", person.albumName()));
    }

    private EditText field(String hint, String tag, String value) {
        EditText field = new EditText(this);
        field.setTag(tag);
        field.setHint(hint);
        field.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        field.setText(value);
        field.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return field;
    }

    private String nextPersonId() { return "person-" + nextPersonNumber++; }

    private static int numberAfterPrefix(String id) {
        try { return Integer.parseInt(id.replace("person-", "")); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private static String taggedText(LinearLayout card, String tag) {
        return ((EditText) card.findViewWithTag(tag)).getText().toString().trim();
    }

    private Map<String, FaceObservation> portraits(List<FaceIdentityGroup> groups,
            Map<String, String> assignments) {
        LinkedHashMap<String, FaceObservation> result = new LinkedHashMap<>();
        Map<String, String> corrections = new FaceCorrectionStore(this).load();
        for (FaceIdentityGroup group : groups) for (FaceObservation face : group.members()) {
            String person = corrections.get(FaceCorrectionStore.key(face));
            if (person != null && !FaceCorrectionStore.IGNORE.equals(person))
                result.putIfAbsent(person, face);
        }
        for (FaceIdentityGroup group : groups) {
            String person = assignments.get(group.id());
            if (person != null && !group.members().isEmpty()) result.putIfAbsent(person,
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
            thumbnailLoader.load(portrait, Uri.parse(face.photoId()), 256,
                    bitmap -> showFaceCrop(portrait, bitmap, face));
        }
    }

    private static String predictedPerson(FaceIdentityGroup group,
            Map<String, String> predictions) {
        HashMap<String, Integer> counts = new HashMap<>();
        for (FaceObservation face : group.members()) {
            String person = predictions.get(FaceCorrectionStore.key(face));
            if (person != null) counts.merge(person, 1, Integer::sum);
        }
        return counts.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("");
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
