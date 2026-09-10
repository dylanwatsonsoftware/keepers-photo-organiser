package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.TextView;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowDialog;

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
        grid.getChildAt(0).performClick();
        Intent started = org.robolectric.Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(PersonDetailActivity.class.getName(), started.getComponent().getClassName());
        assertEquals("ada", started.getStringExtra(PersonDetailActivity.EXTRA_PERSON_ID));
    }

    @Test public void tappingAnUnknownFaceCanConfirmItAndTeachFutureMatches() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String photo = "content://photos/confirm-face";
        new FaceObservationStore(context).save(photo, List.of(
                face(photo, 0, .10, "1,0,0")));
        new TrackedPersonStore(context).save(List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", true)));
        new FaceCorrectionStore(context).save(Map.of());
        PreviewActivity activity = Robolectric.buildActivity(PreviewActivity.class,
                new Intent(context, PreviewActivity.class).setData(Uri.parse(photo))).setup().get();

        Method showAnalysis = PreviewActivity.class.getDeclaredMethod("showAnalysis");
        showAnalysis.setAccessible(true);
        showAnalysis.invoke(activity);
        GridLayout grid = activity.findViewById(R.id.preview_analysis_faces);
        assertTrue(grid.getChildAt(0).isClickable());
        grid.getChildAt(0).performClick();
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        dialog.getListView().performItemClick(null, 0, 0);

        assertEquals("ada", new FaceCorrectionStore(activity).load().get(photo + "#0"));
        assertEquals("Ada", label(grid, 0));
        FaceObservation future = face("content://photos/future", 0, .10, ".99,.01,0");
        assertEquals("ada", FaceIdentityLearner.predict(List.of(
                        new FaceObservationStore(activity).load(photo).get(0), future),
                new FaceCorrectionStore(activity).load(), .15).get("content://photos/future#0"));
    }

    @Test public void tappingALearnedChildOpensTheirAssociatedFacesScreen() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String confirmedPhoto = "content://photos/confirmed-child";
        String currentPhoto = "content://photos/suggested-child";
        new FaceObservationStore(context).save(confirmedPhoto, List.of(
                face(confirmedPhoto, 0, .10, "1,0,0")));
        new FaceObservationStore(context).save(currentPhoto, List.of(
                face(currentPhoto, 0, .10, ".99,.01,0")));
        new TrackedPersonStore(context).save(List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", true)));
        new FaceCorrectionStore(context).save(Map.of(confirmedPhoto + "#0", "ada"));
        PreviewActivity activity = Robolectric.buildActivity(PreviewActivity.class,
                new Intent(context, PreviewActivity.class).setData(Uri.parse(currentPhoto)))
                .setup().get();

        Method showAnalysis = PreviewActivity.class.getDeclaredMethod("showAnalysis");
        showAnalysis.setAccessible(true);
        showAnalysis.invoke(activity);
        GridLayout grid = activity.findViewById(R.id.preview_analysis_faces);
        assertEquals("Ada\nSuggested", label(grid, 0));
        grid.getChildAt(0).performClick();

        Intent started = org.robolectric.Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull(started);
        assertEquals(PersonDetailActivity.class.getName(), started.getComponent().getClassName());
        assertEquals("ada", started.getStringExtra(PersonDetailActivity.EXTRA_PERSON_ID));
    }

    @Test public void suggestedPersonCanBeConfirmedDirectlyOnTheFaceCard() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String confirmedPhoto = "content://photos/confirmed-person";
        String suggestedPhoto = "content://photos/suggested-person";
        new FaceObservationStore(context).save(confirmedPhoto, List.of(
                face(confirmedPhoto, 0, .10, "1,0,0")));
        new FaceObservationStore(context).save(suggestedPhoto, List.of(
                face(suggestedPhoto, 0, .10, ".99,.01,0")));
        new TrackedPersonStore(context).save(List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", true)));
        new FaceCorrectionStore(context).save(Map.of(confirmedPhoto + "#0", "ada"));
        PreviewActivity activity = Robolectric.buildActivity(PreviewActivity.class,
                new Intent(context, PreviewActivity.class).setData(Uri.parse(suggestedPhoto)))
                .setup().get();

        Method showAnalysis = PreviewActivity.class.getDeclaredMethod("showAnalysis");
        showAnalysis.setAccessible(true);
        showAnalysis.invoke(activity);
        GridLayout grid = activity.findViewById(R.id.preview_analysis_faces);
        TextView confirm = grid.getChildAt(0).findViewWithTag("confirm_face_identity");
        TextView change = grid.getChildAt(0).findViewWithTag("change_face_identity");

        assertNotNull(confirm);
        assertNotNull(change);
        assertEquals("Yes", confirm.getText().toString());
        confirm.performClick();

        assertEquals("ada", new FaceCorrectionStore(activity).load()
                .get(suggestedPhoto + "#0"));
        assertEquals("Ada", label(grid, 0));
    }

    @Test public void suggestedPersonCanBeRejectedWithoutIgnoringTheFace() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String known = "content://photos/reject-known";
        String suggested = "content://photos/reject-suggested";
        new FaceObservationStore(context).save(known, List.of(face(known, 0, .10, "1,0,0")));
        new FaceObservationStore(context).save(suggested,
                List.of(face(suggested, 0, .10, ".99,.01,0")));
        new TrackedPersonStore(context).save(List.of(
                new TrackedPerson("ada", "Ada", "Ada Photos", true)));
        new FaceCorrectionStore(context).save(Map.of(known + "#0", "ada"));
        PreviewActivity activity = Robolectric.buildActivity(PreviewActivity.class,
                new Intent(context, PreviewActivity.class).setData(Uri.parse(suggested)))
                .setup().get();
        Method showAnalysis = PreviewActivity.class.getDeclaredMethod("showAnalysis");
        showAnalysis.setAccessible(true);
        showAnalysis.invoke(activity);
        GridLayout grid = activity.findViewById(R.id.preview_analysis_faces);
        TextView reject = grid.getChildAt(0).findViewWithTag("reject_face_identity");

        assertNotNull(reject);
        assertEquals("No", reject.getText().toString());
        reject.performClick();

        assertEquals("Unknown", label(grid, 0));
        assertTrue(new FaceSuggestionRejectionStore(activity)
                .isRejected(suggested + "#0", "ada"));
        assertTrue(!new FaceCorrectionStore(activity).load().containsKey(suggested + "#0"));
    }

    @Test public void unknownFaceCanCreateAndTeachANewTrackedPersonInPlace() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String photo = "content://photos/new-person-face";
        new FaceObservationStore(context).save(photo, List.of(
                face(photo, 0, .10, "1,0,0")));
        new TrackedPersonStore(context).save(List.of());
        new FaceCorrectionStore(context).save(Map.of());
        PreviewActivity activity = Robolectric.buildActivity(PreviewActivity.class,
                new Intent(context, PreviewActivity.class).setData(Uri.parse(photo))).setup().get();

        Method showAnalysis = PreviewActivity.class.getDeclaredMethod("showAnalysis");
        showAnalysis.setAccessible(true);
        showAnalysis.invoke(activity);
        GridLayout grid = activity.findViewById(R.id.preview_analysis_faces);
        grid.getChildAt(0).performClick();
        AlertDialog chooser = (AlertDialog) ShadowDialog.getLatestDialog();
        chooser.getListView().performItemClick(null, 0, 0);

        AlertDialog create = (AlertDialog) ShadowDialog.getLatestDialog();
        EditText name = create.getWindow().getDecorView().findViewWithTag("new_person_name");
        EditText album = create.getWindow().getDecorView().findViewWithTag("new_person_album");
        name.setText("Charlie");
        album.setText("Charlie Photos");
        create.getWindow().getDecorView().findViewWithTag("add_new_person").performClick();

        TrackedPerson person = new TrackedPersonStore(activity).load().get(0);
        assertEquals("Charlie", person.name());
        assertEquals("Charlie Photos", person.albumName());
        assertEquals(person.id(), new FaceCorrectionStore(activity).load().get(photo + "#0"));
        assertEquals(photo + "#0", new PersonFeatureFaceStore(activity).load(person.id()));
        assertEquals("Charlie", label(grid, 0));
    }

    @Test public void recommendedAssessmentHeadingDisplaysItsStar() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String photo = "content://photos/recommended-heading";
        new PhotoInsightStore(context).save(List.of(new PhotoFeatures(photo, 0, 0, .8)),
                Map.of(), Set.of(photo));
        PreviewActivity activity = Robolectric.buildActivity(PreviewActivity.class,
                new Intent(context, PreviewActivity.class).setData(Uri.parse(photo))).setup().get();

        Method showAnalysis = PreviewActivity.class.getDeclaredMethod("showAnalysis");
        showAnalysis.setAccessible(true);
        showAnalysis.invoke(activity);

        TextView heading = activity.findViewById(R.id.preview_analysis_title);
        assertEquals("Recommended best shot", heading.getText().toString());
        assertNotNull(heading.getCompoundDrawablesRelative()[0]);
    }

    @Test public void metadataOnlyShowsAlbumsThatThisPhotoWasSavedTo() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String photo = "content://photos/saved-albums";
        AlbumCompletionStore completions = new AlbumCompletionStore(context);
        completions.clear();
        completions.mark(photo, "Charlie Photos");
        completions.mark(photo, "Family Adventures");
        PreviewActivity activity = Robolectric.buildActivity(PreviewActivity.class,
                new Intent(context, PreviewActivity.class).setData(Uri.parse(photo))).setup().get();

        Method showAnalysis = PreviewActivity.class.getDeclaredMethod("showAnalysis");
        showAnalysis.setAccessible(true);
        showAnalysis.invoke(activity);

        assertEquals(ViewGroup.VISIBLE,
                activity.findViewById(R.id.preview_saved_albums_section).getVisibility());
        assertEquals("Charlie Photos\nFamily Adventures",
                ((TextView) activity.findViewById(R.id.preview_saved_albums)).getText().toString());
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
