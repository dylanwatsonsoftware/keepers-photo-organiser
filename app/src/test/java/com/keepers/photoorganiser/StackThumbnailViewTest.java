package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class StackThumbnailViewTest {
    @Test public void selectedOutlineIsAnOverlayAboveAnInsetPhoto() {
        FrameLayout thumbnail = StackThumbnailView.create(RuntimeEnvironment.getApplication(), true);

        assertEquals(2, thumbnail.getChildCount());
        ImageView image = (ImageView) thumbnail.getChildAt(0);
        FrameLayout.LayoutParams imageParams = (FrameLayout.LayoutParams) image.getLayoutParams();
        assertEquals(3, imageParams.leftMargin);
        assertEquals(3, imageParams.topMargin);
        View outline = thumbnail.getChildAt(1);
        assertNotNull(outline.getBackground());
    }
}
