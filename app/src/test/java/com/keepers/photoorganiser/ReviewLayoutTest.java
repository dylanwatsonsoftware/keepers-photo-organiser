package com.keepers.photoorganiser;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
    }
}
