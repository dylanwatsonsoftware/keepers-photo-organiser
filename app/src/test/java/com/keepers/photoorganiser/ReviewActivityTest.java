package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.Manifest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowViewGroup;

@RunWith(RobolectricTestRunner.class)
public class ReviewActivityTest {
    @Test public void analysisProgressUsesATemporaryStripAboveTheGrid() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        View strip = activity.findViewById(id(activity, "analysis_progress_strip"));
        ProgressBar progress = activity.findViewById(id(activity, "analysis_progress_bar"));

        activity.showAnalysisProgress(17, 60);

        assertEquals(View.VISIBLE, strip.getVisibility());
        assertEquals(View.VISIBLE, progress.getVisibility());
        assertEquals(60, progress.getMax());
        assertEquals(17, progress.getProgress());
        assertEquals("Analysing 17 of 60", text(activity,
                id(activity, "analysis_progress_label")));

        activity.showSuggestions(Set.of("photo"));

        assertEquals(View.INVISIBLE, strip.getVisibility());
    }

    @Test public void galleryDefaultsToFourPhotoColumns() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();

        assertEquals(4, activity.<GridLayout>findViewById(R.id.photo_grid).getColumnCount());
    }

    @Test public void ratingsDisplayIsCountedAsAnActiveAdvancedFilter() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        TextView badge = activity.findViewById(id(activity, "gallery_filter_badge"));

        assertEquals(View.GONE, badge.getVisibility());
        activity.findViewById(R.id.toggle_metadata).performClick();

        assertEquals(View.VISIBLE, badge.getVisibility());
        assertEquals("1", badge.getText().toString());
    }

    @Test public void chosenGridDensityIsClampedAndRemembered() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();

        activity.setGridColumns(2);
        assertEquals(2, activity.gridColumns());
        assertEquals(2, activity.<GridLayout>findViewById(R.id.photo_grid).getColumnCount());

        ReviewActivity reopened = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        assertEquals(2, reopened.gridColumns());
        reopened.setGridColumns(9);
        assertEquals(4, reopened.gridColumns());
    }

    @Test public void pinchPreviewsContinuouslyBeforeCommittingNearestGridDensity() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/pinch-1"),
                Uri.parse("content://media/photo/pinch-2"),
                Uri.parse("content://media/photo/pinch-3"),
                Uri.parse("content://media/photo/pinch-4")));
        View content = activity.findViewById(android.R.id.content);
        content.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
        content.layout(0, 0, 1080, 1920);
        GridLayout grid = activity.findViewById(R.id.photo_grid);
        ScrollView scroll = activity.findViewById(R.id.review_scroll);
        float centreX = scroll.getWidth() / 2f;
        float centreY = scroll.getTop() + Math.min(300, scroll.getHeight() / 2f);

        dispatchPinch(activity, MotionEvent.ACTION_DOWN, centreX - 100, centreY,
                centreX + 100, centreY, 1);
        dispatchPinch(activity, MotionEvent.ACTION_POINTER_DOWN
                        | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                centreX - 100, centreY, centreX + 100, centreY, 2);
        dispatchPinch(activity, MotionEvent.ACTION_MOVE, centreX - 140, centreY,
                centreX + 140, centreY, 2);
        dispatchPinch(activity, MotionEvent.ACTION_MOVE, centreX - 200, centreY,
                centreX + 200, centreY, 2);

        assertEquals(4, grid.getColumnCount());
        assertTrue(grid.getScaleX() > 1.2f);

        dispatchPinch(activity, MotionEvent.ACTION_POINTER_UP
                        | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                centreX - 200, centreY, centreX + 200, centreY, 2);
        dispatchPinch(activity, MotionEvent.ACTION_UP, centreX - 200, centreY,
                centreX + 200, centreY, 1);

        assertEquals(3, activity.gridColumns());
        assertNotNull(grid.getAnimation());
    }

    @Test public void populatedGridCanShrinkWithoutInvalidColumnIndices() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/one"),
                Uri.parse("content://media/photo/two"),
                Uri.parse("content://media/photo/three"),
                Uri.parse("content://media/photo/four")));

        activity.setGridColumns(2);

        GridLayout grid = activity.findViewById(R.id.photo_grid);
        assertEquals(2, grid.getColumnCount());
        assertEquals(4, grid.getChildCount());
    }

    @Test public void filteredTileCanReattachAfterGridShrinks() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        Uri hiddenDuringResize = Uri.parse("content://media/photo/reattached");
        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/one"), hiddenDuringResize));
        GridLayout grid = activity.findViewById(R.id.photo_grid);
        View tile = grid.getChildAt(1);
        grid.removeView(tile);
        GridLayout.LayoutParams stale = (GridLayout.LayoutParams) tile.getLayoutParams();
        stale.columnSpec = GridLayout.spec(3);
        tile.setLayoutParams(stale);

        activity.setGridColumns(2);
        activity.showSuggestions(Set.of(hiddenDuringResize.toString()));

        assertEquals(2, grid.getColumnCount());
        assertEquals(2, grid.getChildCount());
    }

    @Test public void oneColumnDensityUsesAFullWidthNonSquarePhoto() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.setGridColumns(1);
        activity.showPhotos(List.of(Uri.parse("content://media/photo/full-width")));

        View tile = activity.<GridLayout>findViewById(R.id.photo_grid).getChildAt(0);
        assertTrue(tile.getLayoutParams().width > 0);
        assertTrue(tile.getLayoutParams().height > 0);
        assertTrue(tile.getLayoutParams().width != tile.getLayoutParams().height);
    }

    @Test public void longPressSelectionReplacesBottomNavigation() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(Uri.parse("content://media/photo/contextual-bar")));

        assertTrue(activity.<GridLayout>findViewById(R.id.photo_grid)
                .getChildAt(0).performLongClick());

        assertEquals(View.GONE,
                activity.findViewById(id(activity, "gallery_bottom_navigation")).getVisibility());
        assertEquals(View.VISIBLE,
                activity.findViewById(R.id.gallery_selection_actions).getVisibility());
        assertEquals(View.INVISIBLE,
                activity.findViewById(R.id.gallery_controls).getVisibility());
        assertEquals("×", text(activity, R.id.cancel_selection));
    }

    @Test public void tappingKeeperHeartStartsSubtleFeedback() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(Uri.parse("content://media/photo/animated-heart")));
        ImageView heart = (ImageView) activity.<GridLayout>findViewById(R.id.photo_grid)
                .getChildAt(0).findViewWithTag("marker");

        heart.performClick();

        assertTrue(heart.getAnimation() instanceof ScaleAnimation);
        assertTrue(heart.getAnimation().getDuration() <= 200);
    }

    @Test public void keeperHeartStillAnimatesAfterAnalysisHasCompleted() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        String id = "content://media/photo/analyzed-heart";
        new PhotoInsightStore(activity).save(List.of(
                new PhotoFeatures(id, 0, 12, .8)), Map.of(), Set.of());
        activity.showPhotos(List.of(Uri.parse(id)));
        ImageView heart = (ImageView) activity.<GridLayout>findViewById(R.id.photo_grid)
                .getChildAt(0).findViewWithTag("marker");

        heart.performClick();

        assertTrue(heart.getAnimation() instanceof ScaleAnimation);
        assertEquals(Set.of(), new SuggestionStore(activity).load());

        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(160,
                TimeUnit.MILLISECONDS);

        assertEquals(Set.of(id), new SuggestionStore(activity).load());
    }

    @Test public void applyingGalleryFiltersUsesVisibleTapFeedback() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        View keepers = activity.findViewById(R.id.filter_keepers);
        View suggested = activity.findViewById(R.id.filter_recommended);

        keepers.performClick();
        suggested.performClick();

        assertNotNull(keepers.getAnimation());
        assertNotNull(suggested.getAnimation());
    }

    @Test public void keepersFilterHeartIsWhiteUntilTheFilterIsSelected() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        TextView keepers = activity.findViewById(R.id.filter_keepers);

        assertEquals(Color.WHITE, keepers.getCompoundDrawableTintList().getColorForState(
                new int[]{-android.R.attr.state_selected}, Color.TRANSPARENT));

        keepers.performClick();

        assertEquals(activity.getColor(R.color.gallery_accent_keeper),
                keepers.getCompoundDrawableTintList().getColorForState(
                        new int[]{android.R.attr.state_selected}, Color.TRANSPARENT));
    }

    @Test public void destinationsShowFeedbackBeforeNavigating() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        View albums = activity.findViewById(R.id.albums_destination);
        Shadows.shadowOf(activity).getNextStartedActivity();

        albums.performClick();

        assertNotNull(albums.getAnimation());
        assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(140,
                TimeUnit.MILLISECONDS);
        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(AlbumReviewActivity.class.getName(),
                started.getComponent().getClassName());
    }

    @Test public void recommendationStarAdaptsToGridDensity() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        Uri photo = Uri.parse("content://media/photo/adaptive-star");
        activity.showPhotos(List.of(photo));
        activity.showSuggestions(Set.of(photo.toString()));
        ImageView star = (ImageView) ((ViewGroup) activity.<GridLayout>findViewById(
                R.id.photo_grid).getChildAt(0)).getChildAt(2);

        assertTrue(star.getLayoutParams().width <= dp(activity, 22));
        activity.setGridColumns(2);
        assertTrue(star.getLayoutParams().width >= dp(activity, 26));
    }

    @Test public void filterActionOpensAndApplyClosesBottomSheet() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        View overlay = activity.findViewById(id(activity, "gallery_filter_overlay"));

        activity.findViewById(id(activity, "open_gallery_filters")).performClick();
        assertEquals(View.VISIBLE, overlay.getVisibility());
        activity.findViewById(id(activity, "apply_gallery_filters")).performClick();
        assertEquals(View.GONE, overlay.getVisibility());
    }

    @Test public void backClosesFilterSheetBeforeLeavingGallery() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        View overlay = activity.findViewById(id(activity, "gallery_filter_overlay"));
        activity.findViewById(id(activity, "open_gallery_filters")).performClick();

        activity.onBackPressed();

        assertEquals(View.GONE, overlay.getVisibility());
        assertFalse(activity.isFinishing());
    }

    @Test public void galleryHeaderUsesOneCountedKeepersShortcutAndOneFilterAction() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        Uri keeper = Uri.parse("content://media/photo/header-keeper");
        new KeeperSelectionStore(activity).replace(Set.of(keeper.toString()));

        activity.showPhotos(List.of(keeper,
                Uri.parse("content://media/photo/header-unreviewed")));

        View keepers = activity.findViewById(id(activity, "filter_keepers"));
        View filters = activity.findViewById(id(activity, "open_gallery_filters"));
        assertEquals("Keepers · 1", ((TextView) keepers).getText().toString());
        assertTrue(keepers instanceof TextView);
        assertTrue(!(keepers instanceof android.widget.Button));
        assertNotNull(keepers.getBackground());
        assertNotNull(filters);
        assertEquals(0, activity.getResources().getIdentifier(
                "photo_summary", "id", activity.getPackageName()));
    }

    @Test public void galleryHasCompactGalleryReviewAndAlbumsDestinations() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();

        assertTrue(activity.findViewById(id(activity, "gallery_destination")).isSelected());
        assertTrue(activity.findViewById(id(activity, "review_destination")).isClickable());

        activity.findViewById(id(activity, "albums_destination")).performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(140,
                TimeUnit.MILLISECONDS);
        Intent albums = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(AlbumReviewActivity.class.getName(),
                albums.getComponent().getClassName());
    }

    @Test public void quickReviewActionOpensFirstPhotoInOptionalMode() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        Uri first = Uri.parse("content://media/photo/quick-first");
        activity.showPhotos(List.of(first, Uri.parse("content://media/photo/quick-second")));

        activity.findViewById(R.id.review_destination).performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(140,
                TimeUnit.MILLISECONDS);

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
        new PhotoContextStore(activity).save(photo,
                PhotoContext.of(PhotoContextType.PORTRAIT, .85));
        activity.showPhotos(List.of(Uri.parse(photo)));
        GridLayout grid = activity.findViewById(R.id.photo_grid);
        ViewGroup tile = (ViewGroup) grid.getChildAt(0);
        TextView overlay = tile.findViewWithTag("metadata_overlay");

        assertEquals(View.GONE, overlay.getVisibility());
        activity.findViewById(R.id.toggle_metadata).performClick();

        assertEquals(View.VISIBLE, overlay.getVisibility());
        assertEquals("Type · Portrait 85%\nRank 76% · Limiting Motion 47%\n"
                        + "Focus 92%\nComposition 88%\nDetail 81%",
                overlay.getText().toString());
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
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(140,
                TimeUnit.MILLISECONDS);

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(PeopleActivity.class.getName(), started.getComponent().getClassName());
    }

    @Test public void albumReviewButtonOpensTheGuidedQueue() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();

        activity.findViewById(R.id.albums_destination).performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(140,
                TimeUnit.MILLISECONDS);

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
        assertEquals("*/*", started.getType());
        assertTrue(started.getIntExtra(android.provider.MediaStore.EXTRA_PICK_IMAGES_MAX, 0) > 1);
    }

    @Test public void manifestRequestsVideoLibraryAccess() throws Exception {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        String[] permissions = activity.getPackageManager().getPackageInfo(
                activity.getPackageName(), android.content.pm.PackageManager.GET_PERMISSIONS)
                .requestedPermissions;

        assertTrue(java.util.Arrays.asList(permissions).contains(Manifest.permission.READ_MEDIA_VIDEO));
    }

    @Test public void galleryShowsVideoDurationAndCanFilterToVideos() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        RecentPhoto photo = new RecentPhoto(Uri.parse("content://media/images/media/1"), 20);
        RecentPhoto video = new RecentPhoto(Uri.parse("content://media/video/media/2"), 10,
                MediaType.VIDEO, 65_000);

        activity.showMedia(List.of(photo, video));
        GridLayout grid = activity.findViewById(R.id.photo_grid);
        TextView duration = ((ViewGroup) grid.getChildAt(1)).findViewWithTag("video_duration");

        assertEquals("▶  1:05", duration.getText().toString());
        assertEquals(View.VISIBLE, duration.getVisibility());
        assertTrue(grid.getChildAt(1).getContentDescription().toString().startsWith("Video"));
        for (int id : new int[]{R.id.filter_media_all, R.id.filter_photos, R.id.filter_videos}) {
            View filter = activity.findViewById(id);
            assertTrue(filter instanceof TextView);
            assertTrue(!(filter instanceof android.widget.Button));
            assertNotNull(filter.getBackground());
        }
        activity.findViewById(R.id.filter_videos).performClick();
        assertEquals(1, grid.getChildCount());
        assertEquals(video.uri(), grid.getChildAt(0).getTag());
    }

    @Test public void tappingGalleryVideoRequestsFullscreenAutoplay() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        RecentPhoto video = new RecentPhoto(Uri.parse("content://media/video/media/autoplay"), 10,
                MediaType.VIDEO, 3_000);
        activity.showMedia(List.of(video));

        activity.<GridLayout>findViewById(R.id.photo_grid).getChildAt(0).performClick();

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertTrue(started.getBooleanExtra("autoplay_video", false));
    }

    @Test public void galleryMetadataShowsRankedVideoSignalsAfterAnalysis() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        Uri uri = Uri.parse("content://media/video/media/rated");
        new VideoInsightStore(activity).save(new VideoFeatures(VideoFeatures.SCHEMA_VERSION,
                uri.toString(), 3_000, 3, .7, .8, .9, .6, .75, 0, 0));
        activity.showMedia(List.of(new RecentPhoto(uri, 10, MediaType.VIDEO, 3_000)));

        activity.findViewById(R.id.toggle_metadata).performClick();

        TextView overlay = activity.<GridLayout>findViewById(R.id.photo_grid)
                .getChildAt(0).findViewWithTag("metadata_overlay");
        assertEquals("Exposure 90%\nFocus 80%\nStability 75%", overlay.getText().toString());
    }

    @Test public void quickReviewHonoursTheVideoFilter() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        RecentPhoto photo = new RecentPhoto(Uri.parse("content://media/images/media/11"), 20);
        RecentPhoto video = new RecentPhoto(Uri.parse("content://media/video/media/12"), 10,
                MediaType.VIDEO, 3_000);
        activity.showMedia(List.of(photo, video));

        activity.findViewById(R.id.filter_videos).performClick();
        activity.findViewById(R.id.review_destination).performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(140,
                TimeUnit.MILLISECONDS);

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(video.uri(), started.getData());
        assertEquals(MediaType.VIDEO.name(), started.getStringExtra(
                PreviewActivity.EXTRA_MEDIA_TYPE));
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
        activity.findViewById(R.id.filter_include_keepers).performClick();

        ((android.view.ViewGroup) grid.getChildAt(0)).getChildAt(1).performClick();

        assertEquals("Keepers · 1", text(activity, R.id.filter_keepers));
        assertEquals(2, grid.getChildCount());
        assertEquals(View.VISIBLE, grid.getChildAt(1).getVisibility());
        assertEquals(1f, grid.getChildAt(0).getAlpha(), 0.001f);
        assertEquals(1f, grid.getChildAt(1).getAlpha(), 0.001f);
    }

    @Test public void galleryCanBulkHideSelectedPhotos() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        Uri first = Uri.parse("content://media/photo/hide-1");
        Uri second = Uri.parse("content://media/photo/hide-2");
        activity.showPhotos(List.of(first, second));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        assertTrue(grid.getChildAt(0).performLongClick());
        grid.getChildAt(1).performClick();
        activity.findViewById(id(activity, "selection_hide")).performClick();

        assertEquals(Set.of(first.toString(), second.toString()),
                new HiddenPhotoStore(activity).load());
        assertEquals(0, grid.getChildCount());
    }

    @Test public void galleryCanShareASelectedGroupOfPhotosAndVideos() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        Uri photo = Uri.parse("content://media/images/media/share-photo");
        Uri video = Uri.parse("content://media/video/media/share-video");
        activity.showMedia(List.of(
                new RecentPhoto(photo, 20),
                new RecentPhoto(video, 10, MediaType.VIDEO, 3_000)));
        GridLayout grid = activity.findViewById(R.id.photo_grid);
        assertTrue(grid.getChildAt(0).performLongClick());
        grid.getChildAt(1).performClick();
        activity.findViewById(id(activity, "selection_share")).performClick();

        Intent chooser = Shadows.shadowOf(activity).getNextStartedActivity();
        Intent share = chooser.getParcelableExtra(Intent.EXTRA_INTENT);
        assertEquals(Intent.ACTION_SEND_MULTIPLE, share.getAction());
        assertEquals("*/*", share.getType());
        assertEquals(List.of(photo, video),
                share.getParcelableArrayListExtra(Intent.EXTRA_STREAM));
        assertTrue((share.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
    }

    @Test public void gallerySelectionUsesANeutralTickInsteadOfRecommendationGold() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(Uri.parse("content://media/photo/select-green")));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        assertTrue(grid.getChildAt(0).performLongClick());

        TextView check = grid.getChildAt(0).findViewWithTag("hide_selection_check");
        assertEquals(View.VISIBLE, check.getVisibility());
        assertEquals("✓", check.getText().toString());
        GradientDrawable background = (GradientDrawable) check.getBackground();
        assertEquals(activity.getColor(R.color.gallery_selection),
                background.getColor().getDefaultColor());
    }

    @Test public void longPressStartsInsetGallerySelectionWithCountAndCancel() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(Uri.parse("content://media/photo/one"),
                Uri.parse("content://media/photo/two")));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        assertTrue(grid.getChildAt(0).performLongClick());

        assertEquals("1 selected", text(activity, id(activity, "selection_count")));
        assertEquals(View.VISIBLE,
                activity.findViewById(id(activity, "gallery_selection_actions")).getVisibility());
        ImageView image = (ImageView) ((ViewGroup) grid.getChildAt(0)).getChildAt(0);
        Shadows.shadowOf(android.os.Looper.getMainLooper())
                .idleFor(140, TimeUnit.MILLISECONDS);
        assertEquals(.84f, image.getScaleX(), .001f);
        assertEquals(activity.getColor(R.color.gallery_surface_elevated),
                ((ColorDrawable) grid.getChildAt(0).getBackground()).getColor());
        TextView check = grid.getChildAt(0).findViewWithTag("hide_selection_check");
        assertEquals(Gravity.TOP | Gravity.START,
                ((android.widget.FrameLayout.LayoutParams) check.getLayoutParams()).gravity);

        grid.getChildAt(1).performClick();
        assertEquals("2 selected", text(activity, id(activity, "selection_count")));
        activity.findViewById(id(activity, "cancel_selection")).performClick();

        assertEquals(View.GONE,
                activity.findViewById(id(activity, "gallery_selection_actions")).getVisibility());
        Shadows.shadowOf(android.os.Looper.getMainLooper())
                .idleFor(140, TimeUnit.MILLISECONDS);
        assertEquals(1f, image.getScaleX(), .001f);
        assertEquals(Color.TRANSPARENT,
                ((ColorDrawable) grid.getChildAt(0).getBackground()).getColor());
        assertEquals(View.GONE, check.getVisibility());
    }

    @Test public void galleryPhotoAnimatesWhenSelectionShrinksAndExpands() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(Uri.parse("content://media/photo/animated-selection")));
        GridLayout grid = activity.findViewById(R.id.photo_grid);
        View tile = grid.getChildAt(0);
        ImageView image = (ImageView) ((ViewGroup) tile).getChildAt(0);

        assertTrue(tile.performLongClick());

        assertEquals(140L, image.animate().getDuration());
        Shadows.shadowOf(android.os.Looper.getMainLooper())
                .idleFor(140, TimeUnit.MILLISECONDS);
        assertEquals(.84f, image.getScaleX(), .001f);

        activity.findViewById(R.id.cancel_selection).performClick();

        assertEquals(140L, image.animate().getDuration());
        Shadows.shadowOf(android.os.Looper.getMainLooper())
                .idleFor(140, TimeUnit.MILLISECONDS);
        assertEquals(1f, image.getScaleX(), .001f);
    }

    @Test public void heldDragExpandsAndContractsTheVisibleSelectionRange() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/range-1"),
                Uri.parse("content://media/photo/range-2"),
                Uri.parse("content://media/photo/range-3"),
                Uri.parse("content://media/photo/range-4"),
                Uri.parse("content://media/photo/range-5"),
                Uri.parse("content://media/photo/range-6"),
                Uri.parse("content://media/photo/range-7"),
                Uri.parse("content://media/photo/range-8"),
                Uri.parse("content://media/photo/range-9"),
                Uri.parse("content://media/photo/range-10"),
                Uri.parse("content://media/photo/range-11"),
                Uri.parse("content://media/photo/range-12")));
        GridLayout grid = activity.findViewById(R.id.photo_grid);
        layoutThreeColumnGrid(grid, 100);
        View anchor = grid.getChildAt(0);

        assertTrue(anchor.performLongClick());
        dispatchMove(anchor, 250, 350);

        assertEquals("12 selected", text(activity, R.id.selection_count));
        for (int index = 0; index < 12; index++) assertEquals(View.VISIBLE,
                grid.getChildAt(index).findViewWithTag("hide_selection_check").getVisibility());

        dispatchMove(anchor, 150, 50);

        assertEquals("2 selected", text(activity, R.id.selection_count));
        assertEquals(View.VISIBLE,
                grid.getChildAt(0).findViewWithTag("hide_selection_check").getVisibility());
        assertEquals(View.VISIBLE,
                grid.getChildAt(1).findViewWithTag("hide_selection_check").getVisibility());
        assertEquals(View.GONE,
                grid.getChildAt(2).findViewWithTag("hide_selection_check").getVisibility());
    }

    @Test public void heldRangeGesturePreventsVerticalScrollFromStealingLaterRows() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(Uri.parse("content://media/photo/scroll-lock")));
        GridLayout grid = activity.findViewById(R.id.photo_grid);
        ScrollView scroll = activity.findViewById(R.id.review_scroll);
        View anchor = grid.getChildAt(0);
        ShadowViewGroup shadowScroll = Shadows.shadowOf(scroll);

        assertFalse(shadowScroll.getDisallowInterceptTouchEvent());
        assertTrue(anchor.performLongClick());
        assertTrue(shadowScroll.getDisallowInterceptTouchEvent());

        dispatchUp(anchor, 50, 50);
        assertFalse(shadowScroll.getDisallowInterceptTouchEvent());
    }

    @Test public void heldSelectionAutoScrollsAndExtendsAtTheBottomEdge() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        ArrayList<Uri> photos = new ArrayList<>();
        for (int index = 0; index < 30; index++)
            photos.add(Uri.parse("content://media/photo/edge-scroll-" + index));
        activity.showPhotos(photos);
        ScrollView scroll = activity.findViewById(R.id.review_scroll);
        GridLayout grid = activity.findViewById(R.id.photo_grid);
        scroll.layout(0, 0, 300, 300);
        layoutThreeColumnGrid(grid, 100);
        View anchor = grid.getChildAt(0);

        assertTrue(anchor.performLongClick());
        try {
            dispatchMoveAtViewport(anchor, 150, 295);
            int selectedBeforeScroll = selectedCount(activity);
            Shadows.shadowOf(android.os.Looper.getMainLooper())
                    .idleFor(160, TimeUnit.MILLISECONDS);

            assertTrue(scroll.getScrollY() > 0);
            assertTrue("before=" + selectedBeforeScroll + " after=" + selectedCount(activity)
                            + " scroll=" + scroll.getScrollY(),
                    selectedCount(activity) > selectedBeforeScroll);
            int bottomScroll = scroll.getScrollY();

            scroll.layout(0, 0, 300, 300);
            dispatchMoveAtViewport(anchor, 150, 150);
            Shadows.shadowOf(android.os.Looper.getMainLooper())
                    .idleFor(80, TimeUnit.MILLISECONDS);
            assertTrue(scroll.getScrollY() <= bottomScroll);
            int settledBottomScroll = scroll.getScrollY();

            scroll.layout(0, 0, 300, 300);
            dispatchMoveAtViewport(anchor, 150, 5);
            Shadows.shadowOf(android.os.Looper.getMainLooper())
                    .idleFor(160, TimeUnit.MILLISECONDS);
            assertTrue(scroll.getScrollY() < settledBottomScroll);
        } finally {
            dispatchRangeUp(anchor, 150, 5);
        }
    }

    @Test public void heldSelectionScrollsFasterDeeperInsideTheEdge() {
        int shallowScroll = autoScrollDistanceAtBottomEdge(245);
        int deepScroll = autoScrollDistanceAtBottomEdge(295);

        assertTrue("deep=" + deepScroll + " shallow=" + shallowScroll,
                shallowScroll > 0 && deepScroll > shallowScroll);
    }

    private static int autoScrollDistanceAtBottomEdge(float pointerY) {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        ArrayList<Uri> photos = new ArrayList<>();
        for (int index = 0; index < 30; index++)
            photos.add(Uri.parse("content://media/photo/edge-speed-" + index));
        activity.showPhotos(photos);
        ScrollView scroll = activity.findViewById(R.id.review_scroll);
        GridLayout grid = activity.findViewById(R.id.photo_grid);
        scroll.layout(0, 0, 300, 300);
        layoutThreeColumnGrid(grid, 100);
        View anchor = grid.getChildAt(0);

        assertTrue(anchor.performLongClick());
        try {
            dispatchMoveAtViewport(anchor, 150, pointerY);
            Shadows.shadowOf(android.os.Looper.getMainLooper())
                    .idleFor(160, TimeUnit.MILLISECONDS);
            return scroll.getScrollY();
        } finally {
            dispatchRangeUp(anchor, 150, pointerY);
        }
    }

    @Test public void heldDragPreservesSelectionsMadeBeforeThatRangeGesture() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/existing-1"),
                Uri.parse("content://media/photo/existing-2"),
                Uri.parse("content://media/photo/existing-3"),
                Uri.parse("content://media/photo/existing-4"),
                Uri.parse("content://media/photo/existing-5")));
        GridLayout grid = activity.findViewById(R.id.photo_grid);
        layoutThreeColumnGrid(grid, 100);
        grid.getChildAt(4).performLongClick();
        dispatchUp(grid.getChildAt(4), 150, 150);
        grid.getChildAt(0).performClick();

        View anchor = grid.getChildAt(1);
        anchor.performLongClick();
        dispatchMove(anchor, -50, 150);
        assertEquals("5 selected", text(activity, R.id.selection_count));
        dispatchMove(anchor, 50, 50);

        assertEquals("3 selected", text(activity, R.id.selection_count));
        assertEquals(View.VISIBLE,
                grid.getChildAt(0).findViewWithTag("hide_selection_check").getVisibility());
        assertEquals(View.VISIBLE,
                grid.getChildAt(1).findViewWithTag("hide_selection_check").getVisibility());
        assertEquals(View.VISIBLE,
                grid.getChildAt(4).findViewWithTag("hide_selection_check").getVisibility());
        assertEquals(View.GONE,
                grid.getChildAt(2).findViewWithTag("hide_selection_check").getVisibility());
    }

    @Test public void heldDragFromASelectedPhotoRemovesAndRestoresTheDraggedRange() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        ArrayList<Uri> photos = new ArrayList<>();
        for (int index = 0; index < 12; index++)
            photos.add(Uri.parse("content://media/photo/unselect-range-" + index));
        activity.showPhotos(photos);
        GridLayout grid = activity.findViewById(R.id.photo_grid);
        layoutThreeColumnGrid(grid, 100);
        View first = grid.getChildAt(0);
        assertTrue(first.performLongClick());
        dispatchMove(first, 250, 350);
        dispatchUp(first, 250, 350);
        assertEquals("12 selected", text(activity, R.id.selection_count));

        View selectedAnchor = grid.getChildAt(2);
        assertTrue(selectedAnchor.performLongClick());
        dispatchMove(selectedAnchor, 50, 250);
        assertEquals("5 selected", text(activity, R.id.selection_count));

        dispatchMove(selectedAnchor, 50, 150);
        assertEquals("8 selected", text(activity, R.id.selection_count));
        assertEquals(View.VISIBLE,
                grid.getChildAt(6).findViewWithTag("hide_selection_check").getVisibility());
        assertEquals(View.GONE,
                grid.getChildAt(4).findViewWithTag("hide_selection_check").getVisibility());
        dispatchUp(selectedAnchor, 50, 150);
    }

    @Test public void removingTheWholeSelectedRangeExitsSelectionImmediately() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/remove-all-1"),
                Uri.parse("content://media/photo/remove-all-2"),
                Uri.parse("content://media/photo/remove-all-3")));
        GridLayout grid = activity.findViewById(R.id.photo_grid);
        layoutThreeColumnGrid(grid, 100);
        View first = grid.getChildAt(0);
        assertTrue(first.performLongClick());
        dispatchMove(first, 250, 50);
        dispatchUp(first, 250, 50);
        assertEquals("3 selected", text(activity, R.id.selection_count));

        assertTrue(first.performLongClick());
        dispatchMove(first, 250, 50);

        assertEquals(View.GONE, activity.findViewById(R.id.selection_count).getVisibility());
        assertEquals(View.GONE,
                activity.findViewById(R.id.gallery_selection_actions).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.gallery_title).getVisibility());
        dispatchUp(first, 250, 50);
    }

    @Test public void gallerySelectionCanMarkMultipleItemsAsKeepers() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        Uri first = Uri.parse("content://media/photo/keeper-one");
        Uri second = Uri.parse("content://media/photo/keeper-two");
        activity.showPhotos(List.of(first, second));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        assertTrue(grid.getChildAt(0).performLongClick());
        grid.getChildAt(1).performClick();
        activity.findViewById(id(activity, "selection_keeper")).performClick();

        assertEquals(Set.of(first.toString(), second.toString()),
                new KeeperSelectionStore(activity).load());
        assertEquals(View.GONE,
                activity.findViewById(id(activity, "gallery_selection_actions")).getVisibility());
    }

    @Test public void gallerySelectionCanOpenOnlyThoseItemsInAddToAlbums() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        Uri first = Uri.parse("content://media/photo/album-one");
        Uri second = Uri.parse("content://media/photo/album-two");
        activity.showPhotos(List.of(first, second));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        assertTrue(grid.getChildAt(0).performLongClick());
        activity.findViewById(id(activity, "selection_albums")).performClick();

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(AlbumReviewActivity.class.getName(), started.getComponent().getClassName());
        assertEquals(List.of(first.toString()),
                started.getStringArrayListExtra("selected_media"));
        assertTrue(new KeeperSelectionStore(activity).load().contains(first.toString()));
        assertFalse(new KeeperSelectionStore(activity).load().contains(second.toString()));
    }

    @Test public void galleryBulkHideHidesEveryPhotoInASelectedStack() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        String first = "content://media/photo/hide-stack-1";
        String second = "content://media/photo/hide-stack-2";
        String single = "content://media/photo/keep-visible";
        activity.showPhotos(List.of(Uri.parse(first), Uri.parse(second), Uri.parse(single)));
        List<String> stack = List.of(first, second);
        activity.showStacks(Map.of(
                first, new PhotoStackPosition(1, 2),
                second, new PhotoStackPosition(2, 2)),
                Map.of(first, stack, second, stack));
        new PhotoStackStore(activity).save(Map.of(first, stack, second, stack));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        assertTrue(grid.getChildAt(0).performLongClick());
        activity.findViewById(id(activity, "selection_hide")).performClick();

        assertEquals(Set.of(first, second), new HiddenPhotoStore(activity).load());
        assertEquals(1, grid.getChildCount());
        assertEquals(single, grid.getChildAt(0).getTag().toString());
    }

    @Test public void hiddenFilterShowsOnlyHiddenPhotos() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        Uri visible = Uri.parse("content://media/photo/visible");
        Uri hidden = Uri.parse("content://media/photo/hidden");
        activity.showPhotos(List.of(visible, hidden));
        new HiddenPhotoStore(activity).hide(Set.of(hidden.toString()));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        activity.findViewById(R.id.filter_hidden).performClick();

        assertEquals(1, grid.getChildCount());
        assertEquals(hidden.toString(), grid.getChildAt(0).getTag().toString());
        assertTrue(activity.findViewById(R.id.filter_hidden).isSelected());
    }

    @Test public void quickReviewStartsOnFirstVisiblePhoto() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        Uri hidden = Uri.parse("content://media/photo/hidden-first");
        Uri visible = Uri.parse("content://media/photo/visible-second");
        activity.showPhotos(List.of(hidden, visible));
        new HiddenPhotoStore(activity).hide(Set.of(hidden.toString()));

        activity.findViewById(R.id.review_destination).performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(140,
                TimeUnit.MILLISECONDS);

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(visible, started.getData());
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

        assertEquals("Keepers · 1", text(activity, R.id.filter_keepers));
        assertEquals(0, grid.getChildCount());
        activity.findViewById(R.id.filter_keepers).performClick();
        assertEquals(1, grid.getChildCount());
        assertEquals("Saved Keeper photo. Tap to remove.",
                grid.getChildAt(0).getContentDescription());
    }

    @Test public void newKeepersAreRedAndSavedKeepersAreWhite() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        String fresh = "content://media/photo/fresh-keeper";
        String saved = "content://media/photo/saved-keeper";
        new KeeperSelectionStore(activity).replace(Set.of(fresh, saved));
        AlbumCompletionStore completions = new AlbumCompletionStore(activity);
        completions.clear();
        completions.mark(saved, "Family");

        activity.showPhotos(List.of(Uri.parse(fresh), Uri.parse(saved)));
        activity.findViewById(R.id.filter_include_keepers).performClick();

        GridLayout grid = activity.findViewById(R.id.photo_grid);
        ImageView freshHeart = (ImageView) ((ViewGroup) grid.getChildAt(0)).getChildAt(1);
        ImageView savedHeart = (ImageView) ((ViewGroup) grid.getChildAt(1)).getChildAt(1);
        assertEquals(activity.getColor(R.color.gallery_accent_keeper),
                Shadows.shadowOf((PorterDuffColorFilter) freshHeart.getColorFilter()).getColor());
        assertEquals(Color.WHITE,
                Shadows.shadowOf((PorterDuffColorFilter) savedHeart.getColorFilter()).getColor());
    }

    @Test public void suggestionsUseStarsWithoutFadingAlternatives() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        activity.showPhotos(List.of(
                Uri.parse("content://media/photo/1"),
                Uri.parse("content://media/photo/2")));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        activity.showSuggestions(Set.of("content://media/photo/2"));

        assertEquals(View.INVISIBLE,
                activity.findViewById(R.id.analysis_progress_strip).getVisibility());
        assertEquals("Keepers · 0", text(activity, R.id.filter_keepers));
        assertEquals(1f, grid.getChildAt(0).getAlpha(), 0.001f);
        assertEquals(1f, grid.getChildAt(1).getAlpha(), 0.001f);
        assertEquals(View.VISIBLE,
                ((android.view.ViewGroup) grid.getChildAt(1)).getChildAt(2).getVisibility());

        activity.findViewById(R.id.filter_include_keepers).performClick();
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

    @Test public void defaultGalleryExcludesOnlyKeepersAlreadyAddedToAnAlbum() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        String keeper = "content://media/photo/keeper";
        String unreviewed = "content://media/photo/still-to-review";
        activity.showPhotos(List.of(
                Uri.parse(keeper),
                Uri.parse(unreviewed)));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        ((ViewGroup) grid.getChildAt(0)).getChildAt(1).performClick();

        assertEquals(2, grid.getChildCount());
        assertEquals(keeper, grid.getChildAt(0).getTag().toString());

        new AlbumCompletionStore(activity).mark(keeper, "Family");
        activity.onResume();

        assertEquals(1, grid.getChildCount());
        assertEquals(unreviewed, grid.getChildAt(0).getTag().toString());

        activity.findViewById(R.id.filter_include_keepers).performClick();

        assertEquals(2, grid.getChildCount());
        assertEquals(keeper, grid.getChildAt(0).getTag().toString());
        assertEquals(unreviewed, grid.getChildAt(1).getTag().toString());
        assertTrue(activity.findViewById(R.id.filter_include_keepers).isSelected());
    }

    @Test public void includeKeepersStillExcludesHiddenPhotos() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        String keeper = "content://media/photo/keeper";
        String hidden = "content://media/photo/hidden";
        new KeeperSelectionStore(activity).toggle(Uri.parse(keeper));
        new HiddenPhotoStore(activity).hide(Set.of(hidden));
        activity.showPhotos(List.of(Uri.parse(keeper), Uri.parse(hidden)));

        activity.findViewById(R.id.filter_include_keepers).performClick();

        GridLayout grid = activity.findViewById(R.id.photo_grid);
        assertEquals(1, grid.getChildCount());
        assertEquals(keeper, grid.getChildAt(0).getTag().toString());
    }

    @Test public void allSourcesPreservesTheIncludeKeepersChoice() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        String keeper = "content://media/local/keeper";
        String cloud = "content://media/picker/cloud/unreviewed";
        new KeeperSelectionStore(activity).toggle(Uri.parse(keeper));
        activity.showPhotos(List.of(Uri.parse(keeper), Uri.parse(cloud)), Map.of(
                keeper, PhotoOrigin.LOCAL,
                cloud, PhotoOrigin.CLOUD));
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        activity.findViewById(R.id.filter_include_keepers).performClick();
        activity.findViewById(R.id.filter_origin_cloud).performClick();
        assertEquals(1, grid.getChildCount());

        activity.findViewById(R.id.filter_origin_all).performClick();

        assertEquals(2, grid.getChildCount());
        assertTrue(activity.findViewById(R.id.filter_include_keepers).isSelected());
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
        assertEquals(Math.round(22 * density), star.getLayoutParams().width);
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

    @Test public void cachedStacksRenderWithoutStartingAnalysisAgain() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        String first = "content://media/photo/cached-1";
        String recommended = "content://media/photo/cached-2";
        List<PhotoFeatures> cachedFeatures = List.of(
                new PhotoFeatures(first, 0, 1, .6),
                new PhotoFeatures(recommended, 1, 2, .9));
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
        assertEquals(View.INVISIBLE,
                activity.findViewById(R.id.analysis_progress_strip).getVisibility());
        TextView badge = (TextView) ((ViewGroup) grid.getChildAt(0)).getChildAt(3);
        assertEquals("2", badge.getText().toString());
    }

    @Test public void onlyNewPhotosEnterAnalysisWhenCachedMediaIsUnchanged() {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        String cached = "content://media/photo/cached";
        String added = "content://media/photo/added";
        new PhotoInsightStore(activity).save(List.of(
                new PhotoFeatures(cached, 0, 19, .8)), Map.of(), Set.of(cached));

        activity.showPhotos(List.of(Uri.parse(cached), Uri.parse(added)));

        assertEquals("Analysing 1 of 2", text(activity, R.id.analysis_progress_label));
        assertEquals(View.VISIBLE,
                activity.findViewById(R.id.analysis_progress_strip).getVisibility());
    }

    @Test public void aStackStaysVisibleUntilItsKeeperIsAddedToAnAlbum() {
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

        new AlbumCompletionStore(activity).mark(first, "Family");
        activity.onResume();

        assertEquals(0, grid.getChildCount());

        activity.findViewById(R.id.filter_include_keepers).performClick();
        assertEquals(1, grid.getChildCount());
        assertEquals(first, grid.getChildAt(0).getTag().toString());
        assertEquals("2", ((TextView) ((android.view.ViewGroup)
                grid.getChildAt(0)).getChildAt(3)).getText());

        activity.findViewById(R.id.filter_keepers).performClick();
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

    @Test public void shortFilteredGridLoadsAnotherPageWithoutAUserScroll() throws Exception {
        ReviewActivity activity = Robolectric.buildActivity(ReviewActivity.class).setup().get();
        java.lang.reflect.Field hasMore = ReviewActivity.class
                .getDeclaredField("hasMorePhotos");
        hasMore.setAccessible(true);
        hasMore.setBoolean(activity, true);
        android.widget.ScrollView scroll = activity.findViewById(R.id.review_scroll);
        GridLayout grid = activity.findViewById(R.id.photo_grid);

        scroll.layout(0, 0, 900, 900);
        grid.layout(0, 0, 900, 300);

        assertEquals(60, activity.reviewLimit());
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertEquals(120, activity.reviewLimit());
        grid.layout(0, 0, 900, 301);
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
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

    private static int id(ReviewActivity activity, String name) {
        int id = activity.getResources().getIdentifier(name, "id", activity.getPackageName());
        assertTrue("Missing view id " + name, id != 0);
        return id;
    }

    private static int selectedCount(ReviewActivity activity) {
        return Integer.parseInt(text(activity, R.id.selection_count).split(" ")[0]);
    }

    private static int dp(ReviewActivity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static void layoutThreeColumnGrid(GridLayout grid, int tileSize) {
        int rows = (grid.getChildCount() + 2) / 3;
        grid.layout(0, 0, tileSize * 3, tileSize * rows);
        for (int index = 0; index < grid.getChildCount(); index++) {
            int column = index % 3;
            int row = index / 3;
            grid.getChildAt(index).layout(column * tileSize, row * tileSize,
                    (column + 1) * tileSize, (row + 1) * tileSize);
        }
    }

    private static void dispatchMove(View view, float localX, float localY) {
        MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE,
                localX, localY, 0);
        view.dispatchTouchEvent(event);
        event.recycle();
    }

    private static void dispatchMoveAtViewport(View view, float viewportX, float viewportY) {
        ScrollView scroll = (ScrollView) view.getParent().getParent();
        dispatchRangeTouch(view, MotionEvent.ACTION_MOVE,
                viewportX + scroll.getScrollX(), viewportY + scroll.getScrollY());
    }

    private static void dispatchRangeUp(View view, float localX, float localY) {
        dispatchRangeTouch(view, MotionEvent.ACTION_UP, localX, localY);
    }

    private static void dispatchRangeTouch(View view, int action, float localX, float localY) {
        MotionEvent event = MotionEvent.obtain(0, 0, action, localX, localY, 0);
        Shadows.shadowOf(view).getOnTouchListener().onTouch(view, event);
        event.recycle();
    }

    private static void dispatchUp(View view, float localX, float localY) {
        MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP,
                localX, localY, 0);
        view.dispatchTouchEvent(event);
        event.recycle();
    }

    private static void dispatchPinch(ReviewActivity activity, int action,
            float firstX, float firstY, float secondX, float secondY, int pointerCount) {
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[pointerCount];
        MotionEvent.PointerCoords[] coordinates = new MotionEvent.PointerCoords[pointerCount];
        for (int index = 0; index < pointerCount; index++) {
            properties[index] = new MotionEvent.PointerProperties();
            properties[index].id = index;
            properties[index].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coordinates[index] = new MotionEvent.PointerCoords();
            coordinates[index].x = index == 0 ? firstX : secondX;
            coordinates[index].y = index == 0 ? firstY : secondY;
            coordinates[index].pressure = 1f;
            coordinates[index].size = 1f;
        }
        MotionEvent event = MotionEvent.obtain(0, 32, action, pointerCount, properties,
                coordinates, 0, 0, 1f, 1f, 0, 0,
                android.view.InputDevice.SOURCE_TOUCHSCREEN, 0);
        activity.dispatchTouchEvent(event);
        event.recycle();
    }
}
