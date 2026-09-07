package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.net.Uri;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class GooglePhotosIntentFactoryTest {
    private static final String GOOGLE_PHOTOS = "com.google.android.apps.photos";

    @Test
    public void openExistingTargetsGooglePhotosWithoutSendingAFile() {
        Uri photo = Uri.parse("content://media/photo/7");

        Intent intent = GooglePhotosIntentFactory.openExisting(photo);

        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals(photo, intent.getData());
        assertEquals(GOOGLE_PHOTOS, intent.getPackage());
        assertTrue((intent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
    }

    @Test
    public void shareExperimentTargetsGooglePhotosWithEverySelectedUri() {
        List<Uri> photos = Arrays.asList(
                Uri.parse("content://media/photo/7"),
                Uri.parse("content://media/photo/8"));

        Intent intent = GooglePhotosIntentFactory.shareExperiment(photos);

        assertEquals(Intent.ACTION_SEND_MULTIPLE, intent.getAction());
        assertEquals(GOOGLE_PHOTOS, intent.getPackage());
        assertEquals(photos, intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM));
        assertTrue((intent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
    }
}
