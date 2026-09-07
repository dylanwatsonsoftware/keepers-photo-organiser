package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class AnalysisSheetTransformTest {
    @Test public void photoAndSheetStayEdgeConnectedDuringUpwardDrag() {
        AnalysisSheetTransform halfway = AnalysisSheetTransform.from(-150, 300, 60);
        AnalysisSheetTransform fullyOpen = AnalysisSheetTransform.from(-400, 300, 60);

        assertEquals(-150f, halfway.photoTranslationY(), 0.001f);
        assertEquals(90f, halfway.sheetTranslationY(), 0.001f);
        assertEquals(-240f, fullyOpen.photoTranslationY(), 0.001f);
        assertEquals(0f, fullyOpen.sheetTranslationY(), 0.001f);
    }
}
