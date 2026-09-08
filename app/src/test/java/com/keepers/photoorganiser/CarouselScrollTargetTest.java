package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CarouselScrollTargetTest {
    @Test public void centersSelectedThumbnailAndClampsAtBothEnds() {
        assertEquals(0, CarouselScrollTarget.centered(0, 72, 320, 720));
        assertEquals(380, CarouselScrollTarget.centered(7, 72, 320, 720));
        assertEquals(400, CarouselScrollTarget.centered(9, 72, 320, 720));
    }
}
