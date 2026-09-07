package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SwipeDirectionTest {
    @Test public void classifiesDominantSwipesAndIgnoresShortMovement() {
        assertEquals(SwipeDirection.NEXT, SwipeDirection.classify(-120, 10, 64));
        assertEquals(SwipeDirection.PREVIOUS, SwipeDirection.classify(120, 10, 64));
        assertEquals(SwipeDirection.BACK, SwipeDirection.classify(10, 120, 64));
        assertEquals(SwipeDirection.NONE, SwipeDirection.classify(20, 30, 64));
        assertEquals(SwipeDirection.NONE, SwipeDirection.classify(5, -120, 64));
    }
}
