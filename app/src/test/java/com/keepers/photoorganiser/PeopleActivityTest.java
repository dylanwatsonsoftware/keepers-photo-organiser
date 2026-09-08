package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.widget.EditText;
import android.widget.Switch;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import android.content.Intent;

@RunWith(RobolectricTestRunner.class)
public class PeopleActivityTest {
    @Test public void canAddMoreThanThreePeople() {
        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();

        View add = findViewWithText(activity.findViewById(android.R.id.content), "Add another person");
        assertEquals(true, add != null);
        add.performClick();

        LinearLayout profiles = findContainerWithContentDescription(
                activity.findViewById(android.R.id.content), "Tracked people");
        assertEquals(4, profiles.getChildCount());
    }

    @Test public void advancedLinkKeepsTheProofOfConceptToolsAvailable() {
        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();

        activity.findViewById(R.id.open_advanced_settings).performClick();

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(MainActivity.class.getName(), started.getComponent().getClassName());
        assertEquals("‹  Gallery", ((android.widget.TextView) activity.findViewById(
                R.id.people_back)).getText().toString());
    }

    @Test public void savesThreeEditableTrackedPeopleAndAlbumNames() {
        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();
        LinearLayout profiles = findContainerWithContentDescription(
                activity.findViewById(android.R.id.content), "Tracked people");
        LinearLayout first = (LinearLayout) profiles.getChildAt(0);
        ((EditText) first.findViewWithTag("person_name")).setText("Ada");
        ((EditText) first.findViewWithTag("person_album")).setText("Ada Photos");
        ((Switch) first.findViewWithTag("person_tracked")).setChecked(true);

        activity.findViewById(R.id.save_people).performClick();

        assertEquals(List.of(new TrackedPerson("person-1", "Ada", "Ada Photos", true),
                        new TrackedPerson("person-2", "", "", false),
                        new TrackedPerson("person-3", "", "", false)),
                new TrackedPersonStore(activity).load());
    }

    @Test public void changingPeopleInvalidatesAnEarlierAlbumReview() {
        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();
        new AlbumReviewSelectionStore(activity).save(java.util.Set.of("photo\nperson-1"));

        LinearLayout profiles = findContainerWithContentDescription(
                activity.findViewById(android.R.id.content), "Tracked people");
        ((EditText) profiles.getChildAt(0).findViewWithTag("person_name")).setText("Ada");
        activity.findViewById(R.id.save_people).performClick();

        assertEquals(false, new AlbumReviewSelectionStore(activity).hasReview());
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
        assertEquals("Seen in 3 photos", groups.getChildAt(0).getContentDescription());
        assertEquals("Seen in 1 photo", groups.getChildAt(1).getContentDescription());
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
        LinearLayout groups = activity.findViewById(R.id.discovered_face_groups);
        Spinner chooser = findFirst(groups.getChildAt(0), Spinner.class);
        View personChoice = chooser.getAdapter().getDropDownView(1, null, chooser);

        assertEquals("Ada", findFirst(personChoice, TextView.class).getText().toString());
        assertEquals(true, findFirst(personChoice, ImageView.class) != null);
    }

    @Test public void tappingAGroupOpensAllOfItsFacesForVerification() {
        FaceObservationStore observations = new FaceObservationStore(
                org.robolectric.RuntimeEnvironment.getApplication());
        observations.save("content://photos/a", List.of(face("content://photos/a", "1,0,0")));
        PeopleActivity activity = Robolectric.buildActivity(PeopleActivity.class).setup().get();

        activity.<LinearLayout>findViewById(R.id.discovered_face_groups).getChildAt(0).performClick();

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(FaceGroupReviewActivity.class.getName(), started.getComponent().getClassName());
        assertEquals(FaceClusterer.cluster(observations.loadAll(), .30).get(0).id(),
                started.getStringExtra(FaceGroupReviewActivity.EXTRA_GROUP_ID));
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
}
