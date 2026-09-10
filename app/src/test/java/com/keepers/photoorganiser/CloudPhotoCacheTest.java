package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.net.Uri;
import java.io.File;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class CloudPhotoCacheTest {
    @Test public void sameGoogleMediaItemUsesOnePrivateReviewFileAndStableUri() throws Exception {
        CloudPhotoCache cache = new CloudPhotoCache(RuntimeEnvironment.getApplication());
        Bitmap first = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        Bitmap replacement = Bitmap.createBitmap(3, 3, Bitmap.Config.ARGB_8888);

        Uri firstUri = cache.save("google/item:1", first);
        Uri secondUri = cache.save("google/item:1", replacement);

        assertEquals(firstUri, secondUri);
        assertEquals("content", firstUri.getScheme());
        assertEquals("com.keepers.photoorganiser.cloud", firstUri.getAuthority());
        File[] files = new File(RuntimeEnvironment.getApplication().getFilesDir(),
                "cloud-review").listFiles();
        assertTrue(files != null && files.length == 1);
    }
}
