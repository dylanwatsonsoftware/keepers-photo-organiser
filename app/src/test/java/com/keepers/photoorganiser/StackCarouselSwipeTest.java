package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StackCarouselSwipeTest {
    @Test public void horizontalSwipesMoveAcrossStackMembersAndClampAtTheEnds() {
        assertEquals(3, StackCarouselSwipe.targetIndex(2, 5, -100, 24, 72));
        assertEquals(1, StackCarouselSwipe.targetIndex(2, 5, 100, 24, 72));
        assertEquals(4, StackCarouselSwipe.targetIndex(3, 5, -170, 24, 72));
        assertEquals(2, StackCarouselSwipe.targetIndex(2, 5, 10, 24, 72));
        assertEquals(0, StackCarouselSwipe.targetIndex(0, 5, 100, 24, 72));
    }
}
