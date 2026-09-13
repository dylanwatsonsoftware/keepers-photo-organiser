package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VideoFeatureExtractorTest {
    @Test public void aggregatesBoundedFrameQualityAndDetectsBlackFrames() {
        Bitmap detailed = checkerboard();
        Bitmap black = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        black.eraseColor(Color.BLACK);

        VideoFeatures features = VideoFeatureExtractor.fromFrames(
                "video", 12_000, List.of(detailed, black, detailed));

        assertEquals(VideoFeatures.SCHEMA_VERSION, features.schemaVersion());
        assertEquals(3, features.sampledFrames());
        assertEquals(1d / 3d, features.blackFrameRate(), .001);
        assertTrue(features.detail() > 0);
        assertTrue(features.score() >= 0 && features.score() <= 100);
    }

    private static Bitmap checkerboard() {
        Bitmap bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++)
            bitmap.setPixel(x, y, (x + y) % 2 == 0 ? Color.WHITE : Color.BLACK);
        return bitmap;
    }
}
