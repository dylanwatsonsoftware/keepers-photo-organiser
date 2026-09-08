package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.widget.EditText;
import android.widget.Switch;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PeopleActivityTest {
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
}
