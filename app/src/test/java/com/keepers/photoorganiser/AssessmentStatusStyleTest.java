package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AssessmentStatusStyleTest {
    @Test public void recommendationStatesUseTheSameStarsAsTheGallery() {
        assertEquals(R.drawable.ic_star, AssessmentStatusStyle.iconRes(true, false));
        assertEquals(R.drawable.ic_star_outline, AssessmentStatusStyle.iconRes(false, true));
        assertEquals(0, AssessmentStatusStyle.iconRes(false, false));
    }
}
