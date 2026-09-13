package com.keepers.photoorganiser;

import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;

public final class GooglePhotosKeyboardGuard {
    private GooglePhotosKeyboardGuard() {}

    static boolean shouldDismiss(List<Integer> windowTypes) {
        return windowTypes.contains(AccessibilityWindowInfo.TYPE_INPUT_METHOD);
    }
}
