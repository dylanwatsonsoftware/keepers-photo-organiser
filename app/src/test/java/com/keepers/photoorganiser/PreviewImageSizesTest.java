package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PreviewImageSizesTest {
    @Test public void usesFastPreviewThenHighQualityScreenSizedImage() {
        PreviewImageSizes sizes = PreviewImageSizes.forScreen(2400);

        assertEquals(480, sizes.previewPixels());
        assertEquals(2400, sizes.fullPixels());
        assertEquals(3072, PreviewImageSizes.forScreen(4000).fullPixels());
    }
}
