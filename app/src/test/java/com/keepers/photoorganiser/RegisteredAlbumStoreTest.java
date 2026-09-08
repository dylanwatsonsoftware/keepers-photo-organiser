package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class RegisteredAlbumStoreTest {
    @Test public void savesIndependentAlbumsWithOptionalFeaturePhotos() {
        RegisteredAlbumStore store = new RegisteredAlbumStore(
                RuntimeEnvironment.getApplication());
        List<RegisteredAlbum> albums = List.of(
                new RegisteredAlbum("album-1", "Family adventures", "content://cover/1"),
                new RegisteredAlbum("album-2", "School", ""));

        store.save(albums);

        assertEquals(albums, store.load());
    }
}
