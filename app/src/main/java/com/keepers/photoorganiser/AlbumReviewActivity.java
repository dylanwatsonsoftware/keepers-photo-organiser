package com.keepers.photoorganiser;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AlbumReviewActivity extends Activity {
    public static final String EXTRA_COMPLETED_COUNT = "completed_album_change_count";
    private AsyncThumbnailLoader thumbnailLoader;
    private AlbumReviewSelectionStore reviewStore;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_album_review);
        thumbnailLoader = AsyncThumbnailLoader.forResolver(getContentResolver());
        reviewStore = new AlbumReviewSelectionStore(this);
        findViewById(R.id.album_review_back).setOnClickListener(view -> finish());
        findViewById(R.id.album_review_setup_people).setOnClickListener(view ->
                startActivity(new Intent(this, PeopleActivity.class)));
        findViewById(R.id.confirm_album_review).setOnClickListener(view -> confirmReview());
        showAutomationResult();
    }

    private void showAutomationResult() {
        int count = getIntent().getIntExtra(EXTRA_COMPLETED_COUNT, 0);
        TextView status = findViewById(R.id.album_review_run_status);
        if (count <= 0) {
            status.setVisibility(android.view.View.GONE);
            return;
        }
        status.setText("Done — " + count + (count == 1
                ? " album change completed" : " album changes completed"));
        status.setVisibility(android.view.View.VISIBLE);
    }

    @Override protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        Set<String> keepers = new KeeperSelectionStore(this).load();
        List<TrackedPerson> people = new TrackedPersonStore(this).load().stream()
                .filter(person -> !person.albumName().isBlank()).toList();
        LinearLayout container = findViewById(R.id.album_review_items);
        container.removeAllViews();
        TextView summary = findViewById(R.id.album_review_summary);
        android.view.View setup = findViewById(R.id.album_review_setup_people);
        setup.setVisibility(android.view.View.GONE);
        updateConfirmAction(Set.of());
        if (keepers.isEmpty()) {
            summary.setText("Choose some Keepers first. Nothing will be added without your approval.");
            return;
        }
        if (people.isEmpty()) {
            summary.setText("Before reviewing suggestions, add a person and link their exact Google Photos album name.");
            setup.setVisibility(android.view.View.VISIBLE);
            return;
        }
        List<FaceIdentityGroup> groups = FaceClusterer.cluster(
                new FaceObservationStore(this).loadAll(), .30);
        List<AlbumAssignment> proposals = AlbumProposalEngine.propose(keepers, groups,
                new FaceGroupAssignmentStore(this).load(), new FaceCorrectionStore(this).load(), people);
        AlbumCompletionStore completions = new AlbumCompletionStore(this);
        Set<String> proposed = proposalKeys(proposals.stream().filter(proposal ->
                !completions.contains(proposal.photoId(), proposal.albumName())).toList());
        Set<String> selected = reviewStore.hasReview() ? reviewStore.load() : proposed;
        HashSet<String> eligible = new HashSet<>();
        for (String photo : keepers) for (TrackedPerson person : people)
            if (!completions.contains(photo, person.albumName()))
                eligible.add(AlbumReviewSelectionStore.key(photo, person.id()));
        boolean removedCompleted = selected.retainAll(eligible);
        if (!reviewStore.hasReview() || removedCompleted) reviewStore.save(selected);
        Map<String, FaceObservation> portraits = portraits(people, groups,
                new FaceGroupAssignmentStore(this).load(), new FaceCorrectionStore(this).load());

        ArrayList<String> photos = new ArrayList<>(keepers);
        photos.sort(String::compareTo);
        for (String photo : photos)
            container.addView(photoCard(photo, people, selected, proposed, portraits, completions));
        summary.setText(photos.size() + (photos.size() == 1 ? " Keeper" : " Keepers")
                + " · check or uncheck each child before continuing");
        updateConfirmAction(selected);
    }

    private LinearLayout photoCard(String photo, List<TrackedPerson> people, Set<String> selected,
            Set<String> proposed, Map<String, FaceObservation> portraits,
            AlbumCompletionStore completions) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackgroundResource(R.drawable.person_setup_card);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(params);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(Color.rgb(232, 234, 237));
        card.addView(image, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(180)));
        thumbnailLoader.load(image, Uri.parse(photo), 600);
        TextView instruction = new TextView(this);
        instruction.setText("Add this Keeper to:");
        instruction.setTextColor(0xFF3C4043);
        instruction.setTextSize(14);
        instruction.setPadding(0, dp(10), 0, dp(2));
        card.addView(instruction);
        GridLayout choices = new GridLayout(this);
        choices.setColumnCount(3);
        choices.setAlignmentMode(GridLayout.ALIGN_MARGINS);
        card.addView(choices, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        for (TrackedPerson person : people) {
            String key = AlbumReviewSelectionStore.key(photo, person.id());
            boolean completed = completions.contains(photo, person.albumName());
            LinearLayout choice = personChoice(person, portraits.get(person.id()),
                    selected.contains(key) && !completed, proposed.contains(key), completed);
            if (!completed) choice.setOnClickListener(view -> {
                boolean checked = !choice.isSelected();
                HashSet<String> changed = new HashSet<>(reviewStore.load());
                if (checked) changed.add(key); else changed.remove(key);
                AlbumApprovalInvalidator.invalidate(this);
                reviewStore.save(changed);
                updateChoice(choice, person, checked, proposed.contains(key), false);
                updateConfirmAction(changed);
            });
            choices.addView(choice);
        }
        return card;
    }

    private LinearLayout personChoice(TrackedPerson person, FaceObservation face,
            boolean selected, boolean suggested, boolean completed) {
        LinearLayout choice = new LinearLayout(this);
        choice.setOrientation(LinearLayout.VERTICAL);
        choice.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        choice.setPadding(dp(3), dp(3), dp(3), dp(8));
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = dp(98);
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.setMargins(0, 0, dp(8), dp(8));
        choice.setLayoutParams(params);
        choice.setClickable(true);
        choice.setFocusable(true);

        FrameLayout portraitFrame = new FrameLayout(this);
        portraitFrame.setTag("portrait_frame");
        portraitFrame.setBackgroundResource(R.drawable.album_person_choice);
        portraitFrame.setPadding(dp(3), dp(3), dp(3), dp(3));
        choice.addView(portraitFrame, new LinearLayout.LayoutParams(dp(84), dp(84)));
        ImageView portrait = new ImageView(this);
        portrait.setScaleType(ImageView.ScaleType.CENTER_CROP);
        portrait.setBackgroundResource(R.drawable.preview_face_crop);
        portrait.setClipToOutline(true);
        portrait.setContentDescription("Loosely cropped face for " + displayName(person));
        portraitFrame.addView(portrait, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView check = new TextView(this);
        check.setTag("selection_check");
        check.setText("✓");
        check.setTextColor(Color.WHITE);
        check.setTextSize(14);
        check.setGravity(android.view.Gravity.CENTER);
        check.setBackgroundResource(R.drawable.album_choice_check);
        FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(dp(24), dp(24),
                android.view.Gravity.TOP | android.view.Gravity.END);
        checkParams.setMargins(0, dp(2), dp(2), 0);
        portraitFrame.addView(check, checkParams);
        TextView name = new TextView(this);
        name.setText(displayName(person));
        name.setTextColor(0xFF3C4043);
        name.setTextSize(13);
        name.setGravity(android.view.Gravity.CENTER);
        name.setMaxLines(2);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameParams.setMargins(0, dp(5), 0, 0);
        choice.addView(name, nameParams);
        TextView source = new TextView(this);
        source.setTag("assignment_source");
        source.setTextSize(11);
        source.setTextColor(suggested ? 0xFFB06000 : 0xFF174EA6);
        source.setGravity(android.view.Gravity.CENTER);
        source.setBackgroundResource(suggested ? R.drawable.suggestion_summary_chip
                : R.drawable.keeper_summary_chip);
        source.setPadding(dp(7), 0, dp(7), 0);
        LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(22));
        sourceParams.setMargins(0, dp(4), 0, 0);
        choice.addView(source, sourceParams);
        updateChoice(choice, person, selected, suggested, completed);
        if (face != null) thumbnailLoader.loadProgressive(portrait, Uri.parse(face.photoId()),
                520, 1200, bitmap -> showLooseCrop(portrait, bitmap, face));
        return choice;
    }

    private void updateChoice(LinearLayout choice, TrackedPerson person, boolean selected,
            boolean suggested, boolean completed) {
        choice.setSelected(selected);
        FrameLayout frame = choice.findViewWithTag("portrait_frame");
        frame.setSelected(selected);
        TextView check = choice.findViewWithTag("selection_check");
        check.setVisibility(selected ? android.view.View.VISIBLE : android.view.View.GONE);
        TextView source = choice.findViewWithTag("assignment_source");
        source.setText(completed ? "Added" : suggested ? "Suggested" : "Your choice");
        source.setVisibility(completed || suggested || selected ? android.view.View.VISIBLE
                : android.view.View.GONE);
        choice.setContentDescription(displayName(person) + (completed ? " already added to "
                : selected ? " selected for " : " not selected for ") + person.albumName());
        choice.setClickable(!completed);
        choice.setFocusable(!completed);
    }

    private Map<String, FaceObservation> portraits(List<TrackedPerson> people,
            List<FaceIdentityGroup> groups, Map<String, String> assignments,
            Map<String, String> corrections) {
        LinkedHashMap<String, FaceObservation> result = new LinkedHashMap<>();
        HashMap<String, FaceObservation> byKey = new HashMap<>();
        for (FaceIdentityGroup group : groups) for (FaceObservation face : group.members())
            byKey.put(FaceCorrectionStore.key(face), face);
        PersonFeatureFaceStore featureFaces = new PersonFeatureFaceStore(this);
        for (TrackedPerson person : people) {
            FaceObservation preferred = byKey.get(featureFaces.load(person.id()));
            if (preferred != null) result.put(person.id(), preferred);
        }
        for (FaceIdentityGroup group : groups) for (FaceObservation face : group.members()) {
            String corrected = corrections.get(FaceCorrectionStore.key(face));
            if (corrected != null && !FaceCorrectionStore.IGNORE.equals(corrected))
                result.putIfAbsent(corrected, face);
        }
        for (FaceIdentityGroup group : groups) {
            String personId = FaceGroupAssignmentResolver.personFor(group, assignments);
            if (!personId.isBlank() && !group.members().isEmpty())
                result.putIfAbsent(personId, group.members().get(0));
        }
        return result;
    }

    private static void showLooseCrop(ImageView view, Bitmap bitmap, FaceObservation face) {
        if (bitmap == null) return;
        double marginX = (face.right() - face.left()) * .48;
        double marginY = (face.bottom() - face.top()) * .48;
        int left = Math.max(0, (int) ((face.left() - marginX) * bitmap.getWidth()));
        int top = Math.max(0, (int) ((face.top() - marginY) * bitmap.getHeight()));
        int right = Math.min(bitmap.getWidth(), Math.max(left + 1,
                (int) ((face.right() + marginX) * bitmap.getWidth())));
        int bottom = Math.min(bitmap.getHeight(), Math.max(top + 1,
                (int) ((face.bottom() + marginY) * bitmap.getHeight())));
        view.setImageBitmap(Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top));
    }

    private static String displayName(TrackedPerson person) {
        return person.name().isBlank() ? "Unnamed person" : person.name();
    }

    private static Set<String> proposalKeys(List<AlbumAssignment> proposals) {
        HashSet<String> keys = new HashSet<>();
        for (AlbumAssignment proposal : proposals) keys.add(AlbumReviewSelectionStore.key(
                proposal.photoId(), proposal.personId()));
        return keys;
    }

    private void updateConfirmAction(Set<String> selected) {
        TextView action = findViewById(R.id.confirm_album_review);
        int count = selected.size();
        action.setText(count == 0 ? "No album changes selected"
                : "Add " + count + (count == 1 ? " album change" : " album changes"));
        action.setEnabled(count > 0);
        action.setAlpha(count > 0 ? 1f : .45f);
    }

    private void confirmReview() {
        int count = reviewStore.load().size();
        if (count == 0) return;
        new AlertDialog.Builder(this).setTitle("Add to Google Photos albums?")
                .setMessage(count + (count == 1 ? " approved album change" : " approved album changes")
                        + " will run now. Keepers will only use the choices on this review screen.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add now", (dialog, which) -> startApprovedQueue()).show();
    }

    void startApprovedQueue() {
        if (!AccessibilityStatus.isKeepersEnabled(this)) {
            new AlbumActionQueueStore(this).cancel();
            new AlertDialog.Builder(this).setTitle("Enable Keepers first")
                    .setMessage("Android must allow the Keepers helper before approved album changes can run. No queue has been started.")
                    .setNegativeButton("Not now", null)
                    .setPositiveButton("Open accessibility settings", (dialog, which) ->
                            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                    .show();
            return;
        }
        HashMap<String, TrackedPerson> people = new HashMap<>();
        for (TrackedPerson person : new TrackedPersonStore(this).load()) people.put(person.id(), person);
        AlbumCompletionStore completions = new AlbumCompletionStore(this);
        ArrayList<AlbumAction> actions = new ArrayList<>();
        for (String key : reviewStore.load()) {
            int split = key.lastIndexOf('\n');
            if (split < 0) continue;
            TrackedPerson person = people.get(key.substring(split + 1));
            if (person == null || person.albumName().isBlank()) continue;
            String photoId = key.substring(0, split);
            if (completions.contains(photoId, person.albumName())) continue;
            actions.add(new AlbumAction(photoId, person.name(), person.albumName()));
        }
        actions.sort(Comparator.comparing(AlbumAction::photoId).thenComparing(AlbumAction::albumName));
        AlbumActionQueueStore queue = new AlbumActionQueueStore(this);
        queue.begin(actions);
        AlbumAction first = queue.current();
        if (first != null) startActivity(AlbumAutomationCoordinator.arm(this, first));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        if (thumbnailLoader != null) thumbnailLoader.close();
        super.onDestroy();
    }
}
