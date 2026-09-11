package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.widget.EditText;
import android.widget.Button;
import android.widget.Switch;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.Spinner;
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
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;
import android.content.Intent;

@RunWith(RobolectricTestRunner.class)
public class PeopleActivityTest {
    @Test public void settingsOffersStyledDeviceAndGooglePhotosImports() {
        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();

        View device = activity.findViewById(R.id.settings_import_device);
        View google = activity.findViewById(R.id.settings_import_google_photos);
        assertTrue(device instanceof TextView);
        assertTrue(google instanceof TextView);
        assertTrue(!(device instanceof Button));
        assertTrue(!(google instanceof Button));
        assertTrue(device.isClickable());
        assertTrue(google.isClickable());
        assertTrue(device.getBackground() != null);
        assertTrue(google.getBackground() != null);
    }

    @Test public void settingsOffersAStyledFeedbackExport() {
        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();

        View export = activity.findViewById(R.id.settings_export_feedback);

        assertTrue(export instanceof TextView);
        assertTrue(!(export instanceof Button));
        assertTrue(export.isClickable());
        assertTrue(export.getBackground() != null);
    }

    @Test public void settingsOffersOptInAutomaticFeedbackSharing() {
        android.content.Context context = org.robolectric.RuntimeEnvironment.getApplication();
        FeedbackSyncPreferences preferences = new FeedbackSyncPreferences(context);
        preferences.clear();
        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();

        TextView toggle = activity.findViewById(R.id.settings_feedback_sync);

        assertTrue(!(toggle instanceof Button));
        assertTrue(toggle.isClickable());
        assertTrue(toggle.getBackground() != null);
        assertEquals("Automatic sharing: Off", toggle.getText().toString());
        toggle.performClick();
        assertTrue(preferences.isEnabled());
        assertEquals("Automatic sharing: On", toggle.getText().toString());
        assertTrue(((TextView) activity.findViewById(R.id.settings_feedback_sync_status))
                .getText().toString().contains("network"));
    }

    @Test public void settingsFeedbackExportSharesPrivacySafeJson() {
        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();
        new RecommendationFeedbackStore(activity).clear();
        new RecommendationFeedbackStore(activity).save(RecommendationFeedback.from(
                new PhotoFeatures("content://private/settings-photo", 123, 456, .8),
                RecommendationFeedback.LOVED, "Great expression"));

        activity.findViewById(R.id.settings_export_feedback).performClick();

        Intent chooser = Shadows.shadowOf(activity).getNextStartedActivity();
        Intent share = chooser.getParcelableExtra(Intent.EXTRA_INTENT);
        assertEquals(Intent.ACTION_SEND, share.getAction());
        assertEquals("application/json", share.getType());
        assertTrue(share.getStringExtra(Intent.EXTRA_TEXT).contains("Great expression"));
        assertTrue(!share.getStringExtra(Intent.EXTRA_TEXT).contains("content://"));
    }

    @Test public void settingsCanRestoreHiddenPhotos() {
        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();
        new HiddenPhotoStore(activity).hide(Set.of("hidden"));
        activity.onResume();

        activity.findViewById(R.id.settings_restore_hidden).performClick();

        assertEquals(Set.of(), new HiddenPhotoStore(activity).load());
    }

    @Test public void settingsImportChoicesReturnToTheReviewPickerFlow() {
        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();

        activity.findViewById(R.id.settings_import_device).performClick();
        Intent device = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(ReviewActivity.class.getName(), device.getComponent().getClassName());
        assertEquals(ReviewActivity.ACTION_IMPORT_DEVICE_PHOTOS, device.getAction());
        assertTrue((device.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0);

        activity.findViewById(R.id.settings_import_google_photos).performClick();
        Intent google = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(ReviewActivity.ACTION_IMPORT_GOOGLE_PHOTOS, google.getAction());
    }

    @Test public void canAddMoreThanThreePeople() {
        android.content.Context context = org.robolectric.RuntimeEnvironment.getApplication();
        new TrackedPersonStore(context).save(List.of(
                new TrackedPerson("person-1", "Ada", "", true),
                new TrackedPerson("person-2", "Ben", "", true),
                new TrackedPerson("person-3", "Cam", "", true)));
        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();

        View add = findViewWithText(activity.findViewById(android.R.id.content), "Add another person");
        assertEquals(true, add != null);
        assertEquals(false, add instanceof Button);
        assertEquals(true, add.getBackground() != null);
        add.performClick();

        LinearLayout profiles = findContainerWithContentDescription(
                activity.findViewById(android.R.id.content), "Tracked people");
        assertEquals(4, profiles.getChildCount());
        assertEquals(4, new TrackedPersonStore(activity).load().size());
    }

    @Test public void canRegisterAnAlbumThatIsNotTiedToAPerson() {
        android.content.Context context = org.robolectric.RuntimeEnvironment.getApplication();
        new RegisteredAlbumStore(context).save(List.of());
        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();

        activity.findViewById(R.id.add_other_album).performClick();

        assertEquals(1, new RegisteredAlbumStore(activity).load().size());
        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(AlbumDetailActivity.class.getName(), started.getComponent().getClassName());
        assertEquals("album-1", started.getStringExtra(AlbumDetailActivity.EXTRA_ALBUM_ID));
    }

    @Test public void advancedLinkKeepsTheProofOfConceptToolsAvailable() {
        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();

        activity.findViewById(R.id.open_advanced_settings).performClick();

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(MainActivity.class.getName(), started.getComponent().getClassName());
        assertEquals("‹  Gallery", ((android.widget.TextView) activity.findViewById(
                R.id.people_back)).getText().toString());
    }

    @Test public void peopleSetupHasNoManualSaveStep() {
        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();

        assertEquals(null, findViewWithText(activity.findViewById(android.R.id.content),
                "Save people and albums"));
        assertEquals("People and face choices save automatically.", findFirstWithId(
                activity.findViewById(android.R.id.content), R.id.people_status)
                .getText().toString());
    }

    @Test public void savedPeopleAppearAsCompactFaceAndNameCards() {
        android.content.Context context = org.robolectric.RuntimeEnvironment.getApplication();
        new TrackedPersonStore(context).save(List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", false)));
        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();
        LinearLayout profiles = findContainerWithContentDescription(
                activity.findViewById(android.R.id.content), "Tracked people");
        View card = profiles.getChildAt(0);

        assertEquals(null, findFirst(card, EditText.class));
        assertEquals(null, findFirst(card, Switch.class));
        assertEquals("Ada", findFirst(card, TextView.class).getText().toString());
        assertEquals("Edit Ada", card.getContentDescription());

        card.performClick();
        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals("com.keepers.photoorganiser.PersonDetailActivity",
                started.getComponent().getClassName());
        assertEquals("ada", started.getStringExtra("person_id"));
    }

    @Test public void showsFaceObservationProgressFromGalleryAnalysis() {
        FaceObservationStore observations = new FaceObservationStore(
                org.robolectric.RuntimeEnvironment.getApplication());
        observations.save("photo", List.of(new FaceObservation("photo", 0,
                0.1, 0.1, 0.2, 0.2, 0.5, 0.8, 0.8, 0, 0)));

        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();

        assertEquals("1 face observation ready for grouping",
                ((android.widget.TextView) activity.findViewById(R.id.face_discovery_status))
                        .getText().toString());
    }

    @Test public void showsDiscoveredRecurringFaceGroupsForReview() {
        FaceObservationStore observations = new FaceObservationStore(
                org.robolectric.RuntimeEnvironment.getApplication());
        observations.save("content://photos/a", List.of(face("content://photos/a", "1,0,0")));
        observations.save("content://photos/b", List.of(face("content://photos/b", ".99,.01,0")));

        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();

        LinearLayout groups = activity.findViewById(R.id.discovered_face_groups);
        assertEquals(0, groups.getChildCount());
        activity.findViewById(R.id.load_face_groups).performClick();
        assertEquals(1, groups.getChildCount());
        assertEquals("Seen in 2 photos", groups.getChildAt(0).getContentDescription());
    }

    @Test public void discoveredGroupsShowMostFrequentlySeenFirst() {
        FaceObservationStore observations = new FaceObservationStore(
                org.robolectric.RuntimeEnvironment.getApplication());
        observations.save("content://photos/small", List.of(
                face("content://photos/small", "1,0,0")));
        observations.save("content://photos/large-a", List.of(
                face("content://photos/large-a", "0,1,0")));
        observations.save("content://photos/large-b", List.of(
                face("content://photos/large-b", "0,.99,.01")));
        observations.save("content://photos/large-c", List.of(
                face("content://photos/large-c", "0,.98,.02")));

        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();

        LinearLayout groups = activity.findViewById(R.id.discovered_face_groups);
        activity.findViewById(R.id.load_face_groups).performClick();
        assertEquals("Seen in 3 photos", groups.getChildAt(0).getContentDescription());
        assertEquals("Seen in 1 photo", groups.getChildAt(1).getContentDescription());
    }

    @Test public void discoveryDefaultsToGroupsNeedingFeedbackAndCanRevealConfirmedOnes() {
        android.content.Context context = org.robolectric.RuntimeEnvironment.getApplication();
        FaceObservationStore observations = new FaceObservationStore(context);
        observations.save("content://photos/confirmed", List.of(
                face("content://photos/confirmed", "1,0,0")));
        observations.save("content://photos/new", List.of(
                face("content://photos/new", "0,1,0")));
        List<FaceIdentityGroup> clustered = FaceClusterer.cluster(observations.loadAll(), .30);
        String confirmed = clustered.stream()
                .filter(group -> group.photoIds().contains("content://photos/confirmed"))
                .findFirst().orElseThrow().id();
        new FaceGroupAssignmentStore(context).save(Map.of(confirmed, "ada"));

        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();
        activity.findViewById(R.id.load_face_groups).performClick();
        LinearLayout groups = activity.findViewById(R.id.discovered_face_groups);
        TextView toggle = activity.findViewById(R.id.toggle_confirmed_faces);

        assertEquals(1, groups.getChildCount());
        assertEquals("Seen in 1 photo", groups.getChildAt(0).getContentDescription());
        assertEquals("Show confirmed (1)", toggle.getText().toString());
        toggle.performClick();
        assertEquals(2, groups.getChildCount());
        assertEquals("Hide confirmed", toggle.getText().toString());
    }

    @Test public void personChoicesShowTheSavedNameAndAFacePhoto() {
        android.content.Context context = org.robolectric.RuntimeEnvironment.getApplication();
        new TrackedPersonStore(context).save(List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", true)));
        FaceObservationStore observations = new FaceObservationStore(context);
        observations.save("content://photos/ada", List.of(
                face("content://photos/ada", "1,0,0")));
        FaceIdentityGroup group = FaceClusterer.cluster(observations.loadAll(), .30).get(0);
        new FaceGroupAssignmentStore(context).save(Map.of(group.id(), "ada"));

        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();
        activity.findViewById(R.id.load_face_groups).performClick();
        activity.findViewById(R.id.toggle_confirmed_faces).performClick();
        LinearLayout groups = activity.findViewById(R.id.discovered_face_groups);
        Spinner chooser = findFirst(groups.getChildAt(0), Spinner.class);
        View personChoice = chooser.getAdapter().getDropDownView(1, null, chooser);

        assertEquals("Ada", findFirst(personChoice, TextView.class).getText().toString());
        assertEquals(true, findFirst(personChoice, ImageView.class) != null);
    }

    @Test public void choosingAnotherFaceGroupDoesNotCrashAfterPortraitLoading() {
        android.content.Context context = org.robolectric.RuntimeEnvironment.getApplication();
        new TrackedPersonStore(context).save(List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", true)));
        FaceObservationStore observations = new FaceObservationStore(context);
        observations.save("content://photos/confirmed", List.of(
                face("content://photos/confirmed", "1,0,0")));
        observations.save("content://photos/new", List.of(
                face("content://photos/new", "0,0,1")));
        List<FaceIdentityGroup> clustered = FaceClusterer.cluster(observations.loadAll(), .30);
        String confirmedGroup = clustered.stream()
                .filter(group -> group.photoIds().contains("content://photos/confirmed"))
                .findFirst().orElseThrow().id();
        new FaceGroupAssignmentStore(context).save(Map.of(confirmedGroup, "ada"));
        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();
        activity.findViewById(R.id.load_face_groups).performClick();
        LinearLayout groups = activity.findViewById(R.id.discovered_face_groups);
        Spinner unassigned = null;
        for (int index = 0; index < groups.getChildCount(); index++) {
            Spinner candidate = findFirst(groups.getChildAt(index), Spinner.class);
            if (candidate.getSelectedItemPosition() == 0) unassigned = candidate;
        }

        unassigned.setSelection(1);
        ShadowLooper.idleMainLooper();

        assertEquals(2, new FaceGroupAssignmentStore(activity).load().size());
        assertEquals("ada", new FaceCorrectionStore(activity).load()
                .get("content://photos/new#0"));
    }

    @Test public void tappingAGroupOpensAllOfItsFacesForVerification() {
        FaceObservationStore observations = new FaceObservationStore(
                org.robolectric.RuntimeEnvironment.getApplication());
        observations.save("content://photos/a", List.of(face("content://photos/a", "1,0,0")));
        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();
        activity.findViewById(R.id.load_face_groups).performClick();

        activity.<LinearLayout>findViewById(R.id.discovered_face_groups).getChildAt(0).performClick();

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(FaceGroupReviewActivity.class.getName(), started.getComponent().getClassName());
        assertEquals(FaceClusterer.cluster(observations.loadAll(), .30).get(0).id(),
                started.getStringExtra(FaceGroupReviewActivity.EXTRA_GROUP_ID));
    }

    @Test public void likelyPersonButtonConfirmsAHighConfidenceUnreviewedGroup() {
        android.content.Context context = org.robolectric.RuntimeEnvironment.getApplication();
        new TrackedPersonStore(context).save(List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", true)));
        FaceObservationStore observations = new FaceObservationStore(context);
        observations.save("content://photos/known", List.of(
                face("content://photos/known", "1,0")));
        observations.save("content://photos/candidate-a", List.of(
                face("content://photos/candidate-a", ".65,.76")));
        observations.save("content://photos/candidate-b", List.of(
                face("content://photos/candidate-b", ".66,.75")));
        List<FaceIdentityGroup> groups = FaceClusterer.cluster(observations.loadAll(), .30);
        FaceIdentityGroup known = groups.stream().filter(group ->
                group.photoIds().contains("content://photos/known")).findFirst().orElseThrow();
        FaceIdentityGroup candidate = groups.stream().filter(group ->
                group.photoIds().contains("content://photos/candidate-a"))
                .findFirst().orElseThrow();
        new FaceGroupAssignmentStore(context).save(Map.of(known.id(), "ada"));

        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();
        activity.findViewById(R.id.load_face_groups).performClick();
        View suggestion = findViewWithText(activity.findViewById(android.R.id.content),
                "Likely Ada");

        assertEquals(true, suggestion != null);
        assertEquals(true, suggestion.isClickable());
        suggestion.performClick();
        ShadowLooper.idleMainLooper();
        assertEquals("ada", new FaceGroupAssignmentStore(activity).load().get(candidate.id()));
    }

    @Test public void canDeclineALikelyPersonForEveryFaceInTheSuggestedGroup() {
        android.content.Context context = org.robolectric.RuntimeEnvironment.getApplication();
        new TrackedPersonStore(context).save(List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", true)));
        FaceObservationStore observations = new FaceObservationStore(context);
        observations.save("content://photos/known", List.of(
                face("content://photos/known", "1,0")));
        observations.save("content://photos/candidate-a", List.of(
                face("content://photos/candidate-a", ".65,.76")));
        observations.save("content://photos/candidate-b", List.of(
                face("content://photos/candidate-b", ".66,.75")));
        List<FaceIdentityGroup> groups = FaceClusterer.cluster(observations.loadAll(), .30);
        FaceIdentityGroup known = groups.stream().filter(group ->
                group.photoIds().contains("content://photos/known")).findFirst().orElseThrow();
        FaceIdentityGroup candidate = groups.stream().filter(group ->
                group.photoIds().contains("content://photos/candidate-a"))
                .findFirst().orElseThrow();
        new FaceGroupAssignmentStore(context).save(Map.of(known.id(), "ada"));
        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();
        activity.findViewById(R.id.load_face_groups).performClick();

        View decline = findViewWithText(activity.findViewById(android.R.id.content), "Not Ada");
        assertEquals(true, decline != null);
        assertEquals(false, decline instanceof Button);
        decline.performClick();
        ShadowLooper.idleMainLooper();

        FaceSuggestionRejectionStore rejections = new FaceSuggestionRejectionStore(activity);
        for (FaceObservation member : candidate.members()) assertEquals(true,
                rejections.isRejected(FaceCorrectionStore.key(member), "ada"));
        assertEquals(null, findViewWithText(activity.findViewById(android.R.id.content),
                "Likely Ada"));
    }

    private static FaceObservation face(String photo, String descriptor) {
        return new FaceObservation(photo, 0, 0, 0, 1, 1,
                -1, -1, -1, 0, 0, descriptor);
    }

    private static View findViewWithText(View view, String text) {
        if (view instanceof TextView && text.equals(((TextView) view).getText().toString())) return view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            View found = findViewWithText(((ViewGroup) view).getChildAt(i), text);
            if (found != null) return found;
        }
        return null;
    }

    private static LinearLayout findContainerWithContentDescription(View view, String description) {
        if (view instanceof LinearLayout && view.getContentDescription() != null
                && description.contentEquals(view.getContentDescription()))
            return (LinearLayout) view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            LinearLayout found = findContainerWithContentDescription(
                    ((ViewGroup) view).getChildAt(i), description);
            if (found != null) return found;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends View> T findFirst(View view, Class<T> type) {
        if (type.isInstance(view)) return (T) view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            T found = findFirst(((ViewGroup) view).getChildAt(i), type);
            if (found != null) return found;
        }
        return null;
    }

    private static TextView findFirstWithId(View view, int id) {
        return view.findViewById(id);
    }
}
