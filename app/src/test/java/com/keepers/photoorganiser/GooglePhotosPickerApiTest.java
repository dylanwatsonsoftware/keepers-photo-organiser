package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GooglePhotosPickerApiTest {
    @Test public void extractsPickerUriFromCreatedSession() {
        String response = "{\"id\":\"session-1\",\"pickerUri\":"
                + "\"https://photos.google.com/picker/session-1\"}";

        assertEquals("https://photos.google.com/picker/session-1",
                GooglePhotosPickerApi.parsePickerUri(response));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSessionWithoutPickerUri() {
        GooglePhotosPickerApi.parsePickerUri("{\"id\":\"session-1\"}");
    }
}
