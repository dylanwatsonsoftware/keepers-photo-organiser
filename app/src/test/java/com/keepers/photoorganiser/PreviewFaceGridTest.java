package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PreviewFaceGridTest {
    @Test public void metadataShowsEveryFaceWithConfirmedOrUnknownName() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String photo = "content://photos/face-grid";
        new FaceObservationStore(context).save(photo, List.of(
                face(photo, 0, .10, "1,0,0"), face(photo, 1, .60, "0,0,1")));
        new TrackedPersonStore(context).save(List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", true)));
        new FaceCorrectionStore(context).save(Map.of(photo + "#0", "ada"));
        PreviewActivity activity = Robolectric.buildActivity(PreviewActivity.class,
                new Intent(context, PreviewActivity.class).setData(Uri.parse(photo))).setup().get();

        Method showAnalysis = PreviewActivity.class.getDeclaredMethod("showAnalysis");
        showAnalysis.setAccessible(true);
        showAnalysis.invoke(activity);

        int id = activity.getResources().getIdentifier("preview_analysis_faces", "id",
                activity.getPackageName());
        GridLayout grid = activity.findViewById(id);
        assertNotNull(grid);
        assertEquals(2, grid.getChildCount());
        assertEquals("Ada", label(grid, 0));
        assertEquals("Unknown", label(grid, 1));
        ImageView firstFace = (ImageView) ((ViewGroup) grid.getChildAt(0)).getChildAt(0);
        assertEquals("Expanded face crop for Ada", firstFace.getContentDescription());
    }

    private static String label(GridLayout grid, int index) {
        return ((TextView) ((ViewGroup) grid.getChildAt(index)).getChildAt(1))
                .getText().toString();
    }

    private static FaceObservation face(String photo, int index, double left,
            String descriptor) {
        return new FaceObservation(photo, index, left, .2, left + .25, .65,
                -1, -1, -1, 0, 0, descriptor);
    }
}
