package com.keepers.photoorganiser;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

public final class KeepersAccessibilityService extends AccessibilityService {
    public static final String PREFS = "automation";
    public static final String ARMED_UNTIL = "favourite_armed_until";
    public static final String ALBUM_ARMED_UNTIL = "album_armed_until";
    public static final String ALBUM_NAME = "album_name";
    public static final String ALBUM_PHASE = "album_phase";

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (runAlbumStep()) return;
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (System.currentTimeMillis() > prefs.getLong(ARMED_UNTIL, 0)) return;
        AccessibilityNodeInfo target = findFavourite(getRootInActiveWindow());
        if (target == null) return;
        prefs.edit().remove(ARMED_UNTIL).apply();
        toast(click(target) ? "Keepers tapped Favourite" : "Favourite control was not clickable");
    }

    private boolean runAlbumStep() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (System.currentTimeMillis() > prefs.getLong(ALBUM_ARMED_UNTIL, 0)) return false;
        String album = prefs.getString(ALBUM_NAME, "");
        int phase = prefs.getInt(ALBUM_PHASE, 0);
        AccessibilityNodeInfo target = phase == 0
                ? findAdd(getRootInActiveWindow()) : findAlbum(getRootInActiveWindow(), album);
        if (target == null) return false;
        if (phase == 0) prefs.edit().putInt(ALBUM_PHASE, 1).apply();
        else prefs.edit().remove(ALBUM_ARMED_UNTIL).remove(ALBUM_NAME).remove(ALBUM_PHASE).apply();
        toast(click(target) ? (phase == 0 ? "Keepers opened album picker" : "Keepers selected " + album)
                : "Album control was not clickable");
        return true;
    }

    private AccessibilityNodeInfo findFavourite(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (FavouriteControlMatcher.matches(node.getContentDescription())
                || FavouriteControlMatcher.matches(node.getText())) return node;
        return findChild(node, 0, null);
    }

    private AccessibilityNodeInfo findAdd(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (AlbumControlMatcher.isAddToAlbum(node.getContentDescription())
                || AlbumControlMatcher.isAddToAlbum(node.getText())) return node;
        return findChild(node, 1, null);
    }

    private AccessibilityNodeInfo findAlbum(AccessibilityNodeInfo node, String album) {
        if (node == null) return null;
        if (AlbumControlMatcher.isAlbum(node.getContentDescription(), album)
                || AlbumControlMatcher.isAlbum(node.getText(), album)) return node;
        return findChild(node, 2, album);
    }

    private AccessibilityNodeInfo findChild(AccessibilityNodeInfo node, int mode, String album) {
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo found = mode == 0 ? findFavourite(child)
                    : mode == 1 ? findAdd(child) : findAlbum(child, album);
            if (found != null) return found;
        }
        return null;
    }

    private boolean click(AccessibilityNodeInfo node) {
        while (node != null && !node.isClickable()) node = node.getParent();
        return node != null && node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    @Override public void onInterrupt() {}
}
