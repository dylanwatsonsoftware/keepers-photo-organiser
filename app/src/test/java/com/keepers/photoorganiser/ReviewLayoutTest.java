package com.keepers.photoorganiser;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import android.view.LayoutInflater;
import android.view.View;
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
}
