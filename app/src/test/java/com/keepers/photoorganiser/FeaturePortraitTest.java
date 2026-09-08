package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.graphics.Bitmap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class FeaturePortraitTest {
    @Test public void resolvesTheExplicitFeatureFaceForAPerson() {
        FaceObservation first = face("content://photos/first", 0);
        FaceObservation chosen = face("content://photos/chosen", 2);

        FaceObservation result = FeaturePortrait.resolve("ada", List.of(first, chosen),
                Map.of("ada", FaceCorrectionStore.key(chosen)));

        assertEquals(chosen, result);
    }

    @Test public void featureCropIncludesSubstantiallyMoreContextThanTheFaceBounds() {
        Bitmap source = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888);
        FaceObservation face = new FaceObservation("content://photos/ada", 0,
                .4, .4, .6, .6, -1, -1, -1, 0, 0, "1,0,0");

        Bitmap crop = FeaturePortrait.crop(source, face);

        assertEquals(560, crop.getWidth());
        assertEquals(560, crop.getHeight());
    }

    @Test public void featurePortraitUsesAHighResolutionOriginalStage() {
        assertEquals(640, FeaturePortrait.PREVIEW_PIXELS);
        assertEquals(1800, FeaturePortrait.FULL_PIXELS);
    }

    private static FaceObservation face(String photo, int index) {
        return new FaceObservation(photo, index, .3, .3, .6, .7,
                -1, -1, -1, 0, 0, "1,0,0");
    }
}
