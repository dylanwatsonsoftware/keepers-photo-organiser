package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PersonDetailActivityTest {
    @Test public void editsIdentityAndAlbumWithoutASaveButton() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        new TrackedPersonStore(context).save(List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", false)));
        new AlbumReviewSelectionStore(context).save(java.util.Set.of("photo\nada"));
        Activity activity = launch(context, "ada");
        EditText name = activity.findViewById(id(activity, "person_detail_name"));
        EditText album = activity.findViewById(id(activity, "person_detail_album"));

        assertEquals("Ada", name.getText().toString());
        assertEquals("Ada Photos", album.getText().toString());
        name.setText("Ada Watson");
        album.setText("Ada's Album");

        assertEquals(new TrackedPerson("ada", "Ada Watson", "Ada's Album", true),
                new TrackedPersonStore(activity).load().get(0));
        assertEquals(false, new AlbumReviewSelectionStore(activity).hasReview());
        assertEquals(null, findText(activity.findViewById(android.R.id.content), "Save"));
    }

    @Test public void canChooseAFeatureFaceAndRemoveAWrongFace() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String photo = "content://photos/ada-face";
        FaceObservation face = new FaceObservation(photo, 0, .2, .2, .6, .7,
                -1, -1, -1, 0, 0, "1,0,0");
        new TrackedPersonStore(context).save(List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", true)));
        new FaceObservationStore(context).save(photo, List.of(face));
        FaceIdentityGroup group = FaceClusterer.cluster(
                new FaceObservationStore(context).loadAll(), .30).get(0);
        new FaceGroupAssignmentStore(context).save(Map.of(group.id(), "ada"));
        Activity activity = launch(context, "ada");
        LinearLayout faces = activity.findViewById(id(activity, "person_detail_faces"));

        assertEquals(1, faces.getChildCount());
        ImageView crop = findFirst(faces.getChildAt(0), ImageView.class);
        assertNotNull(crop);
        findText(faces.getChildAt(0), "Use as feature").performClick();
        assertEquals(photo + "#0", context.getSharedPreferences(
                "person_feature_faces", Context.MODE_PRIVATE).getString("ada", ""));

        findText(faces.getChildAt(0), "Remove from Ada").performClick();
        assertEquals(FaceCorrectionStore.IGNORE,
                new FaceCorrectionStore(context).load().get(photo + "#0"));
    }

    @Test public void showsFacesLearnedFromConfirmedExamplesForCorrection() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        new TrackedPersonStore(context).save(List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", true)));
        new FaceObservationStore(context).save("content://photos/confirmed", List.of(
                face("content://photos/confirmed", 0, "1,0,0")));
        new FaceObservationStore(context).save("content://photos/suggested", List.of(
                face("content://photos/suggested", 0, ".99,.01,0")));
        new FaceCorrectionStore(context).save(Map.of("content://photos/confirmed#0", "ada"));

        Activity activity = launch(context, "ada");
        LinearLayout faces = activity.findViewById(id(activity, "person_detail_faces"));

        assertEquals(2, faces.getChildCount());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Activity launch(Context context, String personId) throws Exception {
        Class type = Class.forName("com.keepers.photoorganiser.PersonDetailActivity");
        return (Activity) Robolectric.buildActivity(type, new Intent(context, type)
                .putExtra("person_id", personId)).setup().get();
    }

    private static int id(Activity activity, String name) {
        return activity.getResources().getIdentifier(name, "id", activity.getPackageName());
    }

    private static FaceObservation face(String photo, int index, String descriptor) {
        return new FaceObservation(photo, index, .2, .2, .6, .7,
                -1, -1, -1, 0, 0, descriptor);
    }

    private static TextView findText(View root, String text) {
        if (root instanceof TextView && text.equals(((TextView) root).getText().toString()))
            return (TextView) root;
        if (root instanceof ViewGroup) for (int index = 0;
                index < ((ViewGroup) root).getChildCount(); index++) {
            TextView found = findText(((ViewGroup) root).getChildAt(index), text);
            if (found != null) return found;
        }
        return null;
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
