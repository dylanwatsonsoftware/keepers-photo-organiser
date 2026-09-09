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
        assertEquals("https://photos.google.com/picker/session-1/autoclose", session.pickerUri());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSessionWithoutPickerUri() {
        GooglePhotosPickerApi.parseSession("{\"id\":\"session-1\"}");
    }

    @Test public void detectsFinishedSelectionAndExtractsItsDisplayUrl() {
        assertEquals(true, GooglePhotosPickerApi.selectionIsComplete(
                "{\"mediaItemsSet\":true}"));
        GooglePhotosPickerApi.PickedMedia media = GooglePhotosPickerApi.parseFirstMedia(
                "{\"mediaItems\":[{\"id\":\"partner-photo-id_123\",\"mediaFile\":"
                        + "{\"baseUrl\":\"https://lh3.googleusercontent.com/picker-image\"}}]}");

        assertEquals("partner-photo-id_123", media.id());
        assertEquals("https://lh3.googleusercontent.com/picker-image=w1200-h1200",
                media.displayUrl());
    }
}
