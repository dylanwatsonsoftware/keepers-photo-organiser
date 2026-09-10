package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;

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

    @Test public void sessionAllowsAReviewSizedBatch() {
        assertEquals("{\"pickingConfig\":{\"maxItemCount\":\"100\"}}",
                GooglePhotosPickerApi.sessionRequestBody());
    }

    @Test public void extractsEverySelectedItemAndPaginationToken() {
        GooglePhotosPickerApi.MediaPage page = GooglePhotosPickerApi.parseMediaPage(
                "{\"mediaItems\":["
                + "{\"id\":\"photo-1\",\"mediaFile\":{\"baseUrl\":\"https://lh3/one\"}},"
                + "{\"id\":\"photo-2\",\"mediaFile\":{\"baseUrl\":\"https://lh3/two\"}}],"
                + "\"nextPageToken\":\"page-2\"}");

        assertEquals(List.of(
                new GooglePhotosPickerApi.PickedMedia("photo-1", "https://lh3/one=w1200-h1200"),
                new GooglePhotosPickerApi.PickedMedia("photo-2", "https://lh3/two=w1200-h1200")),
                page.items());
        assertEquals("page-2", page.nextPageToken());
    }
}
