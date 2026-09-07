package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.database.MatrixCursor;
import android.provider.MediaStore;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MediaStoreFavoriteDiagnosticTest {
    @Test public void identifiesFavoriteRowsFromMediaStore() {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                MediaStore.Images.Media._ID, MediaStore.MediaColumns.IS_FAVORITE});
        cursor.addRow(new Object[]{41L, 0});
        cursor.addRow(new Object[]{42L, 1});

        FavoriteDiagnostic result = MediaStoreFavoriteDiagnostic.read(cursor);

        assertEquals(2, result.scannedCount());
        assertEquals(List.of(RecentCameraQuery.itemUri(42)), result.favoriteUris());
    }
}
