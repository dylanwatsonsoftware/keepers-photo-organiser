package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class PhotoContextClassifierTest {
    @Test public void portraitAndPetContextsCanCoexist() {
        PhotoFeatures photo = new PhotoFeatures("portrait-dog", 0, 0, .7,
                .7, .7, .7, .7, 1, .5, .8, .8);

        PhotoContext context = PhotoContextClassifier.infer(photo,
                List.of(new ImageLabelSignal("Dog", .92)));

        assertTrue(context.probability(PhotoContextType.PORTRAIT) >= .8);
        assertEquals(.92, context.probability(PhotoContextType.PET), .0001);
    }

    @Test public void labelsMapToSeveralUsefulAssessmentContexts() {
        PhotoFeatures photo = new PhotoFeatures("mixed", 0, 0, .7,
                .7, .7, .7, .7, 0, -1, -1, -1);

        PhotoContext context = PhotoContextClassifier.infer(photo, List.of(
                new ImageLabelSignal("Mountain", .9),
                new ImageLabelSignal("Food", .8),
                new ImageLabelSignal("Sports", .7),
                new ImageLabelSignal("Receipt", .6)));

        assertEquals(.9, context.probability(PhotoContextType.LANDSCAPE), .0001);
        assertEquals(.8, context.probability(PhotoContextType.FOOD), .0001);
        assertEquals(.7, context.probability(PhotoContextType.ACTION), .0001);
        assertEquals(.6, context.probability(PhotoContextType.DOCUMENT), .0001);
    }

    @Test public void nightLabelAddsLowLightContextWithoutForcingASubject() {
        PhotoFeatures photo = new PhotoFeatures("dark", 0, 0, .7,
                .7, .7, .7, .7, 0, -1, -1, -1);

        PhotoContext context = PhotoContextClassifier.infer(photo,
                List.of(new ImageLabelSignal("Night", .8)));

        assertEquals(.8, context.probability(PhotoContextType.LOW_LIGHT), .0001);
        assertEquals(PhotoContextType.GENERAL, context.dominantSubject());
    }
}
