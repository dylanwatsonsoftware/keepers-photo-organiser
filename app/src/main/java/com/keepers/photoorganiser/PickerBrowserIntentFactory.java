package com.keepers.photoorganiser;

import android.content.Intent;
import android.net.Uri;
import androidx.browser.customtabs.CustomTabsIntent;

final class PickerBrowserIntentFactory {
    private PickerBrowserIntentFactory() {}

    static Intent create(Uri pickerUri) {
        CustomTabsIntent customTab = new CustomTabsIntent.Builder()
                .setShowTitle(false)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .build();
        customTab.intent.setData(pickerUri);
        return customTab.intent;
    }
}
