package com.keepers.photoorganiser;

import android.net.Uri;

public record ImportedPhoto(Uri uri, long takenAtMillis, PhotoOrigin origin) {}
