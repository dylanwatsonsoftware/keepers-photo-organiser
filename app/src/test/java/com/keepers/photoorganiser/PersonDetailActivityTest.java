package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;

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
        assertNotNull(findText(activity.findViewById(android.R.id.content), "Name"));
        assertNotNull(findText(activity.findViewById(android.R.id.content),
                "Google Photos album"));
        name.setText("Ada Watson");
        album.setText("Ada's Album");

        assertEquals(new TrackedPerson("ada", "Ada Watson", "Ada's Album", true),
                new TrackedPersonStore(activity).load().get(0));
        assertEquals(false, new AlbumReviewSelectionStore(activity).hasReview());
        assertEquals(null, findText(activity.findViewById(android.R.id.content), "Save"));
    }

    @Test public void canChooseAFeatureFaceAndRemoveAWrongFace() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String firstPhoto = "content://photos/ada-a";
        String secondPhoto = "content://photos/ada-b";
        new TrackedPersonStore(context).save(List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", true)));
        FaceObservationStore observations = new FaceObservationStore(context);
        observations.save(firstPhoto, List.of(face(firstPhoto, 0, "1,0,0")));
        observations.save(secondPhoto, List.of(face(secondPhoto, 0, ".99,.01,0")));
        FaceIdentityGroup group = FaceClusterer.cluster(
                observations.loadAll(), .30).get(0);
        new FaceGroupAssignmentStore(context).save(Map.of(group.id(), "ada"));
        Activity activity = launch(context, "ada");
        GridLayout faces = activity.findViewById(id(activity, "person_detail_faces"));

        assertEquals(2, faces.getColumnCount());
        assertEquals(2, faces.getChildCount());
        assertEquals(0, faces.getChildAt(0).getLayoutParams().width);
        ImageView crop = findFirst(faces.getChildAt(0), ImageView.class);
        assertNotNull(crop);
        TextView current = findText(faces.getChildAt(0), "Current feature photo");
        assertNotNull(current);
        assertFalse(current.isEnabled());
        assertFalse(current.isClickable());
        assertNull(findText(faces.getChildAt(0), "Use as feature"));
        TextView choose = findText(faces.getChildAt(1), "Use as feature");
        assertTrue(choose.isEnabled());
        assertTrue(choose.isClickable());
        choose.performClick();
        assertEquals(secondPhoto + "#0", context.getSharedPreferences(
                "person_feature_faces", Context.MODE_PRIVATE).getString("ada", ""));
        assertNotNull(findText(faces.getChildAt(1), "Current feature photo"));
        assertNull(findText(faces.getChildAt(1), "Use as feature"));

        ImageButton remove = findFirst(faces.getChildAt(1), ImageButton.class);
        assertNotNull(remove);
        assertEquals("Remove face from Ada", remove.getContentDescription().toString());
        remove.performClick();
        assertNull(new FaceCorrectionStore(context).load().get(secondPhoto + "#0"));
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertEquals("Remove", dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();
        assertEquals(FaceCorrectionStore.IGNORE,
                new FaceCorrectionStore(context).load().get(secondPhoto + "#0"));
        assertEquals(1, faces.getChildCount());
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
        GridLayout faces = activity.findViewById(id(activity, "person_detail_faces"));

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
