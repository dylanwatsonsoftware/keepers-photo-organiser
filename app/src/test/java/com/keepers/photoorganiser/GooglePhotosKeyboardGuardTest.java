package com.keepers.photoorganiser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;
import org.junit.Test;

public class GooglePhotosKeyboardGuardTest {
    @Test public void dismissesBackOnlyWhenAnInputMethodWindowIsVisible() {
        assertTrue(GooglePhotosKeyboardGuard.shouldDismiss(List.of(
                AccessibilityWindowInfo.TYPE_APPLICATION,
                AccessibilityWindowInfo.TYPE_INPUT_METHOD)));
        assertFalse(GooglePhotosKeyboardGuard.shouldDismiss(List.of(
                AccessibilityWindowInfo.TYPE_APPLICATION,
                AccessibilityWindowInfo.TYPE_SYSTEM)));
    }
}
