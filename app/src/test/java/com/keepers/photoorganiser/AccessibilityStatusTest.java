package com.keepers.photoorganiser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.provider.Settings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AccessibilityStatusTest {
    @Test public void requiresBothAndroidAccessibilityAndThisExactService() {
        android.content.Context context = RuntimeEnvironment.getApplication();
        ComponentName keepers = new ComponentName(context, KeepersAccessibilityService.class);
        Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 1);
        Settings.Secure.putString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                new ComponentName("another.app", "Service").flattenToString());
        assertFalse(AccessibilityStatus.isKeepersEnabled(context));

        Settings.Secure.putString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, keepers.flattenToString());
        assertTrue(AccessibilityStatus.isKeepersEnabled(context));
    }
}
