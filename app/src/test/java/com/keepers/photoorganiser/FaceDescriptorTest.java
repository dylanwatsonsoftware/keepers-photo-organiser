package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Color;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class FaceDescriptorTest {
    @Test public void descriptorIsNormalizedAndUsesOnlyTheFaceBounds() {
        Bitmap image = Bitmap.createBitmap(16, 8, Bitmap.Config.ARGB_8888);
        image.eraseColor(Color.BLACK);
        for (int y = 0; y < 8; y++) for (int x = 8; x < 16; x++)
            image.setPixel(x, y, x < 12 ? Color.WHITE : Color.GRAY);

        FaceObservation rightFace = new FaceObservation("photo", 0,
                .5, 0, 1, 1, -1, -1, -1, 0, 0);
        double[] descriptor = FaceDescriptor.extract(image, rightFace);

        assertEquals(64, descriptor.length);
        double magnitude = 0;
        for (double value : descriptor) magnitude += value * value;
        assertEquals(1, Math.sqrt(magnitude), 0.0001);
        assertTrue(descriptor[0] > descriptor[7]);
    }
}
