package com.keepers.photoorganiser;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import android.view.LayoutInflater;
import android.view.View;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class ReviewLayoutTest {
    @Test @Config(qualifiers = "night")
    public void galleryUsesAdaptiveDarkPaletteResources() {
        android.content.Context context = RuntimeEnvironment.getApplication();

        assertEquals(0xFF11110F, context.getColor(R.color.gallery_background));
        assertEquals(0xFFF4F1EA, context.getColor(R.color.gallery_text_primary));
        assertEquals(0xFFFF7A7F, context.getColor(R.color.gallery_accent_keeper));
    }

    @Test public void reviewContentRespectsSystemWindowInsets() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_review, null);

        assertTrue(layout.getFitsSystemWindows());
    }

    @Test public void reviewUsesScrollableGridWithLoadingIndicator() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_review, null);

        assertNotNull(layout.findViewById(R.id.review_scroll));
        assertNotNull(layout.findViewById(R.id.review_loading));
    }

    @Test public void galleryUsesCompactKeeperAndFilterControls() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_review, null);

        assertTrue(findViewWithContentDescription(layout, "Clear keepers") == null);
        assertTrue(layout.findViewById(R.id.open_settings) instanceof ImageButton);
        assertTrue(layout.getResources().getIdentifier(
                "select_media_to_share", "id", layout.getContext().getPackageName()) == 0);
        assertNotNull(layout.findViewById(layout.getResources().getIdentifier(
                "selection_count", "id", layout.getContext().getPackageName())));
        assertNotNull(layout.findViewById(layout.getResources().getIdentifier(
                "cancel_selection", "id", layout.getContext().getPackageName())));
        assertEquals(0, layout.getResources().getIdentifier(
                "photo_summary", "id", layout.getContext().getPackageName()));
        View keeper = layout.findViewById(R.id.filter_keepers);
        assertTrue(keeper instanceof TextView);
        assertTrue(keeper.isClickable());
        assertNotNull(keeper.getBackground());
        assertNotNull(((TextView) keeper).getCompoundDrawablesRelative()[0]);
        assertNotNull(layout.findViewById(R.id.open_gallery_filters));
        assertNotNull(layout.findViewById(R.id.gallery_filter_sheet));
        View include = layout.findViewById(R.id.filter_include_keepers);
        assertTrue(include instanceof TextView);
        assertTrue(!(include instanceof Button));
        assertTrue(include.isClickable());
        assertNotNull(include.getBackground());
        assertTrue(findViewWithText(layout, "On-device photos") == null);
        assertTrue(findViewWithText(layout, "Google Photos") == null);
        View metadata = layout.findViewById(R.id.toggle_metadata);
        assertTrue(metadata instanceof TextView);
        assertTrue(!(metadata instanceof Button));
        assertTrue(metadata.isClickable());
        assertNotNull(metadata.getBackground());
    }

    @Test public void keeperShortcutAndFilterActionAreRightAligned() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_review, null);
        LinearLayout controls = layout.findViewById(R.id.gallery_controls);

        assertTrue(controls.getChildAt(0).getLayoutParams() instanceof LinearLayout.LayoutParams);
        LinearLayout.LayoutParams spacer = (LinearLayout.LayoutParams)
                controls.getChildAt(0).getLayoutParams();
        assertTrue(spacer.weight > 0f);
        assertEquals(R.id.filter_keepers, controls.getChildAt(1).getId());
        assertEquals(R.id.open_gallery_filters, controls.getChildAt(2).getId());
    }

    @Test @Config(qualifiers = "night")
    public void settingsAndAlbumsUseAdaptiveGallerySurfaces() {
        android.content.Context context = RuntimeEnvironment.getApplication();
        View settings = LayoutInflater.from(context).inflate(R.layout.activity_people, null);
        View albums = LayoutInflater.from(context).inflate(R.layout.activity_album_review, null);

        assertEquals(context.getColor(R.color.gallery_background),
                ((android.graphics.drawable.ColorDrawable) settings.getBackground()).getColor());
        assertEquals(context.getColor(R.color.gallery_background),
                ((android.graphics.drawable.ColorDrawable) albums.getBackground()).getColor());
    }

    @Test public void gallerySelectionMenuUsesEstablishedStyledActions() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_review, null);
        int actionsId = layout.getResources().getIdentifier(
                "gallery_selection_actions", "id", layout.getContext().getPackageName());

        assertTrue(actionsId != 0);
        for (String name : new String[]{"selection_hide", "selection_share",
                "selection_keeper", "selection_albums"}) {
            int actionId = layout.getResources().getIdentifier(
                    name, "id", layout.getContext().getPackageName());
            assertTrue(actionId != 0);
            View action = layout.findViewById(actionId);
            assertTrue(action instanceof TextView);
            assertTrue(!(action instanceof Button));
            assertTrue(action.isClickable());
            assertNotNull(action.getBackground());
            assertNotNull(((TextView) action).getCompoundDrawables()[1]);
        }
    }

    private static View findViewWithText(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) return view;
        if (!(view instanceof android.view.ViewGroup)) return null;
        android.view.ViewGroup group = (android.view.ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            View match = findViewWithText(group.getChildAt(index), text);
            if (match != null) return match;
        }
        return null;
    }

    private static View findViewWithContentDescription(View view, String description) {
        if (description.equals(String.valueOf(view.getContentDescription()))) return view;
        if (!(view instanceof android.view.ViewGroup)) return null;
        android.view.ViewGroup group = (android.view.ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            View match = findViewWithContentDescription(group.getChildAt(index), description);
            if (match != null) return match;
        }
        return null;
    }

    @Test public void albumActionClearlyDescribesAddingPhotosToAlbums() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_review, null);

        View action = layout.findViewById(R.id.albums_destination);
        assertTrue(action instanceof TextView);
        assertTrue(!(action instanceof Button));
        assertNotNull(action.getBackground());
        assertTrue("Albums".contentEquals(((TextView) action).getText()));
        assertNotNull(((TextView) action).getCompoundDrawables()[1]);
        assertTrue(action.isClickable());
    }

    @Test public void albumDestinationScreenUsesACompactSingleLineTitle() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_album_review, null);
        android.view.ViewGroup toolbar = (android.view.ViewGroup)
                ((android.view.ViewGroup) layout).getChildAt(0);
        TextView title = (TextView) toolbar.getChildAt(1);

        assertTrue("Add to albums".contentEquals(title.getText()));
        assertTrue(title.getTextSize() <= 24 * title.getResources()
                .getDisplayMetrics().scaledDensity);
        assertTrue(title.getMaxLines() == 1);
    }

    @Test public void galleryActionsUseMatchingCenteredIconColumns() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_review, null);

        View quickReview = layout.findViewById(R.id.review_destination);
        View albumReview = layout.findViewById(R.id.albums_destination);
        assertTrue(quickReview instanceof TextView);
        assertTrue(albumReview instanceof TextView);
        assertTrue(!(quickReview instanceof Button));
        assertNotNull(quickReview.getBackground());
        assertNotNull(((TextView) quickReview).getCompoundDrawables()[1]);
        assertNotNull(((TextView) albumReview).getCompoundDrawables()[1]);
        assertTrue("Review".contentEquals(((TextView) quickReview).getText()));
        assertTrue(layout.getResources().getIdentifier("export_feedback", "id",
                layout.getContext().getPackageName()) == 0);
    }
}
