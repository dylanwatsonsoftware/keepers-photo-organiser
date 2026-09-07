package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HeartIconStyleTest {
    @Test public void filledHeartCompensatesForMissingOutlineStroke() {
        assertEquals(6, HeartIconStyle.paddingDp(false));
        assertEquals(5, HeartIconStyle.paddingDp(true));
    }
}
