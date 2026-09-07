package com.keepers.photoorganiser;

import android.content.Intent;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

public final class GooglePhotosIntentFactory {
    public static final String GOOGLE_PHOTOS_PACKAGE = "com.google.android.apps.photos";

    private GooglePhotosIntentFactory() {}

    public static Intent openExisting(Uri photo) {
        return new Intent(Intent.ACTION_VIEW)
                .setDataAndType(photo, "image/*")
                .setPackage(GOOGLE_PHOTOS_PACKAGE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    public static Intent shareExperiment(List<Uri> photos) {
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE)
                .setType("image/*")
                .setPackage(GOOGLE_PHOTOS_PACKAGE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, new ArrayList<>(photos));
        return intent;
    }
}
