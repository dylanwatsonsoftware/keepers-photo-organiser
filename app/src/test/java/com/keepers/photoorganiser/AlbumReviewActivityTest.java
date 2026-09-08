package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import android.widget.CheckBox;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.GridLayout;
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

@RunWith(RobolectricTestRunner.class)
public class AlbumReviewActivityTest {
    @Test public void everyKeeperIsShownAndDetectedPersonStartsSelected() {
        seed();

        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();
        LinearLayout queue = activity.findViewById(R.id.album_review_items);

        assertEquals(2, queue.getChildCount());
        LinearLayout first = (LinearLayout) queue.getChildAt(0);
        GridLayout people = (GridLayout) first.getChildAt(2);
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
        GridLayout people = (GridLayout) first.getChildAt(2);
        View ben = people.getChildAt(1);

        assertEquals(View.GONE, ben.findViewWithTag("assignment_source").getVisibility());
        ben.performClick();

        TextView source = ben.findViewWithTag("assignment_source");
        assertEquals(View.VISIBLE, source.getVisibility());
        assertEquals("Your choice", source.getText().toString());
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

        ((GridLayout) first.getChildAt(2)).getChildAt(0).performClick();

        assertEquals(Set.of(), new AlbumReviewSelectionStore(activity).load());
        assertEquals("No album changes selected", confirm.getText().toString());
        assertTrue(!confirm.isEnabled());
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

    @Test public void missingAlbumMappingsGuideTheUserToPeopleSetup() {
        android.content.Context context = RuntimeEnvironment.getApplication();
        new AlbumReviewSelectionStore(context).clear();
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

    private static void seed() {
        android.content.Context context = RuntimeEnvironment.getApplication();
        Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 1);
        Settings.Secure.putString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                new ComponentName(context, KeepersAccessibilityService.class).flattenToString());
        new AlbumReviewSelectionStore(context).clear();
        new AlbumActionQueueStore(context).cancel();
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
}
