package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.Spinner;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class FaceGroupReviewActivityTest {
    @Test public void showsEveryFaceAndAllowsOneToBeIgnored() {
        android.content.Context context = RuntimeEnvironment.getApplication();
        new TrackedPersonStore(context).save(List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", true)));
        FaceObservationStore observations = new FaceObservationStore(context);
        observations.save("content://photos/a", List.of(face("content://photos/a", "1,0")));
        observations.save("content://photos/b", List.of(face("content://photos/b", ".99,.01")));
        FaceIdentityGroup group = FaceClusterer.cluster(observations.loadAll(), .30).stream()
                .filter(candidate -> candidate.photoIds().contains("content://photos/a"))
                .findFirst().orElseThrow();
        new FaceGroupAssignmentStore(context).save(Map.of(group.id(), "ada"));
        new FaceCorrectionStore(context).save(Map.of());

        FaceGroupReviewActivity activity = Robolectric.buildActivity(FaceGroupReviewActivity.class,
                new Intent(context, FaceGroupReviewActivity.class)
                        .putExtra(FaceGroupReviewActivity.EXTRA_GROUP_ID, group.id())).setup().get();
        LinearLayout faces = activity.findViewById(R.id.face_group_members);

        assertEquals(2, faces.getChildCount());
        Spinner firstChoice = (Spinner) ((LinearLayout) faces.getChildAt(0)).getChildAt(1);
        assertEquals("Use group choice · Ada", firstChoice.getSelectedItem().toString());
        firstChoice.setSelection(2);
        assertEquals(FaceCorrectionStore.IGNORE,
                new FaceCorrectionStore(activity).load().get("content://photos/a#0"));
    }

    private static FaceObservation face(String photo, String descriptor) {
        return new FaceObservation(photo, 0, 0, 0, 1, 1,
                -1, -1, -1, 0, 0, descriptor);
    }
}
