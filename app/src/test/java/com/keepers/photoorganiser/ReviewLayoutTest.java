package com.keepers.photoorganiser;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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

        assertTrue(layout.findViewById(R.id.clear_keepers) instanceof ImageButton);
        assertTrue(layout.findViewById(R.id.open_settings) instanceof ImageButton);
        LinearLayout summary = layout.findViewById(R.id.photo_summary);
        assertNotNull(summary);
        assertTrue(summary.getOrientation() == LinearLayout.HORIZONTAL);
        assertNotNull(layout.findViewById(R.id.filter_keepers));
        assertNotNull(layout.findViewById(R.id.filter_recommended));
        View googlePhotos = layout.findViewById(R.id.import_google_photos);
        assertTrue(googlePhotos instanceof TextView);
        assertTrue(!(googlePhotos instanceof Button));
        assertTrue(googlePhotos.isClickable());
        View metadata = layout.findViewById(R.id.toggle_metadata);
        assertTrue(metadata instanceof TextView);
        assertTrue(!(metadata instanceof Button));
        assertTrue(metadata.isClickable());
        assertNotNull(metadata.getBackground());
    }

    @Test public void albumReviewActionUsesTheCompactKeepersCallToAction() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_review, null);

        View action = layout.findViewById(R.id.open_album_review);
        assertTrue(action instanceof TextView);
        assertTrue(!(action instanceof Button));
        TextView label = (TextView) action;
        assertTrue("Review albums".contentEquals(label.getText()));
        assertNotNull(label.getCompoundDrawablesRelative()[0]);
        assertTrue(label.isClickable());
    }

    @Test public void galleryOffersStyledQuickReviewAndFeedbackExportActions() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_review, null);

        View quickReview = layout.findViewById(R.id.open_quick_review);
        View export = layout.findViewById(R.id.export_feedback);
        assertTrue(quickReview instanceof TextView);
        assertTrue(export instanceof TextView);
        assertTrue(!(quickReview instanceof Button));
        assertTrue(!(export instanceof Button));
        assertNotNull(quickReview.getBackground());
        assertNotNull(export.getBackground());
    }
}
