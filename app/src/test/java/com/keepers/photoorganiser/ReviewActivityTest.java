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
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

@RunWith(RobolectricTestRunner.class)
public class ReviewActivityTest {
    @Test public void quickReviewActionOpensFirstPhotoInOptionalMode() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        Uri first = Uri.parse("content://media/photo/quick-first");
        activity.showPhotos(List.of(first, Uri.parse("content://media/photo/quick-second")));

        activity.findViewById(R.id.open_quick_review).performClick();

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(PreviewActivity.class.getName(), started.getComponent().getClassName());
        assertEquals(first, started.getData());
        assertTrue(started.getBooleanExtra(PreviewActivity.EXTRA_QUICK_REVIEW, false));
    }

    @Test public void metadataToggleShowsRankedPercentagesOverGalleryPhotos() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        String photo = "content://media/photo/metadata";
        new PhotoInsightStore(activity).save(List.of(new PhotoFeatures(photo, 0, 0, .81,
                .92, .63, .88, .47, 0, -1, -1)), Map.of(), Set.of());
        activity.showPhotos(List.of(Uri.parse(photo)));
        GridLayout grid = activity.findViewById(R.id.photo_grid);
        ViewGroup tile = (ViewGroup) grid.getChildAt(0);
        TextView overlay = tile.findViewWithTag("metadata_overlay");

        assertEquals(View.GONE, overlay.getVisibility());
        activity.findViewById(R.id.toggle_metadata).performClick();

        assertEquals(View.VISIBLE, overlay.getVisibility());
        assertEquals("Focus 92%\nComposition 88%\nDetail 81%", overlay.getText().toString());
        assertTrue(activity.findViewById(R.id.toggle_metadata).isSelected());
    }
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

    @Test public void settingsButtonOpensGuidedPeopleSetupDirectly() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();

        activity.findViewById(R.id.open_settings).performClick();

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(PeopleActivity.class.getName(), started.getComponent().getClassName());
    }

    @Test public void albumReviewButtonOpensTheGuidedQueue() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();

        activity.findViewById(R.id.open_album_review).performClick();

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(AlbumReviewActivity.class.getName(), started.getComponent().getClassName());
    }

    @Test public void settingsDeviceImportActionOpensAndroidsMultiPhotoPicker() {
        Intent request = new Intent(org.robolectric.RuntimeEnvironment.getApplication(),
                ReviewActivity.class).setAction(ReviewActivity.ACTION_IMPORT_DEVICE_PHOTOS);
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class, request)
                .setup().get();

        Intent started = Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;
        assertEquals(android.provider.MediaStore.ACTION_PICK_IMAGES, started.getAction());
        assertEquals("image/*", started.getType());
        assertTrue(started.getIntExtra(android.provider.MediaStore.EXTRA_PICK_IMAGES_MAX, 0) > 1);
    }

    @Test public void completedGoogleSelectionStartsImportWithoutASecondTap() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        AtomicBoolean importStarted = new AtomicBoolean();

        activity.importCompletedGoogleSelection(() -> importStarted.set(true));

        assertTrue(importStarted.get());
    }

    @Test public void selectingPhotoKeepsEveryTileAtFullStrength() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/1"),
                Uri.parse("content://media/photo/2")));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        ((android.view.ViewGroup) grid.getChildAt(0)).getChildAt(1).performClick();

        assertEquals("1 new keeper", text(activity, R.id.keeper_count));
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

        assertEquals("No new keepers", text(activity, R.id.keeper_count));
        assertEquals(1f, grid.getChildAt(0).getAlpha(), 0.001f);
        assertEquals(1f, grid.getChildAt(1).getAlpha(), 0.001f);
    }

    @Test public void savedKeeperIsNotCountedAsNewAndUsesCompletedDescription() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        String photo = "content://media/photo/already-saved";
        AlbumCompletionStore completions = new AlbumCompletionStore(activity);
        completions.clear();
        completions.mark(photo, "Charlie Photos");
        activity.showPhotos(List.of(Uri.parse(photo)));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        ((android.view.ViewGroup) grid.getChildAt(0)).getChildAt(1).performClick();

        assertEquals("No new keepers", text(activity, R.id.keeper_count));
        assertEquals("Saved Keeper photo. Tap to remove.",
                grid.getChildAt(0).getContentDescription());
    }

    @Test public void suggestionsUseStarsWithoutFadingAlternatives() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/1"),
                Uri.parse("content://media/photo/2")));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        activity.showSuggestions(Set.of("content://media/photo/2"));

        assertEquals("1 suggested best shot", text(activity, R.id.suggestion_count));
        assertEquals("No new keepers", text(activity, R.id.keeper_count));
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

    @Test public void originFiltersSeparateLocalAndCloudPhotos() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        Uri local = Uri.parse("content://media/local/1");
        Uri cloud = Uri.parse("content://media/picker/cloud/2");
        activity.showPhotos(List.of(local, cloud), Map.of(
                local.toString(), PhotoOrigin.LOCAL,
                cloud.toString(), PhotoOrigin.CLOUD));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        activity.findViewById(R.id.filter_origin_cloud).performClick();
        assertEquals(1, grid.getChildCount());
        assertEquals(cloud.toString(), grid.getChildAt(0).getTag().toString());
        assertEquals("Cloud-only photo", ((ViewGroup) grid.getChildAt(0)).getChildAt(4)
                .getContentDescription());
        assertEquals(View.VISIBLE, ((ViewGroup) grid.getChildAt(0)).getChildAt(4)
                .getVisibility());

        activity.findViewById(R.id.filter_origin_local).performClick();
        assertEquals(1, grid.getChildCount());
        assertEquals(local.toString(), grid.getChildAt(0).getTag().toString());

        activity.findViewById(R.id.filter_origin_all).performClick();
        assertEquals(2, grid.getChildCount());
    }

    @Test public void savedCloudReviewsLoadEvenWithoutLocalLibraryPermission() {
        android.content.Context context = org.robolectric.RuntimeEnvironment.getApplication();
        ImportedPhotoStore imports = new ImportedPhotoStore(context);
        imports.clear();
        Uri cloud = Uri.parse("content://com.keepers.photoorganiser.cloud/saved.jpg");
        imports.add(new ImportedPhoto(cloud, 50, PhotoOrigin.CLOUD));

        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();

        GridLayout grid = activity.findViewById(R.id.photo_grid);
        assertEquals(1, grid.getChildCount());
        assertEquals(cloud.toString(), grid.getChildAt(0).getTag().toString());
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

    @Test public void duplicatePhotosCollapseToOneCoverWithStackCount() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/1"),
                Uri.parse("content://media/photo/2"),
                Uri.parse("content://media/photo/3")));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        List<String> stack = List.of("content://media/photo/1", "content://media/photo/2");
        activity.showSuggestions(Set.of("content://media/photo/2"));
        activity.showStacks(Map.of(
                "content://media/photo/1", new PhotoStackPosition(1, 2),
                "content://media/photo/2", new PhotoStackPosition(2, 2)), Map.of(
                "content://media/photo/1", stack, "content://media/photo/2", stack));

        assertEquals(2, grid.getChildCount());
        assertEquals("content://media/photo/2", grid.getChildAt(0).getTag().toString());
        TextView badge = (TextView) ((android.view.ViewGroup)
                grid.getChildAt(0)).getChildAt(3);
        assertEquals("2", badge.getText());
        assertEquals("Stack of 2 photos", badge.getContentDescription());
        assertNotNull(badge.getCompoundDrawables()[0]);
        assertEquals(View.GONE, ((android.view.ViewGroup)
                grid.getChildAt(1)).getChildAt(3).getVisibility());
    }

    @Test public void savedStacksAndRecommendationsRenderBeforeReassessmentFinishes() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        String first = "content://media/photo/cached-1";
        String recommended = "content://media/photo/cached-2";
        List<PhotoFeatures> cachedFeatures = List.of(
                new PhotoFeatures(first, 10, 1, .6),
                new PhotoFeatures(recommended, 11, 2, .9));
        Map<String, PhotoStackPosition> cachedStacks = Map.of(
                first, new PhotoStackPosition(1, 2),
                recommended, new PhotoStackPosition(2, 2));
        List<String> members = List.of(first, recommended);
        new PhotoInsightStore(activity).save(cachedFeatures, cachedStacks,
                Set.of(recommended));
        new PhotoStackStore(activity).save(Map.of(first, members, recommended, members));

        activity.showPhotos(List.of(Uri.parse(first), Uri.parse(recommended)));

        GridLayout grid = activity.findViewById(R.id.photo_grid);
        assertEquals(1, grid.getChildCount());
        assertEquals(recommended, grid.getChildAt(0).getTag().toString());
        assertEquals("1 suggested best shot", text(activity, R.id.suggestion_count));
        TextView badge = (TextView) ((ViewGroup) grid.getChildAt(0)).getChildAt(3);
        assertEquals("2", badge.getText().toString());
    }

    @Test public void keeperBecomesTheGalleryCoverForItsStack() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        String first = "content://media/photo/1";
        String recommended = "content://media/photo/2";
        activity.showPhotos(List.of(Uri.parse(first), Uri.parse(recommended)));
        List<String> stack = List.of(first, recommended);
        activity.showSuggestions(Set.of(recommended));
        activity.showStacks(Map.of(
                first, new PhotoStackPosition(1, 2),
                recommended, new PhotoStackPosition(2, 2)),
                Map.of(first, stack, recommended, stack));
        new KeeperSelectionStore(activity).toggle(Uri.parse(first));

        activity.onResume();

        GridLayout grid = activity.findViewById(R.id.photo_grid);
        assertEquals(1, grid.getChildCount());
        assertEquals(first, grid.getChildAt(0).getTag().toString());
        assertEquals("2", ((TextView) ((android.view.ViewGroup)
                grid.getChildAt(0)).getChildAt(3)).getText());
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
