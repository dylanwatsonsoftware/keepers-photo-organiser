package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.net.Uri;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MediaUriResolverTest {
    @Test
    public void mediaStoreUrisPassThroughUnchanged() {
        Uri media = Uri.parse("content://media/external/images/media/42");
        MediaUriResolver resolver = new MediaUriResolver(uri -> null);

        assertEquals(List.of(media), resolver.resolveAll(List.of(media)));
    }

    @Test
    public void documentUrisAreConvertedToMediaStoreItems() {
        Uri document = Uri.parse("content://com.android.providers.media.documents/document/image%3A42");
        Uri media = Uri.parse("content://media/external/images/media/42");
        MediaUriResolver resolver = new MediaUriResolver(uri -> media);

        assertEquals(List.of(media), resolver.resolveAll(List.of(document)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void selectionFailsWhenAnyItemHasNoLocalMediaStoreEquivalent() {
        Uri local = Uri.parse("content://media/external/images/media/42");
        Uri cloudOnly = Uri.parse("content://cloud.provider/photo/99");
        MediaUriResolver resolver = new MediaUriResolver(uri -> null);

        resolver.resolveAll(Arrays.asList(local, cloudOnly));
    }
}
