package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import android.widget.CheckBox;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowDialog;
import android.app.AlertDialog;
import android.provider.Settings;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;

@RunWith(RobolectricTestRunner.class)
public class AlbumReviewActivityTest {
    @Test public void oneDestinationCanBeAddedToEveryShownPhotoWithoutClearingSelections() {
        seed();
        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();
        LinearLayout choices = activity.findViewById(R.id.album_review_apply_all_choices);

        assertEquals(3, choices.getChildCount());
        View addBenToAll = choices.getChildAt(1);
        assertTrue(!(addBenToAll instanceof Button));
        assertEquals("Add Ben to all shown photos", addBenToAll.getContentDescription());
        addBenToAll.performClick();

        assertEquals(Set.of(
                AlbumReviewSelectionStore.key("content://photos/a", "ada"),
                AlbumReviewSelectionStore.key("content://photos/a", "ben"),
                AlbumReviewSelectionStore.key("content://photos/b", "ben")),
                new AlbumReviewSelectionStore(activity).load());
    }

    @Test public void completedAutomationReturnsToAClearResultBanner() {
        seed();
        android.content.Context context = RuntimeEnvironment.getApplication();
        Intent intent = new Intent(context, AlbumReviewActivity.class)
                .putExtra(AlbumReviewActivity.EXTRA_COMPLETED_COUNT, 2);

        AlbumReviewActivity activity = Robolectric.buildActivity(
                AlbumReviewActivity.class, intent).setup().get();

        TextView result = activity.findViewById(R.id.album_review_run_status);
        assertEquals(View.VISIBLE, result.getVisibility());
        assertEquals("Done — 2 album changes completed", result.getText().toString());
    }

    @Test public void everyKeeperIsShownAndDetectedPersonStartsSelected() {
        seed();

        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();
        LinearLayout queue = activity.findViewById(R.id.album_review_items);

        assertEquals(2, queue.getChildCount());
        LinearLayout first = (LinearLayout) queue.getChildAt(0);
        GridLayout people = destinations(first);
        assertEquals(3, people.getChildCount());
        View ada = people.getChildAt(0);
        assertEquals("Ada", ((TextView) ((ViewGroup) ada).getChildAt(1)).getText().toString());
        assertEquals("Loosely cropped face for Ada",
                findFirst(ada, ImageView.class).getContentDescription());
        assertEquals("Ada selected for Ada Photos", ada.getContentDescription());
        assertTrue(ada.isSelected());
        assertEquals("Suggested", ((TextView) ada.findViewWithTag("assignment_source"))
                .getText().toString());
        assertEquals(null, findFirst(people, CheckBox.class));
    }

    @Test public void manualCorrectionIsClearlyDistinguishedFromAFaceSuggestion() {
        seed();
        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();
        LinearLayout first = (LinearLayout) activity.<LinearLayout>findViewById(
                R.id.album_review_items).getChildAt(0);
        GridLayout people = destinations(first);
        View ben = people.getChildAt(1);

        assertEquals(View.GONE, ben.findViewWithTag("assignment_source").getVisibility());
        ben.performClick();

        TextView source = ben.findViewWithTag("assignment_source");
        assertEquals(View.VISIBLE, source.getVisibility());
        assertEquals("Your choice", source.getText().toString());
    }

    @Test public void independentAlbumAppearsBesidePeopleAndCanBeQueued() {
        seed();
        android.content.Context context = RuntimeEnvironment.getApplication();
        new RegisteredAlbumStore(context).save(List.of(
                new RegisteredAlbum("album-1", "Family adventures", "content://cover/1")));
        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();
        LinearLayout first = (LinearLayout) activity.<LinearLayout>findViewById(
                R.id.album_review_items).getChildAt(0);
        GridLayout destinations = destinations(first);

        assertEquals(4, destinations.getChildCount());
        View album = destinations.getChildAt(3);
        assertEquals("Family adventures", ((TextView) ((ViewGroup) album)
                .getChildAt(1)).getText().toString());
        assertEquals("Family adventures not selected", album.getContentDescription());
        album.performClick();
        assertTrue(new AlbumReviewSelectionStore(activity).load().contains(
                AlbumReviewSelectionStore.key("content://photos/a", "album:album-1")));
        activity.startApprovedQueue();
        assertTrue(new AlbumActionQueueStore(activity).isActive());
    }

    @Test public void selectedDestinationsMoveFirstWithoutChangingNaturalGroupOrder() {
        seed();
        android.content.Context context = RuntimeEnvironment.getApplication();
        RegisteredAlbum album = new RegisteredAlbum(
                "album-1", "Family adventures", "content://cover/1");
        new RegisteredAlbumStore(context).save(List.of(album));
        new AlbumReviewSelectionStore(context).save(Set.of(
                AlbumReviewSelectionStore.key("content://photos/a", "ben"),
                AlbumReviewSelectionStore.key("content://photos/a", "cam"),
                AlbumReviewSelectionStore.key("content://photos/a", "album:album-1")));

        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();
        LinearLayout first = (LinearLayout) activity.<LinearLayout>findViewById(
                R.id.album_review_items).getChildAt(0);
        GridLayout destinations = destinations(first);

        assertEquals("Ben", destinationName(destinations.getChildAt(0)));
        assertEquals("Cam", destinationName(destinations.getChildAt(1)));
        assertEquals("Family adventures", destinationName(destinations.getChildAt(2)));
        assertEquals("Ada", destinationName(destinations.getChildAt(3)));
    }

    @Test public void changingASelectionImmediatelyRestoresSelectedFirstNaturalOrder() {
        seed();
        android.content.Context context = RuntimeEnvironment.getApplication();
        new AlbumReviewSelectionStore(context).save(Set.of(
                AlbumReviewSelectionStore.key("content://photos/a", "ben")));
        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();
        LinearLayout first = (LinearLayout) activity.<LinearLayout>findViewById(
                R.id.album_review_items).getChildAt(0);
        GridLayout destinations = destinations(first);
        assertEquals("Ben", destinationName(destinations.getChildAt(0)));
        assertEquals("Ada", destinationName(destinations.getChildAt(1)));

        destinations.getChildAt(1).performClick();

        assertEquals("Ada", destinationName(destinations.getChildAt(0)));
        assertEquals("Ben", destinationName(destinations.getChildAt(1)));

        destinations.getChildAt(0).performClick();

        assertEquals("Ben", destinationName(destinations.getChildAt(0)));
        assertEquals("Ada", destinationName(destinations.getChildAt(1)));
        assertEquals("Cam", destinationName(destinations.getChildAt(2)));
    }

    @Test public void keeperThumbnailOpensFullscreenForExpressionReview() {
        seed();
        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();
        LinearLayout first = (LinearLayout) activity.<LinearLayout>findViewById(
                R.id.album_review_items).getChildAt(0);
        ImageView keeper = (ImageView) first.getChildAt(0);

        assertEquals("Open Keeper fullscreen", keeper.getContentDescription());
        keeper.performClick();

        Intent opened = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(PreviewActivity.class.getName(), opened.getComponent().getClassName());
        assertEquals("content://photos/a", opened.getDataString());
    }

    @Test public void albumCardsUseCompactFullImageRowsWithoutLetterboxPanels() {
        seed();
        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();
        LinearLayout card = (LinearLayout) activity.<LinearLayout>findViewById(
                R.id.album_review_items).getChildAt(0);
        ImageView photo = (ImageView) card.getChildAt(0);
        HorizontalScrollView destinationScroll = findFirst(card, HorizontalScrollView.class);
        assertNotNull(destinationScroll);
        GridLayout destinations = findFirst(destinationScroll, GridLayout.class);
        ViewGroup firstDestination = (ViewGroup) destinations.getChildAt(0);
        View destinationImage = firstDestination.getChildAt(0);
        float density = activity.getResources().getDisplayMetrics().density;

        assertEquals(LinearLayout.HORIZONTAL, card.getOrientation());
        assertEquals(ImageView.ScaleType.FIT_CENTER, photo.getScaleType());
        assertEquals(Math.round(92 * density), photo.getLayoutParams().width);
        assertEquals(Math.round(112 * density), photo.getLayoutParams().height);
        assertNull(photo.getBackground());
        assertEquals(1, destinations.getRowCount());
        assertTrue(firstDestination.getLayoutParams().width <= Math.round(70 * density));
        assertTrue(destinationImage.getLayoutParams().width <= Math.round(56 * density));
    }

    @Test public void correctionIsPersistedImmediately() {
        seed();
        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();
        LinearLayout first = (LinearLayout) activity.<LinearLayout>findViewById(
                R.id.album_review_items).getChildAt(0);
        TextView confirm = activity.findViewById(R.id.confirm_album_review);

        assertTrue(!(confirm instanceof Button));
        assertEquals("Add 1 album change", confirm.getText().toString());
        assertTrue(confirm.isEnabled());

        destinations(first).getChildAt(0).performClick();

        assertEquals(Set.of(), new AlbumReviewSelectionStore(activity).load());
        assertEquals("No album changes selected", confirm.getText().toString());
        assertTrue(!confirm.isEnabled());
    }

    @Test public void completedPhotoIsHiddenButCanBeShownAndRetried() {
        seed();
        android.content.Context context = RuntimeEnvironment.getApplication();
        new AlbumReviewSelectionStore(context).save(Set.of(
                AlbumReviewSelectionStore.key("content://photos/a", "ada")));
        new AlbumCompletionStore(context).mark("content://photos/a", "Ada Photos");
        new ReviewedPhotoStore(context).mark("content://photos/a");
        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();
        LinearLayout items = activity.findViewById(R.id.album_review_items);
        assertEquals(1, items.getChildCount());
        TextView toggle = activity.findViewById(R.id.toggle_reviewed_albums);
        assertEquals("Show reviewed (1)", toggle.getText().toString());
        toggle.performClick();
        LinearLayout first = (LinearLayout) items.getChildAt(0);
        View ada = destinations(first).getChildAt(0);

        assertTrue(!ada.isSelected());
        assertEquals("Added", ((TextView) ada.findViewWithTag("assignment_source"))
                .getText().toString());
        ada.performClick();
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertEquals("Retry this album change?", ((TextView) dialog.findViewById(
                activity.getResources().getIdentifier("alertTitle", "id", "android")))
                .getText().toString());
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        org.robolectric.shadows.ShadowLooper.idleMainLooper();
        assertTrue(!new AlbumCompletionStore(activity).contains(
                "content://photos/a", "Ada Photos"));
        assertTrue(!new ReviewedPhotoStore(activity).contains("content://photos/a"));
        assertTrue(new AlbumReviewSelectionStore(activity).load().contains(
                AlbumReviewSelectionStore.key("content://photos/a", "ada")));
    }

    @Test public void reviewedPhotoStaysHiddenWhenFaceAnalysisSuggestsAnotherAlbum() {
        seed();
        android.content.Context context = RuntimeEnvironment.getApplication();
        new AlbumCompletionStore(context).mark(
                "content://photos/a", "Family adventures");
        new ReviewedPhotoStore(context).mark("content://photos/a");

        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();

        LinearLayout items = activity.findViewById(R.id.album_review_items);
        assertEquals(1, items.getChildCount());
        assertEquals("Show reviewed (1)", ((TextView) activity.findViewById(
                R.id.toggle_reviewed_albums)).getText().toString());
    }

    @Test public void completedPhotoIsHistoricalEvenWhenLegacyReviewedFlagIsMissing() {
        seed();
        android.content.Context context = RuntimeEnvironment.getApplication();
        new AlbumCompletionStore(context).mark("content://photos/a", "Ada Photos");

        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();

        LinearLayout items = activity.findViewById(R.id.album_review_items);
        assertEquals(1, items.getChildCount());
        TextView toggle = activity.findViewById(R.id.toggle_reviewed_albums);
        assertEquals(View.VISIBLE, toggle.getVisibility());
        assertEquals("Show reviewed (1)", toggle.getText().toString());

        toggle.performClick();
        assertEquals(2, items.getChildCount());
    }

    @Test public void executionStartsOnlyAfterTheExplicitDialogCommand() {
        seed();
        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();

        assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        activity.findViewById(R.id.confirm_album_review).performClick();
        assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertEquals("Add now", dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        activity.startApprovedQueue();

        assertTrue(new AlbumActionQueueStore(activity).isActive());
        assertEquals("content://photos/a",
                Shadows.shadowOf(activity).getNextStartedActivity().getDataString());
    }

    @Test public void disabledAccessibilityStopsBeforeCreatingAQueue() {
        seed();
        android.content.Context context = RuntimeEnvironment.getApplication();
        Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 0);
        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();

        activity.startApprovedQueue();

        assertTrue(!new AlbumActionQueueStore(activity).isActive());
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertEquals("Open accessibility settings",
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
    }

    @Test public void cloudOnlyKeepersAreSuggestedButNeverQueuedAsUploads() {
        seed();
        android.content.Context context = RuntimeEnvironment.getApplication();
        String cloud = "content://com.keepers.photoorganiser.cloud/google-item-1";
        new KeeperSelectionStore(context).replace(Set.of("content://photos/a", cloud));
        new ImportedPhotoStore(context).add(new ImportedPhoto(Uri.parse(cloud), 42,
                PhotoOrigin.CLOUD));
        new AlbumReviewSelectionStore(context).save(Set.of(
                AlbumReviewSelectionStore.key("content://photos/a", "ada"),
                AlbumReviewSelectionStore.key(cloud, "ada")));
        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();

        assertEquals(2, activity.<LinearLayout>findViewById(R.id.album_review_items)
                .getChildCount());
        activity.startApprovedQueue();

        AlbumActionQueueStore queue = new AlbumActionQueueStore(activity);
        assertEquals(1, queue.totalCount());
        assertEquals("content://photos/a", queue.current().photoId());
    }

    @Test public void missingAlbumMappingsGuideTheUserToPeopleSetup() {
        android.content.Context context = RuntimeEnvironment.getApplication();
        new AlbumReviewSelectionStore(context).clear();
        new AlbumCompletionStore(context).clear();
        new KeeperSelectionStore(context).replace(Set.of("content://photos/a"));
        new TrackedPersonStore(context).save(List.of(
                new TrackedPerson("ada", "Ada", "", true)));

        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();

        assertEquals(View.VISIBLE, activity.findViewById(R.id.album_review_setup_people)
                .getVisibility());
        assertTrue(!activity.findViewById(R.id.confirm_album_review).isEnabled());
        assertEquals(0, activity.<LinearLayout>findViewById(R.id.album_review_items)
                .getChildCount());
        activity.findViewById(R.id.album_review_setup_people).performClick();
        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(PeopleActivity.class.getName(), started.getComponent().getClassName());
    }

    @Test public void returningWithAnUnfinishedQueueShowsExplicitRecoveryActions() {
        seed();
        android.content.Context context = RuntimeEnvironment.getApplication();
        AlbumAction current = new AlbumAction("content://photos/a", "Ada", "Ada Photos");
        new AlbumActionQueueStore(context).begin(List.of(current,
                new AlbumAction("content://photos/b", "Ben", "Ben Photos")));

        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();

        TextView status = activity.findViewById(R.id.album_review_run_status);
        assertEquals(View.VISIBLE, status.getVisibility());
        assertEquals("Album changes paused before 1 of 2", status.getText().toString());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.resume_album_review).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.stop_album_review).getVisibility());
    }

    @Test public void resumeRequiresATapAndReopensOnlyTheCurrentApprovedChange() {
        seed();
        android.content.Context context = RuntimeEnvironment.getApplication();
        new AlbumActionQueueStore(context).begin(List.of(
                new AlbumAction("content://photos/a", "Ada", "Ada Photos")));
        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();

        assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        activity.findViewById(R.id.resume_album_review).performClick();

        Intent resumed = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals("content://photos/a", resumed.getDataString());
        assertTrue(context.getSharedPreferences(KeepersAccessibilityService.PREFS,
                android.content.Context.MODE_PRIVATE).getLong(
                        KeepersAccessibilityService.ALBUM_ARMED_UNTIL, 0)
                > System.currentTimeMillis());
    }

    @Test public void stopCancelsTheQueueButKeepsReviewedSelections() {
        seed();
        android.content.Context context = RuntimeEnvironment.getApplication();
        AlbumActionQueueStore queue = new AlbumActionQueueStore(context);
        queue.begin(List.of(new AlbumAction("content://photos/a", "Ada", "Ada Photos")));
        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();

        activity.findViewById(R.id.stop_album_review).performClick();

        assertTrue(!queue.isActive());
        assertEquals(Set.of(AlbumReviewSelectionStore.key("content://photos/a", "ada")),
                new AlbumReviewSelectionStore(context).load());
    }

    private static void seed() {
        android.content.Context context = RuntimeEnvironment.getApplication();
        Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 1);
        Settings.Secure.putString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                new ComponentName(context, KeepersAccessibilityService.class).flattenToString());
        new AlbumReviewSelectionStore(context).clear();
        new AlbumCompletionStore(context).clear();
        new AlbumActionQueueStore(context).cancel();
        new ReviewedPhotoStore(context).clear();
        new RegisteredAlbumStore(context).save(List.of());
        new KeeperSelectionStore(context).replace(Set.of("content://photos/a", "content://photos/b"));
        new TrackedPersonStore(context).save(List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", true),
                new TrackedPerson("ben", "Ben", "Ben Photos", true),
                new TrackedPerson("cam", "Cam", "Cam Photos", true)));
        new FaceObservationStore(context).save("content://photos/a", List.of(
                new FaceObservation("content://photos/a", 0, 0, 0, 1, 1,
                        -1, -1, -1, 0, 0, "1,0")));
        String groupId = FaceClusterer.cluster(new FaceObservationStore(context).loadAll(), .30)
                .stream().filter(group -> group.photoIds().contains("content://photos/a"))
                .findFirst().orElseThrow().id();
        new FaceGroupAssignmentStore(context).save(Map.of(groupId, "ada"));
    }

    @SuppressWarnings("unchecked")
    private static <T extends View> T findFirst(View root, Class<T> type) {
        if (type.isInstance(root)) return (T) root;
        if (root instanceof ViewGroup) for (int index = 0;
                index < ((ViewGroup) root).getChildCount(); index++) {
            T found = findFirst(((ViewGroup) root).getChildAt(index), type);
            if (found != null) return found;
        }
        return null;
    }

    private static GridLayout destinations(View card) {
        return card.findViewWithTag("album_destinations");
    }

    private static String destinationName(View destination) {
        return ((TextView) ((ViewGroup) destination).getChildAt(1)).getText().toString();
    }
}
