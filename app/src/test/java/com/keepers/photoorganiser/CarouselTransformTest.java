package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CarouselTransformTest {
    @Test public void leftDragKeepsNextPhotoOneGutterAfterCurrentPhoto() {
        CarouselTransform transform = CarouselTransform.from(-300, 1080, 16);

        assertEquals(-300f, transform.currentX(), 0.001f);
        assertEquals(796f, transform.adjacentX(), 0.001f);
    }

    @Test public void rightDragKeepsPreviousPhotoOneGutterBeforeCurrentPhoto() {
        CarouselTransform transform = CarouselTransform.from(300, 1080, 16);

        assertEquals(300f, transform.currentX(), 0.001f);
        assertEquals(-796f, transform.adjacentX(), 0.001f);
    }
}
