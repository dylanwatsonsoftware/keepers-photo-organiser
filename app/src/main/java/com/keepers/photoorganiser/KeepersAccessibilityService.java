package com.keepers.photoorganiser;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

public final class KeepersAccessibilityService extends AccessibilityService {
    public static final String PREFS = "automation";
    public static final String ARMED_UNTIL = "favourite_armed_until";

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        long deadline = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(ARMED_UNTIL, 0);
        if (System.currentTimeMillis() > deadline) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        AccessibilityNodeInfo target = findFavourite(root);
        if (target == null) return;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(ARMED_UNTIL).apply();
        AccessibilityNodeInfo clickable = target;
        while (clickable != null && !clickable.isClickable()) clickable = clickable.getParent();
        boolean clicked = clickable != null
                && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        Toast.makeText(this, clicked ? "Keepers tapped Favourite" : "Favourite control was not clickable",
                Toast.LENGTH_LONG).show();
    }

    private AccessibilityNodeInfo findFavourite(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (FavouriteControlMatcher.matches(node.getContentDescription())
                || FavouriteControlMatcher.matches(node.getText())) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findFavourite(node.getChild(i));
            if (found != null) return found;
        }
        return null;
    }

    @Override public void onInterrupt() {}
}
