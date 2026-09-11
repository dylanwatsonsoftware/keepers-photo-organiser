package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.net.Uri;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class RecentMediaWindowTest {
    @Test public void capsLargeImportsToTheCurrentReviewWindow() {
        ArrayList<ImportedPhoto> imported = new ArrayList<>();
        for (int index = 0; index < 200; index++) imported.add(new ImportedPhoto(
                Uri.parse("content://media/imported/" + index), 1_000 - index,
                PhotoOrigin.LOCAL));

        RecentMediaWindow.Result result = RecentMediaWindow.combine(
                List.of(), imported, ReviewWindow.PAGE_SIZE);

        assertEquals(ReviewWindow.PAGE_SIZE, result.items().size());
        assertTrue(result.hasMore());
    }

    @Test public void mergesByRecencyAndDeduplicatesMediaUris() {
        Uri shared = Uri.parse("content://media/shared");
        RecentMediaWindow.Result result = RecentMediaWindow.combine(
                List.of(new RecentPhoto(shared, 30),
                        new RecentPhoto(Uri.parse("content://media/local"), 20)),
                List.of(new ImportedPhoto(shared, 10, PhotoOrigin.LOCAL),
                        new ImportedPhoto(Uri.parse("content://media/import"), 40,
                                PhotoOrigin.CLOUD)), 10);

        assertEquals(List.of("content://media/import", "content://media/shared",
                        "content://media/local"),
                result.items().stream().map(item -> item.media().uri().toString()).toList());
    }
}
