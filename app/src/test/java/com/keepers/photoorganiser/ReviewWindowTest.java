package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReviewWindowTest {
    @Test public void expandsBySixtyWithoutAFixedCeiling() {
        ReviewWindow window = new ReviewWindow();
        assertEquals(60, window.limit());
        window.expand();
        assertEquals(120, window.limit());
        for (int count = 0; count < 10; count++) window.expand();
        assertEquals(720, window.limit());
    }
}
