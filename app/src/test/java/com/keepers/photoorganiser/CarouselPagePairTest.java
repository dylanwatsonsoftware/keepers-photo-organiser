package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class CarouselPagePairTest {
    @Test public void promotesLoadedAdjacentPageWithoutRecreatingTheScreen() {
        FrameLayout oldCurrent = new FrameLayout(RuntimeEnvironment.getApplication());
        FrameLayout loadedAdjacent = new FrameLayout(RuntimeEnvironment.getApplication());
        ImageView oldImage = new ImageView(RuntimeEnvironment.getApplication());
        ImageView loadedImage = new ImageView(RuntimeEnvironment.getApplication());
        oldCurrent.setTranslationX(-500);
        loadedAdjacent.setVisibility(View.VISIBLE);
        CarouselPagePair pages = new CarouselPagePair(
                oldCurrent, oldImage, loadedAdjacent, loadedImage);

        pages.promoteAdjacent();

        assertSame(loadedAdjacent, pages.currentSurface());
        assertSame(loadedImage, pages.currentImage());
        assertSame(oldCurrent, pages.adjacentSurface());
        assertSame(oldImage, pages.adjacentImage());
        assertEquals(0f, pages.currentSurface().getTranslationX(), 0.001f);
        assertEquals(View.INVISIBLE, pages.adjacentSurface().getVisibility());
    }
}
