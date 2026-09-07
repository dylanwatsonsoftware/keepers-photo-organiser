package com.keepers.photoorganiser;

import android.content.Intent;
import android.net.Uri;

public final class PreviewPageRequest {
    private PreviewPageRequest() {}

    public static Intent forPhoto(Intent current, Uri photo) {
        return new Intent(current).setData(photo);
    }
}
