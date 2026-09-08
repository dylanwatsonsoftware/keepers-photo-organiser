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

    @Test public void pullingDownFromOpenRevealsThePhotoAndCanClose() {
        AnalysisSheetTransform pull = AnalysisSheetTransform.fromOpenPull(90, 300, 60);

        assertEquals(-150f, pull.photoTranslationY(), 0.001f);
        assertEquals(90f, pull.sheetTranslationY(), 0.001f);
        assertEquals(true, AnalysisSheetTransform.shouldClose(90, 64));
        assertEquals(false, AnalysisSheetTransform.shouldClose(40, 64));
    }

    @Test public void aShortIntentionalUpwardPullCommitsOnceTheSheetHasStarted() {
        assertEquals(true, AnalysisSheetTransform.shouldOpen(-28, 24));
        assertEquals(false, AnalysisSheetTransform.shouldOpen(-12, 24));
        assertEquals(false, AnalysisSheetTransform.shouldOpen(28, 24));
    }
}
