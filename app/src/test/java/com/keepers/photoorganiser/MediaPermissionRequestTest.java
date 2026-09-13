package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.Manifest;
import java.util.List;
import org.junit.Test;

public class MediaPermissionRequestTest {
    @Test public void existingPhotoPermissionStillRequestsNewVideoPermission() {
        assertEquals(List.of(Manifest.permission.READ_MEDIA_VIDEO),
                MediaPermissionRequest.missing(35, true, false, false));
    }

    @Test public void selectedMediaAccessDoesNotRepeatedlyRequestFullLibraryAccess() {
        assertEquals(List.of(), MediaPermissionRequest.missing(35, false, false, true));
    }

    @Test public void selectedMediaGrantDoesNotMaskVideoPermissionMissingFromFullPhotoAccess() {
        assertEquals(List.of(Manifest.permission.READ_MEDIA_VIDEO),
                MediaPermissionRequest.missing(35, true, false, true));
    }

    @Test public void selectedMediaGrantDoesNotMaskPhotoPermissionMissingFromFullVideoAccess() {
        assertEquals(List.of(Manifest.permission.READ_MEDIA_IMAGES),
                MediaPermissionRequest.missing(35, false, true, true));
    }
}
