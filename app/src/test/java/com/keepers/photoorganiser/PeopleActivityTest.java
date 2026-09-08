package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.widget.EditText;
import android.widget.Switch;
import android.widget.LinearLayout;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import android.content.Intent;

@RunWith(RobolectricTestRunner.class)
public class PeopleActivityTest {
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
        ((EditText) activity.findViewById(R.id.person_1_name)).setText("Ada");
        ((EditText) activity.findViewById(R.id.person_1_album)).setText("Ada Photos");
        ((Switch) activity.findViewById(R.id.person_1_tracked)).setChecked(true);

        activity.findViewById(R.id.save_people).performClick();

        assertEquals(List.of(new TrackedPerson("person-1", "Ada", "Ada Photos", true),
                        new TrackedPerson("person-2", "", "", false),
                        new TrackedPerson("person-3", "", "", false)),
                new TrackedPersonStore(activity).load());
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
}
