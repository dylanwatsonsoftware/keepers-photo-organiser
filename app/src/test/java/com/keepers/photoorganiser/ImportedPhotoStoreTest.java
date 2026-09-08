package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

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
}
