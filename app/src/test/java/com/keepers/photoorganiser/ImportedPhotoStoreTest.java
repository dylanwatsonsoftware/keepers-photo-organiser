package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.net.Uri;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ImportedPhotoStoreTest {
    @Test public void importedPhotosPersistWithLocalOrCloudOriginWithoutDuplicates() {
        Context context = RuntimeEnvironment.getApplication();
        ImportedPhotoStore store = new ImportedPhotoStore(context);
        store.clear();
        store.add(new ImportedPhoto(Uri.parse("content://media/local/1"), 20,
                PhotoOrigin.LOCAL));
        store.add(new ImportedPhoto(Uri.parse("content://media/picker/cloud/2"), 30,
                PhotoOrigin.CLOUD));
        store.add(new ImportedPhoto(Uri.parse("content://media/local/1"), 40,
                PhotoOrigin.LOCAL));

        assertEquals(List.of(
                new ImportedPhoto(Uri.parse("content://media/local/1"), 40, PhotoOrigin.LOCAL),
                new ImportedPhoto(Uri.parse("content://media/picker/cloud/2"), 30, PhotoOrigin.CLOUD)),
                store.load());
    }

    @Test public void originCanBeLookedUpForAlbumSafetyChecks() {
        Context context = RuntimeEnvironment.getApplication();
        ImportedPhotoStore store = new ImportedPhotoStore(context);
        store.clear();
        store.add(new ImportedPhoto(Uri.parse("content://keepers/cloud/google-item-1"), 30,
                PhotoOrigin.CLOUD));

        assertEquals(PhotoOrigin.CLOUD,
                store.originOf("content://keepers/cloud/google-item-1"));
        assertNull(store.originOf("content://media/local/not-imported"));
    }

    @Test public void importedVideosPersistTheirTypeAndDuration() {
        Context context = RuntimeEnvironment.getApplication();
        ImportedPhotoStore store = new ImportedPhotoStore(context);
        store.clear();
        ImportedPhoto video = new ImportedPhoto(Uri.parse("content://media/video/9"), 50,
                PhotoOrigin.LOCAL, MediaType.VIDEO, 12_500);

        store.add(video);

        assertEquals(List.of(video), store.load());
    }
}
