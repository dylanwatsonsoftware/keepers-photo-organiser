package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

@RunWith(RobolectricTestRunner.class)
public class PreviewQuickReviewTest {
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
