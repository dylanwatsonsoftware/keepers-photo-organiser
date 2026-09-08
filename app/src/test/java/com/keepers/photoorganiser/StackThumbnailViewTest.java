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
        assertEquals(true, image.getClipToOutline());
        assertNotNull(image.getBackground());
        View outline = thumbnail.getChildAt(1);
        assertNotNull(outline.getBackground());
        assertEquals(View.VISIBLE, outline.getVisibility());
    }

    @Test public void unselectedPhotoUsesTheSameRoundedCropWithoutAnOutline() {
        FrameLayout selected = StackThumbnailView.create(
                RuntimeEnvironment.getApplication(), true);
        FrameLayout unselected = StackThumbnailView.create(
                RuntimeEnvironment.getApplication(), false);

        ImageView selectedImage = StackThumbnailView.image(selected);
        ImageView unselectedImage = StackThumbnailView.image(unselected);
        FrameLayout.LayoutParams selectedParams =
                (FrameLayout.LayoutParams) selectedImage.getLayoutParams();
        FrameLayout.LayoutParams unselectedParams =
                (FrameLayout.LayoutParams) unselectedImage.getLayoutParams();
        assertEquals(selectedParams.leftMargin, unselectedParams.leftMargin);
        assertEquals(selectedParams.topMargin, unselectedParams.topMargin);
        assertEquals(true, unselectedImage.getClipToOutline());
        assertNotNull(unselectedImage.getBackground());
        assertEquals(View.INVISIBLE, unselected.getChildAt(1).getVisibility());
    }
}
