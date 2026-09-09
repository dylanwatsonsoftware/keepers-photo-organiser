package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class GooglePhotosPickerApiTest {
    @Test public void extractsPickerUriFromCreatedSession() {
        String response = "{\"id\":\"session-1\",\"pickerUri\":"
                + "\"https://photos.google.com/picker/session-1\"}";

        GooglePhotosPickerApi.PickerSession session =
                GooglePhotosPickerApi.parseSession(response);

        assertEquals("session-1", session.id());
        assertEquals("https://photos.google.com/picker/session-1", session.pickerUri());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSessionWithoutPickerUri() {
        GooglePhotosPickerApi.parseSession("{\"id\":\"session-1\"}");
    }

    @Test public void detectsFinishedSelectionAndExtractsItsPersistentMediaId() {
        assertEquals(true, GooglePhotosPickerApi.selectionIsComplete(
                "{\"mediaItemsSet\":true}"));
        assertEquals("partner-photo-id_123", GooglePhotosPickerApi.parseFirstMediaId(
                "{\"mediaItems\":[{\"id\":\"partner-photo-id_123\",\"type\":\"PHOTO\"}]}"));
    }

    @Test public void buildsUploadFreeExperimentalOriginalUrl() {
        assertEquals("https://photos.google.com/lr/photo/partner-photo-id_123",
                GooglePhotosPickerApi.originalPhotoUri("partner-photo-id_123").toString());
    }
}
