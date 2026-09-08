package com.keepers.photoorganiser;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import android.content.Intent;

public final class KeepersAccessibilityService extends AccessibilityService {
    public static final String PREFS = "automation";
    public static final String ARMED_UNTIL = "favourite_armed_until";
    public static final String ALBUM_ARMED_UNTIL = "album_armed_until";
    public static final String ALBUM_NAME = "album_name";
    public static final String ALBUM_PHASE = "album_phase";
    static final int PHASE_ADD_TO = 0;
    private static final int PHASE_ALBUM_PICKER = 1;
    private static final int PHASE_FIND_OR_SEARCH = 2;
    private static final int PHASE_TYPE_SEARCH = 3;
    private static final int PHASE_SELECT_RESULT = 4;
    private static final int PHASE_CONFIRM_ALBUM = 5;
    private AlbumStepRetryScheduler albumRetry;

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        albumRetry = new AlbumStepRetryScheduler(new Handler(Looper.getMainLooper()),
                this::retryAlbumStep);
        keepAlbumRetryAlive();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (runAlbumStep()) {
            keepAlbumRetryAlive();
            return;
        }
        keepAlbumRetryAlive();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (System.currentTimeMillis() > prefs.getLong(ARMED_UNTIL, 0)) return;
        AccessibilityNodeInfo target = findFavourite(getRootInActiveWindow());
        if (target == null) return;
        prefs.edit().remove(ARMED_UNTIL).apply();
        toast(click(target) ? "Keepers tapped Favourite" : "Favourite control was not clickable");
    }

    private void retryAlbumStep() {
        if (!albumActionIsArmed()) return;
        runAlbumStep();
        keepAlbumRetryAlive();
    }

    private void keepAlbumRetryAlive() {
        if (albumRetry != null) albumRetry.ensureScheduled(albumActionIsArmed());
    }

    private boolean albumActionIsArmed() {
        return System.currentTimeMillis() <= getSharedPreferences(PREFS, MODE_PRIVATE)
                .getLong(ALBUM_ARMED_UNTIL, 0);
    }

    private boolean runAlbumStep() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (System.currentTimeMillis() > prefs.getLong(ALBUM_ARMED_UNTIL, 0)) return false;
        String album = prefs.getString(ALBUM_NAME, "");
        int phase = prefs.getInt(ALBUM_PHASE, 0);
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        if (phase == PHASE_ADD_TO) return runAddStep(root, prefs);
        if (phase == PHASE_ALBUM_PICKER) {
            AccessibilityNodeInfo picker = findAlbumPickerOption(root);
            if (picker == null) return false;
            if (!click(picker)) return actionFailed("Album picker was not clickable");
            advance(prefs, PHASE_FIND_OR_SEARCH);
            toast("Keepers opened album picker");
            return true;
        }
        if (phase == PHASE_FIND_OR_SEARCH) {
            AccessibilityNodeInfo visibleAlbum = findAlbum(root, album);
            if (visibleAlbum != null) return selectAlbum(visibleAlbum, album, prefs);
            AccessibilityNodeInfo search = findAlbumSearch(root);
            if (search == null) return false;
            if (!click(search)) return actionFailed("Album search was not clickable");
            advance(prefs, PHASE_TYPE_SEARCH);
            toast("Keepers opened album search");
            return true;
        }
        if (phase == PHASE_TYPE_SEARCH) {
            AccessibilityNodeInfo searchField = findEditableSearch(root);
            if (searchField == null) return false;
            Bundle arguments = new Bundle();
            arguments.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, album);
            searchField.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            if (!searchField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
                return actionFailed("Album search field could not be filled");
            }
            advance(prefs, PHASE_SELECT_RESULT);
            toast("Keepers searched for " + album);
            return true;
        }
        if (phase == PHASE_SELECT_RESULT) {
            AccessibilityNodeInfo result = findAlbumSearchResult(root, album);
            return result != null && selectAlbum(result, album, prefs);
        }
        if (phase == PHASE_CONFIRM_ALBUM) {
            boolean returned = AlbumSelectionConfirmation.returnedToPhoto(
                    findAdd(root) != null, findAlbumSearch(root) != null,
                    findEditableSearch(root) != null);
            if (!returned) return false;
            prefs.edit().remove(ALBUM_ARMED_UNTIL).remove(ALBUM_NAME)
                    .remove(ALBUM_PHASE).apply();
            toast("Keepers confirmed this photo in " + album);
            startNextApprovedAlbumAction();
            return true;
        }
        return false;
    }

    private boolean runAddStep(AccessibilityNodeInfo root, SharedPreferences prefs) {
        AccessibilityNodeInfo add = findAdd(root);
        if (add == null) return false;
        if (!click(add)) return actionFailed("Add to control was not clickable");
        advance(prefs, PHASE_ALBUM_PICKER);
        toast("Keepers opened Add to");
        return true;
    }

    private boolean selectAlbum(AccessibilityNodeInfo albumNode, String album,
            SharedPreferences prefs) {
        if (!click(albumNode)) return actionFailed("Album was not clickable");
        advance(prefs, PHASE_CONFIRM_ALBUM);
        toast("Keepers selected " + album + " · confirming");
        return true;
    }

    private void advance(SharedPreferences prefs, int phase) {
        prefs.edit().putInt(ALBUM_PHASE, phase).apply();
    }

    private boolean actionFailed(String message) {
        toast(message);
        return true;
    }

    private void startNextApprovedAlbumAction() {
        AlbumActionQueueStore queue = new AlbumActionQueueStore(this);
        if (!queue.isActive()) return;
        int totalCount = queue.totalCount();
        AlbumAction next = queue.completeCurrent();
        if (next == null) {
            toast("All approved album changes are complete");
            startActivity(new Intent(this, AlbumReviewActivity.class)
                    .putExtra(AlbumReviewActivity.EXTRA_COMPLETED_COUNT, totalCount)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return;
        }
        Intent intent = AlbumAutomationCoordinator.arm(this, next)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private AccessibilityNodeInfo findFavourite(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (FavouriteControlMatcher.matches(node.getContentDescription())
                || FavouriteControlMatcher.matches(node.getText())) return node;
        return findChild(node, 0, null);
    }

    private AccessibilityNodeInfo findAlreadyFavourite(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (FavouriteControlMatcher.isAlreadyFavourite(node.getContentDescription())
                || FavouriteControlMatcher.isAlreadyFavourite(node.getText())) return node;
        return findChild(node, 4, null);
    }

    private AccessibilityNodeInfo findMoreOptions(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (FavouriteControlMatcher.isMoreOptions(node.getContentDescription())
                || FavouriteControlMatcher.isMoreOptions(node.getText())) return node;
        return findChild(node, 7, null);
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

    private AccessibilityNodeInfo findAlbumSearchResult(
            AccessibilityNodeInfo node, String album) {
        if (node == null) return null;
        if (AlbumControlMatcher.isAlbumSearchResult(node.getContentDescription(), album,
                insideEditableSearch(node))
                || AlbumControlMatcher.isAlbumSearchResult(node.getText(), album,
                insideEditableSearch(node))) return node;
        // Google Photos repeats the query in the top bar before the actual result row.
        // Search from the bottom so the visible result wins even when the header is not
        // exposed as an editable accessibility node on a particular Photos version.
        for (int index = node.getChildCount() - 1; index >= 0; index--) {
            AccessibilityNodeInfo result = findAlbumSearchResult(node.getChild(index), album);
            if (result != null) return result;
        }
        return null;
    }

    private boolean insideEditableSearch(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        while (current != null) {
            if (current.isEditable()) return true;
            current = current.getParent();
        }
        return false;
    }

    private AccessibilityNodeInfo findAlbumPickerOption(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (AlbumControlMatcher.isAlbumPickerOption(node.getContentDescription())
                || AlbumControlMatcher.isAlbumPickerOption(node.getText())) return node;
        return findChild(node, 3, null);
    }

    private AccessibilityNodeInfo findAlbumSearch(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (AlbumControlMatcher.isAlbumSearch(node.getContentDescription())
                || AlbumControlMatcher.isAlbumSearch(node.getText())) return node;
        return findChild(node, 5, null);
    }

    private AccessibilityNodeInfo findEditableSearch(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isEditable()) return node;
        return findChild(node, 6, null);
    }

    private AccessibilityNodeInfo findChild(AccessibilityNodeInfo node, int mode, String album) {
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo found = mode == 0 ? findFavourite(child)
                    : mode == 1 ? findAdd(child)
                    : mode == 2 ? findAlbum(child, album)
                    : mode == 3 ? findAlbumPickerOption(child)
                    : mode == 4 ? findAlreadyFavourite(child)
                    : mode == 5 ? findAlbumSearch(child)
                    : mode == 6 ? findEditableSearch(child) : findMoreOptions(child);
            if (found != null) return found;
        }
        return null;
    }

    private boolean click(AccessibilityNodeInfo node) {
        while (node != null && !node.isClickable()) node = node.getParent();
        return node != null && node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    @Override public void onInterrupt() {
        if (albumRetry != null) albumRetry.cancel();
    }

    @Override public void onDestroy() {
        if (albumRetry != null) albumRetry.cancel();
        super.onDestroy();
    }
}
