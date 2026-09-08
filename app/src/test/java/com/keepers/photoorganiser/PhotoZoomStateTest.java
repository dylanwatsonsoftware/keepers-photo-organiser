package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PhotoZoomStateTest {
    @Test public void scaleIsBoundedBetweenRestAndFourTimesZoom() {
        PhotoZoomState zoom = new PhotoZoomState();

        zoom.scaleBy(9f);
        assertEquals(4f, zoom.scale(), 0f);

        zoom.scaleBy(0.01f);
        assertEquals(1f, zoom.scale(), 0f);
        assertFalse(zoom.isZoomed());
    }

    @Test public void panIsClampedToTheVisibleScaledPhoto() {
        PhotoZoomState zoom = new PhotoZoomState();
        zoom.scaleBy(2f);

        zoom.panBy(900f, -700f, 400, 800);

        assertEquals(200f, zoom.translationX(), 0f);
        assertEquals(-400f, zoom.translationY(), 0f);
    }

    @Test public void returningToRestScaleRecentresThePhoto() {
        PhotoZoomState zoom = new PhotoZoomState();
        zoom.scaleBy(2f);
        zoom.panBy(100f, 120f, 400, 800);

        zoom.scaleBy(0.5f);

        assertEquals(1f, zoom.scale(), 0f);
        assertEquals(0f, zoom.translationX(), 0f);
        assertEquals(0f, zoom.translationY(), 0f);
    }

    @Test public void scalingKeepsTheFingerMidpointAnchored() {
        PhotoZoomState zoom = new PhotoZoomState();

        zoom.scaleBy(2f, 300f, 600f, 400, 800);

        assertEquals(2f, zoom.scale(), 0f);
        assertEquals(-100f, zoom.translationX(), 0f);
        assertEquals(-200f, zoom.translationY(), 0f);
    }

    @Test public void repeatedScalingKeepsUsingTheCurrentFingerMidpoint() {
        PhotoZoomState zoom = new PhotoZoomState();
        zoom.scaleBy(2f, 300f, 600f, 400, 800);

        zoom.scaleBy(1.5f, 200f, 400f, 400, 800);

        assertEquals(3f, zoom.scale(), 0f);
        assertEquals(-150f, zoom.translationX(), 0f);
        assertEquals(-300f, zoom.translationY(), 0f);
    }

    @Test public void zoomedPhotoOwnsOneFingerDragInsteadOfPageNavigation() {
        PhotoZoomState zoom = new PhotoZoomState();
        assertTrue(zoom.allowsPageGesture());

        zoom.scaleBy(1.2f);

        assertFalse(zoom.allowsPageGesture());
    }

    @Test public void resetRestoresPhotoForTheNextPage() {
        PhotoZoomState zoom = new PhotoZoomState();
        zoom.scaleBy(3f);
        zoom.panBy(100f, 100f, 400, 800);

        zoom.reset();

        assertEquals(1f, zoom.scale(), 0f);
        assertEquals(0f, zoom.translationX(), 0f);
        assertEquals(0f, zoom.translationY(), 0f);
    }
}
