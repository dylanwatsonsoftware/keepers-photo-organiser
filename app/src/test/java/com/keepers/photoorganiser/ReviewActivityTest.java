package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.net.Uri;
import android.content.Intent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

@RunWith(RobolectricTestRunner.class)
public class ReviewActivityTest {
    @Test public void selectingPhotoKeepsEveryTileAtFullStrength() {
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
        assertEquals(1f, grid.getChildAt(1).getAlpha(), 0.001f);
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

    @Test public void suggestionsUseStarsWithoutFadingAlternatives() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/1"),
                Uri.parse("content://media/photo/2")));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        activity.showSuggestions(Set.of("content://media/photo/2"));

        assertEquals("1 suggested best shot", text(activity, R.id.suggestion_count));
        assertEquals("No keepers selected yet", text(activity, R.id.keeper_count));
        assertEquals(1f, grid.getChildAt(0).getAlpha(), 0.001f);
        assertEquals(1f, grid.getChildAt(1).getAlpha(), 0.001f);
        assertEquals(View.VISIBLE,
                ((android.view.ViewGroup) grid.getChildAt(1)).getChildAt(2).getVisibility());

        ((android.view.ViewGroup) grid.getChildAt(1)).getChildAt(1).performClick();

        assertEquals(View.VISIBLE,
                ((android.view.ViewGroup) grid.getChildAt(1)).getChildAt(2).getVisibility());
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

    @Test public void photoMarkersUseCompactOpticallyCenteredSizing() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(Uri.parse("content://media/photo/1")));
        android.view.ViewGroup tile = (android.view.ViewGroup) activity
                .<GridLayout>findViewById(R.id.photo_grid).getChildAt(0);
        TextView heart = (TextView) tile.getChildAt(1);
        TextView star = (TextView) tile.getChildAt(2);
        float density = activity.getResources().getDisplayMetrics().density;

        assertEquals(Math.round(30 * density), heart.getLayoutParams().width);
        assertEquals(1.15f, heart.getTextScaleX(), 0.001f);
        assertEquals(false, heart.getIncludeFontPadding());
        assertEquals(Math.round(28 * density), star.getLayoutParams().width);
        assertEquals(false, star.getIncludeFontPadding());
        assertEquals(-density, star.getTranslationY(), 0.001f);
    }

    @Test public void duplicatePhotosShowTheirSharedStackAndPosition() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/1"),
                Uri.parse("content://media/photo/2"),
                Uri.parse("content://media/photo/3")));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        activity.showStacks(Map.of(
                "content://media/photo/1", new PhotoStackPosition(1, 2),
                "content://media/photo/2", new PhotoStackPosition(2, 2)));

        assertEquals("1/2", ((TextView) ((android.view.ViewGroup)
                grid.getChildAt(0)).getChildAt(3)).getText());
        assertEquals("2/2", ((TextView) ((android.view.ViewGroup)
                grid.getChildAt(1)).getChildAt(3)).getText());
        assertEquals(View.GONE, ((android.view.ViewGroup)
                grid.getChildAt(2)).getChildAt(3).getVisibility());
    }

    @Test public void infiniteScrollExpandsTheReviewWindow() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();

        activity.loadNextPage();

        assertEquals(120, activity.reviewLimit());
    }

    private static String text(ReviewActivity activity, int id) {
        return ((TextView) activity.findViewById(id)).getText().toString();
    }
}
