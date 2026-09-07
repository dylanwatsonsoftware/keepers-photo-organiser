package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.net.Uri;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
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
    @Test public void appLauncherOpensPhotoGallery() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        Intent launcher = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(activity.getPackageName());

        ResolveInfo resolved = activity.getPackageManager().resolveActivity(launcher, 0);

        assertNotNull(resolved);
        assertEquals(ReviewActivity.class.getName(), resolved.activityInfo.name);
    }

    @Test public void appHasAKeepersLauncherIcon() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();

        assertTrue(activity.getApplicationInfo().icon != 0);
    }

    @Test public void settingsButtonOpensExistingSetupScreen() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();

        activity.findViewById(R.id.open_settings).performClick();

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(MainActivity.class.getName(), started.getComponent().getClassName());
    }

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

    @Test public void goodDuplicateUsesOutlinedStarInsteadOfRecommendation() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/1"), Uri.parse("content://media/photo/2")));

        activity.showSuggestions(Set.of("content://media/photo/1"),
                Set.of("content://media/photo/2"));

        GridLayout grid = activity.findViewById(R.id.photo_grid);
        assertEquals("Recommended best shot", ((android.view.ViewGroup) grid.getChildAt(0))
                .getChildAt(2).getContentDescription());
        assertEquals("Good alternative — near-identical photo ranked higher",
                ((android.view.ViewGroup) grid.getChildAt(1)).getChildAt(2)
                        .getContentDescription());
    }

    @Test public void keeperAndRecommendedFiltersAreExclusiveAndToggleBackToAll() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/1"),
                Uri.parse("content://media/photo/2"),
                Uri.parse("content://media/photo/3")));
        GridLayout grid = activity.findViewById(R.id.photo_grid);
        ((android.view.ViewGroup) grid.getChildAt(0)).getChildAt(1).performClick();
        activity.showSuggestions(Set.of("content://media/photo/2"));

        activity.findViewById(R.id.filter_keepers).performClick();
        assertEquals(1, grid.getChildCount());
        assertEquals("content://media/photo/1", grid.getChildAt(0).getTag().toString());

        activity.findViewById(R.id.filter_recommended).performClick();
        assertEquals(1, grid.getChildCount());
        assertEquals("content://media/photo/2", grid.getChildAt(0).getTag().toString());
        assertTrue(activity.findViewById(R.id.filter_recommended).isSelected());
        assertTrue(!activity.findViewById(R.id.filter_keepers).isSelected());

        activity.findViewById(R.id.filter_recommended).performClick();
        assertEquals(3, grid.getChildCount());
        assertEquals("content://media/photo/1", grid.getChildAt(0).getTag().toString());
        assertEquals("content://media/photo/2", grid.getChildAt(1).getTag().toString());
        assertEquals("content://media/photo/3", grid.getChildAt(2).getTag().toString());
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

    @Test public void photoMarkersUseMatchingVectorShapesWithCenteredStar() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(Uri.parse("content://media/photo/1")));
        android.view.ViewGroup tile = (android.view.ViewGroup) activity
                .<GridLayout>findViewById(R.id.photo_grid).getChildAt(0);
        assertTrue(tile.getChildAt(1) instanceof ImageView);
        assertTrue(tile.getChildAt(2) instanceof ImageView);
        ImageView heart = (ImageView) tile.getChildAt(1);
        ImageView star = (ImageView) tile.getChildAt(2);
        float density = activity.getResources().getDisplayMetrics().density;

        assertEquals(Math.round(30 * density), heart.getLayoutParams().width);
        assertNull(heart.getBackground());
        assertEquals(Math.round(28 * density), star.getLayoutParams().width);
        assertEquals(0f, star.getTranslationY(), 0.001f);
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

    @Test public void addingTheNextPagePreservesAlreadyRenderedTiles() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/1"),
                Uri.parse("content://media/photo/2")));
        GridLayout grid = activity.findViewById(R.id.photo_grid);
        View firstTile = grid.getChildAt(0);

        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/1"),
                Uri.parse("content://media/photo/2"),
                Uri.parse("content://media/photo/3")));

        assertSame(firstTile, grid.getChildAt(0));
        assertEquals(3, grid.getChildCount());
    }

    private static String text(ReviewActivity activity, int id) {
        return ((TextView) activity.findViewById(id)).getText().toString();
    }
}
