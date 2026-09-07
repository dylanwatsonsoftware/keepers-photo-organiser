package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class AnalysisSheetTransformTest {
    @Test public void sheetFollowsUpwardDragFromBelowTheScreen() {
        AnalysisSheetTransform halfway = AnalysisSheetTransform.from(-150, 300);
        AnalysisSheetTransform fullyOpen = AnalysisSheetTransform.from(-400, 300);

        assertEquals(150f, halfway.translationY(), 0.001f);
        assertEquals(0.5f, halfway.alpha(), 0.001f);
        assertEquals(0f, fullyOpen.translationY(), 0.001f);
        assertEquals(1f, fullyOpen.alpha(), 0.001f);
    }
}
