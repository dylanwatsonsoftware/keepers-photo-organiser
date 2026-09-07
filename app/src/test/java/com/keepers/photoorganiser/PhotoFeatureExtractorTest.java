package com.keepers.photoorganiser;

import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Color;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PhotoFeatureExtractorTest {
    @Test public void detailedImageScoresHigherThanFlatImage() {
        Bitmap flat = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        flat.eraseColor(Color.GRAY);
        Bitmap detailed = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
            detailed.setPixel(x, y, (x + y) % 2 == 0 ? Color.BLACK : Color.WHITE);
        }

        assertTrue(PhotoFeatureExtractor.quality(detailed)
                > PhotoFeatureExtractor.quality(flat));
    }

    @Test public void smallBrightnessChangeKeepsHashVisuallyClose() {
        Bitmap first = gradient(0);
        Bitmap brighter = gradient(20);

        assertTrue(Long.bitCount(PhotoFeatureExtractor.hash(first)
                ^ PhotoFeatureExtractor.hash(brighter)) <= 4);
    }

    @Test public void balancedExposureScoresHigherThanClippedExposure() {
        Bitmap balanced = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        balanced.eraseColor(Color.rgb(128, 128, 128));
        Bitmap clipped = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        clipped.eraseColor(Color.WHITE);

        assertTrue(PhotoFeatureExtractor.assess(balanced).exposure()
                > PhotoFeatureExtractor.assess(clipped).exposure());
    }

    @Test public void edgeDetailIsReportedSeparatelyFromMotionStability() {
        PhotoQualityAssessment assessment = PhotoFeatureExtractor.assess(gradient(0));

        assertTrue(assessment.detail() >= 0 && assessment.detail() <= 1);
        assertTrue(assessment.motionStability() >= 0 && assessment.motionStability() <= 1);
    }

    private static Bitmap gradient(int offset) {
        Bitmap bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
            int value = Math.min(255, offset + x * 5);
            bitmap.setPixel(x, y, Color.rgb(value, value, value));
        }
        return bitmap;
    }
}
