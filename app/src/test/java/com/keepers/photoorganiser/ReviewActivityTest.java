package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.net.Uri;
import android.content.Intent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

@RunWith(RobolectricTestRunner.class)
public class ReviewActivityTest {
    @Test public void selectingPhotoFadesNonKeepersButKeepsEveryTileVisible() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/1"),
                Uri.parse("content://media/photo/2")));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        ((android.view.ViewGroup) grid.getChildAt(0)).getChildAt(1).performClick();

        assertEquals("1 keeper", text(activity, R.id.keeper_count));
        assertEquals(2, grid.getChildCount());
        assertEquals(View.VISIBLE, grid.getChildAt(1).getVisibility());
        assertEquals(1f, grid.getChildAt(0).getAlpha(), 0.001f);
        assertEquals(0.38f, grid.getChildAt(1).getAlpha(), 0.001f);
    }

    @Test public void clearRestoresAllPhotosAtFullStrength() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/1"),
                Uri.parse("content://media/photo/2")));
        GridLayout grid = activity.findViewById(R.id.photo_grid);
        ((android.view.ViewGroup) grid.getChildAt(0)).getChildAt(1).performClick();

        activity.findViewById(R.id.clear_keepers).performClick();

        assertEquals("No keepers selected yet", text(activity, R.id.keeper_count));
        assertEquals(1f, grid.getChildAt(0).getAlpha(), 0.001f);
        assertEquals(1f, grid.getChildAt(1).getAlpha(), 0.001f);
    }

    @Test public void suggestionsFadeAlternativesWithoutConfirmingThem() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/1"),
                Uri.parse("content://media/photo/2")));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        activity.showSuggestions(Set.of("content://media/photo/2"));

        assertEquals("1 suggested best shot", text(activity, R.id.suggestion_count));
        assertEquals("No keepers selected yet", text(activity, R.id.keeper_count));
        assertEquals(0.5f, grid.getChildAt(0).getAlpha(), 0.001f);
        assertEquals(1f, grid.getChildAt(1).getAlpha(), 0.001f);
    }

    @Test public void tappingPhotoOpensLargePreview() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        Uri photo = Uri.parse("content://media/photo/1");
        activity.showPhotos(List.of(photo));

        activity.<GridLayout>findViewById(R.id.photo_grid).getChildAt(0).performClick();

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(PreviewActivity.class.getName(), started.getComponent().getClassName());
        assertEquals(photo, started.getData());
    }

    @Test public void loadMoreExpandsTheReviewWindow() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();

        activity.findViewById(R.id.load_more).performClick();

        assertEquals(120, activity.reviewLimit());
    }

    private static String text(ReviewActivity activity, int id) {
        return ((TextView) activity.findViewById(id)).getText().toString();
    }
}
