package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.widget.CheckBox;
import android.widget.LinearLayout;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AlbumReviewActivityTest {
    @Test public void everyKeeperIsShownAndDetectedPersonStartsSelected() {
        seed();

        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();
        LinearLayout queue = activity.findViewById(R.id.album_review_items);

        assertEquals(2, queue.getChildCount());
        LinearLayout first = (LinearLayout) queue.getChildAt(0);
        CheckBox ada = (CheckBox) first.getChildAt(2);
        assertEquals("Ada · Ada Photos", ada.getText().toString());
        assertTrue(ada.isChecked());
    }

    @Test public void correctionIsPersistedImmediately() {
        seed();
        AlbumReviewActivity activity = Robolectric.buildActivity(AlbumReviewActivity.class)
                .setup().get();
        LinearLayout first = (LinearLayout) activity.<LinearLayout>findViewById(
                R.id.album_review_items).getChildAt(0);

        ((CheckBox) first.getChildAt(2)).performClick();

        assertEquals(Set.of(), new AlbumReviewSelectionStore(activity).load());
    }

    private static void seed() {
        android.content.Context context = RuntimeEnvironment.getApplication();
        new KeeperSelectionStore(context).replace(Set.of("content://photos/a", "content://photos/b"));
        new TrackedPersonStore(context).save(List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", true)));
        new FaceObservationStore(context).save("content://photos/a", List.of(
                new FaceObservation("content://photos/a", 0, 0, 0, 1, 1,
                        -1, -1, -1, 0, 0, "1,0")));
        new FaceGroupAssignmentStore(context).save(Map.of("face-group-1", "ada"));
    }
}
