package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowSeekBar;

@RunWith(RobolectricTestRunner.class)
public class PreviewQuickReviewTest {
    @Test public void tappingFullscreenPhotoTogglesAllPhotoChrome() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        PreviewActivity activity = create(context,
                Uri.parse("content://photo/fullscreen-chrome-toggle"), false);
        Method swipe = PreviewActivity.class.getDeclaredMethod("handleSwipe", MotionEvent.class);
        swipe.setAccessible(true);

        swipe.invoke(activity, event(MotionEvent.ACTION_DOWN, 100, 100));
        swipe.invoke(activity, event(MotionEvent.ACTION_UP, 100, 100));

        assertEquals(View.INVISIBLE, activity.findViewById(R.id.preview_close).getVisibility());
        assertEquals(View.INVISIBLE, activity.findViewById(R.id.preview_controls).getVisibility());
        assertEquals(View.INVISIBLE,
                activity.findViewById(R.id.preview_start_quick_review).getVisibility());

        swipe.invoke(activity, event(MotionEvent.ACTION_DOWN, 100, 100));
        swipe.invoke(activity, event(MotionEvent.ACTION_UP, 100, 100));

        assertEquals(View.VISIBLE, activity.findViewById(R.id.preview_close).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.preview_controls).getVisibility());
        assertEquals(View.VISIBLE,
                activity.findViewById(R.id.preview_start_quick_review).getVisibility());
    }

    @Test public void tappingQuickReviewCardDoesNotHideDecisionControls() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        PreviewActivity activity = create(context,
                Uri.parse("content://photo/quick-review-no-chrome-toggle"), true);
        Method swipe = PreviewActivity.class.getDeclaredMethod("handleSwipe", MotionEvent.class);
        swipe.setAccessible(true);

        swipe.invoke(activity, event(MotionEvent.ACTION_DOWN, 100, 100));
        swipe.invoke(activity, event(MotionEvent.ACTION_UP, 100, 100));

        assertEquals(View.VISIBLE, activity.findViewById(R.id.preview_controls).getVisibility());
    }

    @Test public void rightSwipeMarksCurrentPhotoAsKeeper() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        Uri photo = Uri.parse("content://photo/quick-keep");
        PreviewActivity activity = Robolectric.buildActivity(PreviewActivity.class,
                new Intent(context, PreviewActivity.class).setData(photo)
                        .putExtra(PreviewActivity.EXTRA_QUICK_REVIEW, true)).setup().get();
        Method swipe = PreviewActivity.class.getDeclaredMethod("handleSwipe", MotionEvent.class);
        swipe.setAccessible(true);

        swipe.invoke(activity, event(MotionEvent.ACTION_DOWN, 100, 100));
        swipe.invoke(activity, event(MotionEvent.ACTION_UP, 220, 100));

        assertTrue(new KeeperSelectionStore(activity).load().contains(photo.toString()));
        assertTrue(((TextView) activity.findViewById(R.id.preview_hint)).getText().toString()
                .contains("right to keep"));
    }

    @Test public void hideActionHidesCurrentPhotoInQuickReview() {
        Context context = RuntimeEnvironment.getApplication();
        Uri photo = Uri.parse("content://photo/quick-hide");
        PreviewActivity activity = create(context, photo, true);

        activity.findViewById(R.id.preview_hide).performClick();

        assertTrue(new HiddenPhotoStore(activity).load().contains(photo.toString()));
    }

    @Test public void hideButtonShowsAnIconInQuickReviewOnly() {
        Context context = RuntimeEnvironment.getApplication();
        Uri photo = Uri.parse("content://photo/quick-hide-icon");
        FrameLayout normalHide = create(context, photo, false).findViewById(R.id.preview_hide);
        FrameLayout quickHide = create(context, photo, true).findViewById(R.id.preview_hide);
        View normalIcon = ((android.view.ViewGroup) normalHide.getChildAt(0)).getChildAt(0);
        View quickIcon = ((android.view.ViewGroup) quickHide.getChildAt(0)).getChildAt(0);

        assertEquals(View.GONE, normalIcon.getVisibility());
        assertEquals(View.VISIBLE, quickIcon.getVisibility());
    }

    @Test public void skipAdvancesWithoutKeepingOrHidingThePhoto() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        ImportedPhotoStore imports = new ImportedPhotoStore(context);
        imports.clear();
        new KeeperSelectionStore(context).clear();
        new HiddenPhotoStore(context).clear();
        Uri first = Uri.parse("content://photo/quick-skip-first");
        Uri second = Uri.parse("content://photo/quick-skip-second");
        imports.add(new ImportedPhoto(first, 20, PhotoOrigin.LOCAL));
        imports.add(new ImportedPhoto(second, 10, PhotoOrigin.LOCAL));
        PreviewActivity activity = create(context, first, true);

        assertEquals(View.VISIBLE, activity.findViewById(R.id.preview_skip).getVisibility());
        activity.findViewById(R.id.preview_skip).performClick();

        assertEquals(second, currentPhoto(activity));
        assertTrue(new KeeperSelectionStore(activity).load().isEmpty());
        assertTrue(new HiddenPhotoStore(activity).load().isEmpty());
        imports.clear();
    }

    @Test public void hidingAStackInQuickReviewSkipsEveryHiddenMember() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        ImportedPhotoStore imports = new ImportedPhotoStore(context);
        imports.clear();
        new HiddenPhotoStore(context).clear();
        Uri first = Uri.parse("content://photo/hide-stack-first");
        Uri sibling = Uri.parse("content://photo/hide-stack-sibling");
        Uri next = Uri.parse("content://photo/after-hidden-stack");
        imports.add(new ImportedPhoto(first, 30, PhotoOrigin.LOCAL));
        imports.add(new ImportedPhoto(sibling, 20, PhotoOrigin.LOCAL));
        imports.add(new ImportedPhoto(next, 10, PhotoOrigin.LOCAL));
        List<String> stack = List.of(first.toString(), sibling.toString());
        new PhotoStackStore(context).save(Map.of(
                first.toString(), stack, sibling.toString(), stack));
        PreviewActivity activity = create(context, first, true);

        activity.findViewById(R.id.preview_hide).performClick();

        assertEquals(Set.of(first.toString(), sibling.toString()),
                new HiddenPhotoStore(activity).load());
        assertEquals(next, currentPhoto(activity));
        imports.clear();
        new HiddenPhotoStore(context).clear();
    }

    @Test public void upwardSwipeDoesNotOpenMetadataInQuickReview() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        PreviewActivity activity = create(context,
                Uri.parse("content://photo/quick-no-metadata-swipe"), true);
        Method swipe = PreviewActivity.class.getDeclaredMethod("handleSwipe", MotionEvent.class);
        swipe.setAccessible(true);

        swipe.invoke(activity, event(MotionEvent.ACTION_DOWN, 100, 300));
        swipe.invoke(activity, event(MotionEvent.ACTION_MOVE, 100, 150));
        swipe.invoke(activity, event(MotionEvent.ACTION_UP, 100, 150));

        assertEquals(View.GONE,
                activity.findViewById(R.id.preview_analysis_sheet).getVisibility());
        assertFalse(((TextView) activity.findViewById(R.id.preview_hint)).getText().toString()
                .contains("Up for details"));
    }

    @Test public void dragRotatesCardButOnlyReleasePastThresholdRecordsDecision()
            throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        Uri photo = Uri.parse("content://photo/quick-release-threshold");
        PreviewActivity activity = Robolectric.buildActivity(PreviewActivity.class,
                new Intent(context, PreviewActivity.class).setData(photo)
                        .putExtra(PreviewActivity.EXTRA_QUICK_REVIEW, true)).setup().get();
        Method swipe = PreviewActivity.class.getDeclaredMethod("handleSwipe", MotionEvent.class);
        swipe.setAccessible(true);

        swipe.invoke(activity, event(MotionEvent.ACTION_DOWN, 100, 100));
        swipe.invoke(activity, event(MotionEvent.ACTION_MOVE, 170, 103));

        assertTrue(activity.findViewById(R.id.preview_current_surface).getRotation() > 0);
        assertEquals(android.view.View.VISIBLE,
                activity.findViewById(R.id.quick_review_keep_indicator).getVisibility());
        assertEquals(android.view.View.GONE,
                activity.findViewById(R.id.quick_review_pass_indicator).getVisibility());
        assertFalse(new KeeperSelectionStore(activity).load().contains(photo.toString()));
        swipe.invoke(activity, event(MotionEvent.ACTION_UP, 170, 103));
        assertFalse(new KeeperSelectionStore(activity).load().contains(photo.toString()));
        assertEquals(android.view.View.GONE,
                activity.findViewById(R.id.quick_review_keep_indicator).getVisibility());

        swipe.invoke(activity, event(MotionEvent.ACTION_DOWN, 100, 100));
        swipe.invoke(activity, event(MotionEvent.ACTION_UP, 210, 100));
        assertTrue(new KeeperSelectionStore(activity).load().contains(photo.toString()));
    }

    @Test public void quickAndNormalNavigationTreatAStackAsOneItem() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        Uri first = Uri.parse("content://photo/stack-first");
        Uri sibling = Uri.parse("content://photo/stack-sibling");
        Uri single = Uri.parse("content://photo/single");
        ImportedPhotoStore imports = new ImportedPhotoStore(context);
        imports.add(new ImportedPhoto(first, 30, PhotoOrigin.LOCAL));
        imports.add(new ImportedPhoto(sibling, 20, PhotoOrigin.LOCAL));
        imports.add(new ImportedPhoto(single, 10, PhotoOrigin.LOCAL));
        List<String> stack = List.of(first.toString(), sibling.toString());
        new PhotoStackStore(context).save(Map.of(
                first.toString(), stack, sibling.toString(), stack));
        new SuggestionStore(context).save(Set.of(sibling.toString()));

        PreviewActivity normal = create(context, first, false);
        PreviewActivity quick = create(context, first, true);

        assertEquals(sibling, currentPhoto(normal));
        assertEquals(sibling, currentPhoto(quick));
        assertEquals(single, navigator(normal).peekNext());
        assertEquals(single, navigator(quick).peekNext());
    }

    @Test public void fullscreenStackCarouselDoesNotExposeHiddenMembers() {
        Context context = RuntimeEnvironment.getApplication();
        ImportedPhotoStore imports = new ImportedPhotoStore(context);
        imports.clear();
        new HiddenPhotoStore(context).clear();
        Uri visible = Uri.parse("content://photo/visible-stack-member");
        Uri hidden = Uri.parse("content://photo/hidden-stack-member");
        imports.add(new ImportedPhoto(visible, 20, PhotoOrigin.LOCAL));
        imports.add(new ImportedPhoto(hidden, 10, PhotoOrigin.LOCAL));
        List<String> stack = List.of(visible.toString(), hidden.toString());
        new PhotoStackStore(context).save(Map.of(
                visible.toString(), stack, hidden.toString(), stack));
        new HiddenPhotoStore(context).hide(Set.of(hidden.toString()));

        PreviewActivity activity = create(context, visible, false);

        assertEquals(0, ((android.view.ViewGroup) activity.findViewById(
                R.id.preview_stack_thumbnails)).getChildCount());
        assertEquals(View.GONE, activity.findViewById(R.id.preview_stack_carousel).getVisibility());
        imports.clear();
        new HiddenPhotoStore(context).clear();
    }

    @Test public void swipingStackCarouselSelectsTheAdjacentStackPhoto() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        ImportedPhotoStore imports = new ImportedPhotoStore(context);
        imports.clear();
        Uri first = Uri.parse("content://photo/carousel-first");
        Uri middle = Uri.parse("content://photo/carousel-middle");
        Uri last = Uri.parse("content://photo/carousel-last");
        imports.add(new ImportedPhoto(first, 30, PhotoOrigin.LOCAL));
        imports.add(new ImportedPhoto(middle, 20, PhotoOrigin.LOCAL));
        imports.add(new ImportedPhoto(last, 10, PhotoOrigin.LOCAL));
        List<String> stack = List.of(first.toString(), middle.toString(), last.toString());
        new PhotoStackStore(context).save(Map.of(first.toString(), stack,
                middle.toString(), stack, last.toString(), stack));
        new SuggestionStore(context).save(Set.of(middle.toString()));
        PreviewActivity activity = create(context, first, false);
        HorizontalScrollView carousel = activity.findViewById(R.id.preview_stack_carousel);
        assertEquals(middle, currentPhoto(activity));

        Method carouselTouch = PreviewActivity.class.getDeclaredMethod(
                "handleStackCarouselTouch", MotionEvent.class);
        carouselTouch.setAccessible(true);
        carouselTouch.invoke(activity, event(MotionEvent.ACTION_DOWN, 220, 40));
        carouselTouch.invoke(activity, event(MotionEvent.ACTION_MOVE, 100, 40));
        carouselTouch.invoke(activity, event(MotionEvent.ACTION_UP, 100, 40));
        org.robolectric.shadows.ShadowLooper.idleMainLooper();

        assertEquals(last, currentPhoto(activity));
        imports.clear();
    }

    @Test public void fullscreenPhotoCanLaunchQuickReviewFromItsCurrentItem() {
        Context context = RuntimeEnvironment.getApplication();
        Uri photo = Uri.parse("content://photo/fullscreen-quick-start");
        PreviewActivity activity = create(context, photo, false);

        activity.findViewById(R.id.preview_start_quick_review).performClick();

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(PreviewActivity.class.getName(), started.getComponent().getClassName());
        assertEquals(photo, started.getData());
        assertTrue(started.getBooleanExtra(PreviewActivity.EXTRA_QUICK_REVIEW, false));
    }

    @Test public void reviewActionIsHiddenWhileMetadataIsOpen() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        PreviewActivity activity = create(context,
                Uri.parse("content://photo/no-review-over-metadata"), false);
        Method showAnalysis = PreviewActivity.class.getDeclaredMethod("showAnalysis");
        showAnalysis.setAccessible(true);

        showAnalysis.invoke(activity);

        assertEquals(View.GONE,
                activity.findViewById(R.id.preview_start_quick_review).getVisibility());
    }

    @Test public void metadataSheetTracksHorizontalDragBeforeChangingPhoto() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        ImportedPhotoStore imports = new ImportedPhotoStore(context);
        imports.clear();
        Uri first = Uri.parse("content://photo/metadata-drag-first");
        Uri second = Uri.parse("content://photo/metadata-drag-second");
        imports.add(new ImportedPhoto(first, 20, PhotoOrigin.LOCAL));
        imports.add(new ImportedPhoto(second, 10, PhotoOrigin.LOCAL));
        PreviewActivity activity = create(context, first, false);
        Method showAnalysis = PreviewActivity.class.getDeclaredMethod("showAnalysis");
        showAnalysis.setAccessible(true);
        showAnalysis.invoke(activity);
        Method scroll = PreviewActivity.class.getDeclaredMethod(
                "handleAnalysisScroll", MotionEvent.class);
        scroll.setAccessible(true);

        scroll.invoke(activity, event(MotionEvent.ACTION_DOWN, 200, 200));
        scroll.invoke(activity, event(MotionEvent.ACTION_MOVE, 100, 204));

        assertTrue(activity.findViewById(R.id.preview_analysis_sheet).getTranslationX() < 0);
        imports.clear();
    }

    @Test public void metadataSheetShowsTheMediaFilename() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        ImportedPhotoStore imports = new ImportedPhotoStore(context);
        imports.clear();
        Uri photo = Uri.parse("content://photo/Favourite%20shot.jpg");
        imports.add(new ImportedPhoto(photo, 20, PhotoOrigin.LOCAL));
        PreviewActivity activity = create(context, photo, false);
        Method showAnalysis = PreviewActivity.class.getDeclaredMethod("showAnalysis");
        showAnalysis.setAccessible(true);

        showAnalysis.invoke(activity);

        int filenameId = activity.getResources().getIdentifier(
                "preview_metadata_filename", "id", activity.getPackageName());
        assertTrue(filenameId != 0);
        assertEquals("File  ·  Favourite shot.jpg",
                ((TextView) activity.findViewById(filenameId)).getText().toString());
        imports.clear();
    }

    @Test public void localVideoUsesPlaybackSurfaceInFullscreen() {
        Context context = RuntimeEnvironment.getApplication();
        ImportedPhotoStore imports = new ImportedPhotoStore(context);
        imports.clear();
        Uri video = Uri.parse("content://media/video/media/preview-video");
        imports.add(new ImportedPhoto(video, 20, PhotoOrigin.LOCAL,
                MediaType.VIDEO, 8_000));

        PreviewActivity activity = create(context, video, false);

        assertEquals(View.VISIBLE, activity.findViewById(R.id.preview_video).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.preview_image).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.preview_video_play).getVisibility());
        imports.clear();
    }

    @Test public void metadataSheetExplainsTheSampledVideoAssessment() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        ImportedPhotoStore imports = new ImportedPhotoStore(context);
        imports.clear();
        Uri video = Uri.parse("content://media/video/media/assessed-video");
        imports.add(new ImportedPhoto(video, 20, PhotoOrigin.LOCAL,
                MediaType.VIDEO, 8_000));
        new VideoInsightStore(context).save(new VideoFeatures(VideoFeatures.SCHEMA_VERSION,
                video.toString(), 8_000, 3, .7, .8, .9, .6, .75, 0, 0));
        PreviewActivity activity = create(context, video, false);
        Method showAnalysis = PreviewActivity.class.getDeclaredMethod("showAnalysis");
        showAnalysis.setAccessible(true);

        showAnalysis.invoke(activity);

        assertEquals("Video assessment · 75/100",
                ((TextView) activity.findViewById(R.id.preview_analysis_title)).getText());
        String body = ((TextView) activity.findViewById(R.id.preview_analysis_body))
                .getText().toString();
        assertTrue(body.contains("3 sampled frames"));
        assertTrue(body.contains("Exposure quality — 90%"));
        imports.clear();
    }

    @Test public void localVideoShowsThumbnailAbovePlaybackSurfaceBeforePlaying() {
        Context context = RuntimeEnvironment.getApplication();
        ImportedPhotoStore imports = new ImportedPhotoStore(context);
        imports.clear();
        Uri video = Uri.parse("content://media/video/media/preview-video-thumbnail");
        imports.add(new ImportedPhoto(video, 20, PhotoOrigin.LOCAL,
                MediaType.VIDEO, 8_000));

        PreviewActivity activity = create(context, video, false);
        FrameLayout surface = activity.findViewById(R.id.preview_current_surface);
        ImageView thumbnail = activity.findViewById(R.id.preview_image);
        VideoView playback = activity.findViewById(R.id.preview_video);

        assertEquals(View.VISIBLE, thumbnail.getVisibility());
        assertTrue(surface.indexOfChild(thumbnail) > surface.indexOfChild(playback));
        imports.clear();
    }

    @Test public void videoPlaybackUsesPaddedImageButtonInsteadOfFontGlyph() {
        Context context = RuntimeEnvironment.getApplication();
        ImportedPhotoStore imports = new ImportedPhotoStore(context);
        imports.clear();
        Uri video = Uri.parse("content://media/video/media/preview-video-icon");
        imports.add(new ImportedPhoto(video, 20, PhotoOrigin.LOCAL,
                MediaType.VIDEO, 8_000));

        PreviewActivity activity = create(context, video, false);
        View control = activity.findViewById(R.id.preview_video_play);

        assertTrue(control instanceof ImageButton);
        assertTrue(((ImageButton) control).getDrawable() != null);
        assertTrue(control.getBackground() != null);
        assertEquals(control.getPaddingLeft(), control.getPaddingRight());
        assertEquals(control.getPaddingTop(), control.getPaddingBottom());
        imports.clear();
    }

    @Test public void replayHidesTheRestoredThumbnailImmediately() {
        Context context = RuntimeEnvironment.getApplication();
        ImportedPhotoStore imports = new ImportedPhotoStore(context);
        imports.clear();
        Uri video = Uri.parse("content://media/video/media/replay-thumbnail");
        imports.add(new ImportedPhoto(video, 20, PhotoOrigin.LOCAL,
                MediaType.VIDEO, 8_000));
        PreviewActivity activity = create(context, video, false);
        ImageView thumbnail = activity.findViewById(R.id.preview_image);
        View play = activity.findViewById(R.id.preview_video_play);
        thumbnail.setVisibility(View.VISIBLE);

        play.performClick();

        assertEquals(View.GONE, thumbnail.getVisibility());
        imports.clear();
    }

    @Test public void videoTimelineShowsDurationAndSeeksFromUserProgress() {
        Context context = RuntimeEnvironment.getApplication();
        ImportedPhotoStore imports = new ImportedPhotoStore(context);
        imports.clear();
        Uri video = Uri.parse("content://media/video/media/scrubbable-video");
        imports.add(new ImportedPhoto(video, 20, PhotoOrigin.LOCAL,
                MediaType.VIDEO, 8_000));
        PreviewActivity activity = create(context, video, false);
        View timeline = activity.findViewById(R.id.preview_video_timeline);
        SeekBar seek = activity.findViewById(R.id.preview_video_seek);
        TextView elapsed = activity.findViewById(R.id.preview_video_elapsed);
        TextView duration = activity.findViewById(R.id.preview_video_duration);
        VideoView playback = activity.findViewById(R.id.preview_video);

        assertEquals(View.VISIBLE, timeline.getVisibility());
        assertEquals(8_000, seek.getMax());
        assertEquals("0:00", elapsed.getText());
        assertEquals("0:08", duration.getText());

        ShadowSeekBar shadowSeek = Shadows.shadowOf(seek);
        shadowSeek.getOnSeekBarChangeListener().onProgressChanged(seek, 3_250, true);

        assertEquals("0:03", elapsed.getText());
        assertEquals(3_250, playback.getCurrentPosition());
        imports.clear();
    }

    @Test public void galleryAutoplayWaitsForTheFullscreenTransition() {
        Context context = RuntimeEnvironment.getApplication();
        ImportedPhotoStore imports = new ImportedPhotoStore(context);
        imports.clear();
        Uri video = Uri.parse("content://media/video/media/gallery-autoplay");
        imports.add(new ImportedPhoto(video, 20, PhotoOrigin.LOCAL,
                MediaType.VIDEO, 8_000));
        Intent request = new Intent(context, PreviewActivity.class).setData(video)
                .putExtra("autoplay_video", true);

        PreviewActivity activity = Robolectric.buildActivity(PreviewActivity.class, request)
                .setup().get();

        assertEquals("Play video", activity.findViewById(R.id.preview_video_play)
                .getContentDescription());
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(300, TimeUnit.MILLISECONDS);
        assertEquals("Pause video", activity.findViewById(R.id.preview_video_play)
                .getContentDescription());
        assertFalse(activity.getIntent().getBooleanExtra("autoplay_video", false));
        imports.clear();
    }

    @Test public void quickReviewLaunchedFromHiddenFullscreenSkipsTheHiddenPhoto()
            throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        ImportedPhotoStore imports = new ImportedPhotoStore(context);
        imports.clear();
        new HiddenPhotoStore(context).clear();
        Uri hidden = Uri.parse("content://photo/hidden-fullscreen");
        Uri visible = Uri.parse("content://photo/visible-fullscreen");
        imports.add(new ImportedPhoto(hidden, 20, PhotoOrigin.LOCAL));
        imports.add(new ImportedPhoto(visible, 10, PhotoOrigin.LOCAL));
        new HiddenPhotoStore(context).hide(Set.of(hidden.toString()));
        PreviewActivity fullscreen = create(context, hidden, false);

        fullscreen.findViewById(R.id.preview_start_quick_review).performClick();
        Intent started = Shadows.shadowOf(fullscreen).getNextStartedActivity();
        PreviewActivity quickReview = Robolectric.buildActivity(PreviewActivity.class, started)
                .setup().get();

        assertEquals(visible, currentPhoto(quickReview));
        imports.clear();
        new HiddenPhotoStore(context).clear();
    }

    @Test public void quickReviewUsesAnInsetElevatedCardDeckAndModeBadge() {
        Context context = RuntimeEnvironment.getApplication();
        Uri photo = Uri.parse("content://photo/card-treatment");
        PreviewActivity normal = create(context, photo, false);
        PreviewActivity quick = create(context, photo, true);

        TextView normalBadge = normal.findViewById(R.id.quick_review_mode_badge);
        TextView quickBadge = quick.findViewById(R.id.quick_review_mode_badge);
        FrameLayout quickCard = quick.findViewById(R.id.preview_current_surface);
        FrameLayout nextCard = quick.findViewById(R.id.preview_adjacent_surface);
        FrameLayout.LayoutParams normalParams = (FrameLayout.LayoutParams) normal
                .findViewById(R.id.preview_current_surface).getLayoutParams();
        FrameLayout.LayoutParams quickParams = (FrameLayout.LayoutParams) quickCard.getLayoutParams();

        assertEquals(View.GONE, normalBadge.getVisibility());
        assertEquals(View.VISIBLE, quickBadge.getVisibility());
        assertTrue(quickBadge.getText().toString().contains("Quick review"));
        assertTrue(quickBadge.getCompoundDrawablesRelative()[0] != null);
        assertEquals(0, normalParams.leftMargin);
        assertTrue(quickParams.leftMargin >= 24);
        assertTrue(quickCard.getClipToOutline());
        assertTrue(quickCard.getForeground() instanceof android.graphics.drawable.GradientDrawable);
        assertTrue(quickCard.getElevation() >= 10);
        assertTrue(nextCard.getScaleX() < 1);
        assertTrue(nextCard.getTranslationY() >= 16);
    }

    private static PreviewActivity create(Context context, Uri photo, boolean quickReview) {
        return Robolectric.buildActivity(PreviewActivity.class,
                new Intent(context, PreviewActivity.class).setData(photo)
                        .putExtra(PreviewActivity.EXTRA_QUICK_REVIEW, quickReview)).setup().get();
    }

    private static PhotoNavigator navigator(PreviewActivity activity) throws Exception {
        Field field = PreviewActivity.class.getDeclaredField("navigator");
        field.setAccessible(true);
        return (PhotoNavigator) field.get(activity);
    }

    private static Uri currentPhoto(PreviewActivity activity) throws Exception {
        Field field = PreviewActivity.class.getDeclaredField("photo");
        field.setAccessible(true);
        return (Uri) field.get(activity);
    }

    private static MotionEvent event(int action, float x, float y) {
        return MotionEvent.obtain(0, 10, action, x, y, 0);
    }
}
