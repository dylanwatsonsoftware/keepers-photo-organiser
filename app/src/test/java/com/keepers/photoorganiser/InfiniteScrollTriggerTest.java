package com.keepers.photoorganiser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InfiniteScrollTriggerTest {
    @Test public void triggersOnceNearBottomAndRearmsAfterMovingAway() {
        InfiniteScrollTrigger trigger = new InfiniteScrollTrigger(200);

        assertFalse(trigger.onScroll(300, 500, 1200, true));
        assertTrue(trigger.onScroll(550, 500, 1200, true));
        assertFalse(trigger.onScroll(560, 500, 1200, true));
        assertFalse(trigger.onScroll(600, 500, 1800, true));
        assertTrue(trigger.onScroll(1150, 500, 1800, true));
    }

    @Test public void doesNotTriggerWhenThereAreNoMorePhotos() {
        InfiniteScrollTrigger trigger = new InfiniteScrollTrigger(200);

        assertFalse(trigger.onScroll(800, 500, 1200, false));
    }
}
