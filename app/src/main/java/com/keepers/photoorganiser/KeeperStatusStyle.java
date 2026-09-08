package com.keepers.photoorganiser;

import android.graphics.Color;

public final class KeeperStatusStyle {
    private KeeperStatusStyle() {}

    public static int heartColor(boolean savedToGooglePhotos) {
        return savedToGooglePhotos ? Color.rgb(66, 133, 244) : Color.rgb(234, 67, 53);
    }
}
