package com.keepers.photoorganiser;

public record AlbumAutomationProgress(
        String title, String detail, int position, int total) {
    static AlbumAutomationProgress from(int completed, int total, String albumName, int phase) {
        int safeTotal = Math.max(1, total);
        int position = Math.min(safeTotal, Math.max(1, completed + 1));
        return new AlbumAutomationProgress("Keepers · " + position + " of " + safeTotal,
                phaseLabel(phase) + " · " + albumName, position, safeTotal);
    }

    private static String phaseLabel(int phase) {
        return switch (phase) {
            case KeepersAccessibilityService.PHASE_ALBUM_PICKER -> "Opening album picker";
            case KeepersAccessibilityService.PHASE_FIND_OR_SEARCH -> "Finding album";
            case KeepersAccessibilityService.PHASE_TYPE_SEARCH -> "Entering album name";
            case KeepersAccessibilityService.PHASE_SELECT_RESULT -> "Choosing album";
            case KeepersAccessibilityService.PHASE_CONFIRM_ALBUM -> "Confirming addition";
            default -> "Opening Add to";
        };
    }
}
