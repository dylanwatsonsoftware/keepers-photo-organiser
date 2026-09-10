package com.keepers.photoorganiser;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import android.view.LayoutInflater;
import android.view.View;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ReviewLayoutTest {
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

    @Test public void galleryUsesCompactIconActionsAndSummaryRow() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_review, null);

        assertTrue(findViewWithContentDescription(layout, "Clear keepers") == null);
        assertTrue(layout.findViewById(R.id.open_settings) instanceof ImageButton);
        LinearLayout summary = layout.findViewById(R.id.photo_summary);
        assertNotNull(summary);
        assertTrue(summary.getOrientation() == LinearLayout.HORIZONTAL);
        LinearLayout filters = layout.findViewById(R.id.review_filter_chips);
        assertNotNull(filters);
        assertTrue(filters.getParent() instanceof HorizontalScrollView);
        assertTrue(layout.findViewById(R.id.filter_keepers).getParent() == filters);
        assertTrue(layout.findViewById(R.id.filter_recommended).getParent() == filters);
        assertTrue(layout.findViewById(R.id.filter_hidden).getParent() == filters);
        assertTrue(layout.findViewById(R.id.filter_origin_local).getParent() == filters);
        assertTrue(layout.findViewById(R.id.filter_origin_cloud).getParent() == filters);
        assertTrue(findViewWithText(layout, "On-device photos") == null);
        assertTrue(findViewWithText(layout, "Google Photos") == null);
        View metadata = layout.findViewById(R.id.toggle_metadata);
        assertTrue(metadata instanceof TextView);
        assertTrue(!(metadata instanceof Button));
        assertTrue(metadata.isClickable());
        assertNotNull(metadata.getBackground());
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

        View action = layout.findViewById(R.id.open_album_review);
        assertTrue(action instanceof TextView);
        assertTrue(!(action instanceof Button));
        TextView label = (TextView) action;
        assertTrue("Add to albums".contentEquals(label.getText()));
        assertTrue("Review and add Keepers to Google Photos albums".contentEquals(
                label.getContentDescription()));
        assertNotNull(label.getCompoundDrawablesRelative()[0]);
        assertTrue(label.isClickable());
    }

    @Test public void albumDestinationScreenUsesACompactSingleLineTitle() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_album_review, null);
        android.view.ViewGroup toolbar = (android.view.ViewGroup)
                ((android.view.ViewGroup) layout).getChildAt(0);
        TextView title = (TextView) toolbar.getChildAt(1);

        assertTrue("Choose albums".contentEquals(title.getText()));
        assertTrue(title.getTextSize() <= 24 * title.getResources()
                .getDisplayMetrics().scaledDensity);
        assertTrue(title.getMaxLines() == 1);
    }

    @Test public void galleryCentersQuickReviewIconAndLabelAsOneGroup() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_review, null);

        View quickReview = layout.findViewById(R.id.open_quick_review);
        assertTrue(quickReview instanceof FrameLayout);
        assertTrue(!(quickReview instanceof Button));
        assertNotNull(quickReview.getBackground());
        LinearLayout content = (LinearLayout) ((FrameLayout) quickReview).getChildAt(0);
        assertTrue(content.getGravity() == Gravity.CENTER);
        assertTrue(content.getChildAt(0) instanceof ImageView);
        assertTrue(content.getChildAt(1) instanceof TextView);
        assertTrue("Quick review".contentEquals(
                ((TextView) content.getChildAt(1)).getText()));
        assertTrue(layout.getResources().getIdentifier("export_feedback", "id",
                layout.getContext().getPackageName()) == 0);
    }
}
