package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.graphics.Bitmap;
import java.io.IOException;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class CloudPhotoBatchImporterTest {
    @Test public void importsEveryReadableSelectionAndReportsPartialFailures() {
        ImportedPhotoStore store = new ImportedPhotoStore(RuntimeEnvironment.getApplication());
        store.clear();
        List<GooglePhotosPickerApi.PickedMedia> selected = List.of(
                new GooglePhotosPickerApi.PickedMedia("one", "https://example/one"),
                new GooglePhotosPickerApi.PickedMedia("broken", "https://example/broken"),
                new GooglePhotosPickerApi.PickedMedia("two", "https://example/two"));
        CloudPhotoBatchImporter importer = new CloudPhotoBatchImporter(
                RuntimeEnvironment.getApplication(), media -> {
                    if (media.id().equals("broken")) throw new IOException("unavailable");
                    return Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
                });

        CloudPhotoBatchImporter.Result result = importer.importAll(selected, 100);

        assertEquals(2, result.importedCount());
        assertEquals(3, result.selectedCount());
        assertEquals(2, store.load().size());
    }
}
