package com.keepers.photoorganiser;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;

public final class AccessibilityStatus {
    private AccessibilityStatus() {}

    public static boolean isKeepersEnabled(Context context) {
        if (Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED, 0) != 1) return false;
        String enabled = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        ComponentName expected = new ComponentName(context, KeepersAccessibilityService.class);
        for (String value : enabled.split(":")) {
            ComponentName component = ComponentName.unflattenFromString(value);
            if (expected.equals(component)) return true;
        }
        return false;
    }
}
