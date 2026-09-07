package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.net.Uri;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PhotoNavigatorTest {
    @Test public void movesBothDirectionsAndStopsAtBoundaries() {
        Uri newest = Uri.parse("content://photo/1");
        Uri middle = Uri.parse("content://photo/2");
        Uri oldest = Uri.parse("content://photo/3");
        PhotoNavigator navigator = new PhotoNavigator(List.of(newest, middle, oldest), middle);

        assertEquals(oldest, navigator.next());
        assertEquals(oldest, navigator.next());
        assertEquals(middle, navigator.previous());
        assertEquals(newest, navigator.previous());
        assertEquals(newest, navigator.previous());
    }

    @Test public void startsAtFirstPhotoWhenRequestedPhotoIsMissing() {
        Uri first = Uri.parse("content://photo/1");
        PhotoNavigator navigator = new PhotoNavigator(List.of(first), Uri.parse("content://gone"));

        assertEquals(first, navigator.current());
    }
}
