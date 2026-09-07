package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.net.Uri;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ReviewActivityTest {
    @Test public void selectingPhotoFadesNonKeepersButKeepsEveryTileVisible() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/1"),
                Uri.parse("content://media/photo/2")));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        grid.getChildAt(0).performClick();

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
        grid.getChildAt(0).performClick();

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

    private static String text(ReviewActivity activity, int id) {
        return ((TextView) activity.findViewById(id)).getText().toString();
    }
}
