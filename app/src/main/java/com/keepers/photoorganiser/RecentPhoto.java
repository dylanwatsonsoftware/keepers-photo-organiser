package com.keepers.photoorganiser;

import android.net.Uri;

public record RecentPhoto(Uri uri, long takenAtMillis) {}
