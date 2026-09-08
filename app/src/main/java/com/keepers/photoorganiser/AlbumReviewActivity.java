package com.keepers.photoorganiser;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.HashMap;

public final class AlbumReviewActivity extends Activity {
    private AsyncThumbnailLoader thumbnailLoader;
    private AlbumReviewSelectionStore reviewStore;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_album_review);
        thumbnailLoader = AsyncThumbnailLoader.forResolver(getContentResolver());
        reviewStore = new AlbumReviewSelectionStore(this);
        findViewById(R.id.album_review_back).setOnClickListener(view -> finish());
        findViewById(R.id.confirm_album_review).setOnClickListener(view -> confirmReview());
        render();
    }

    private void render() {
        Set<String> keepers = new KeeperSelectionStore(this).load();
        List<TrackedPerson> people = new TrackedPersonStore(this).load().stream()
                .filter(person -> person.tracked() && !person.albumName().isBlank()).toList();
        List<FaceIdentityGroup> groups = FaceClusterer.cluster(
                new FaceObservationStore(this).loadAll(), .30);
        List<AlbumAssignment> proposals = AlbumProposalEngine.propose(keepers, groups,
                new FaceGroupAssignmentStore(this).load(), new FaceCorrectionStore(this).load(), people);
        Set<String> selected = reviewStore.hasReview() ? reviewStore.load() : proposalKeys(proposals);
        if (!reviewStore.hasReview()) reviewStore.save(selected);

        ArrayList<String> photos = new ArrayList<>(keepers);
        photos.sort(String::compareTo);
        LinearLayout container = findViewById(R.id.album_review_items);
        for (String photo : photos) container.addView(photoCard(photo, people, selected));
        ((TextView) findViewById(R.id.album_review_summary)).setText(photos.isEmpty()
                ? "Choose some Keepers first. Nothing will be added without your approval."
                : photos.size() + (photos.size() == 1 ? " Keeper" : " Keepers")
                + " · check or uncheck each child before continuing");
        findViewById(R.id.confirm_album_review).setEnabled(!photos.isEmpty());
    }

    private LinearLayout photoCard(String photo, List<TrackedPerson> people, Set<String> selected) {
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
        for (TrackedPerson person : people) {
            String key = AlbumReviewSelectionStore.key(photo, person.id());
            CheckBox choice = new CheckBox(this);
            choice.setText(person.name() + " · " + person.albumName());
            choice.setChecked(selected.contains(key));
            choice.setOnCheckedChangeListener((button, checked) -> {
                HashSet<String> changed = new HashSet<>(reviewStore.load());
                if (checked) changed.add(key); else changed.remove(key);
                AlbumApprovalInvalidator.invalidate(this);
                reviewStore.save(changed);
            });
            card.addView(choice);
        }
        return card;
    }

    private static Set<String> proposalKeys(List<AlbumAssignment> proposals) {
        HashSet<String> keys = new HashSet<>();
        for (AlbumAssignment proposal : proposals) keys.add(AlbumReviewSelectionStore.key(
                proposal.photoId(), proposal.personId()));
        return keys;
    }

    private void confirmReview() {
        int count = reviewStore.load().size();
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
        ArrayList<AlbumAction> actions = new ArrayList<>();
        for (String key : reviewStore.load()) {
            int split = key.lastIndexOf('\n');
            if (split < 0) continue;
            TrackedPerson person = people.get(key.substring(split + 1));
            if (person == null || !person.tracked() || person.albumName().isBlank()) continue;
            actions.add(new AlbumAction(key.substring(0, split), person.name(), person.albumName()));
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
