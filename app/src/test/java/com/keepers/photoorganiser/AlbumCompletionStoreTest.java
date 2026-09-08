package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AlbumCompletionStoreTest {
    @Test public void listsCompletedAlbumNamesForAPhotoWithDisplayCapitalisation() {
        Context context = RuntimeEnvironment.getApplication();
        AlbumCompletionStore store = new AlbumCompletionStore(context);
        store.clear();
        store.mark("content://photos/one", "Charlie Photos");
        store.mark("content://photos/one", "Family Adventures");
        store.mark("content://photos/two", "Other Album");

        assertEquals(List.of("Charlie Photos", "Family Adventures"),
                store.albumNames("content://photos/one"));
    }
}
