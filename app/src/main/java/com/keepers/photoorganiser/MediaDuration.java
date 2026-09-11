package com.keepers.photoorganiser;

import java.util.Locale;

public final class MediaDuration {
    private MediaDuration() {}

    public static String format(long durationMillis) {
        long seconds = Math.max(0, durationMillis / 1000);
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remaining = seconds % 60;
        return hours > 0
                ? String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, remaining)
                : String.format(Locale.ROOT, "%d:%02d", minutes, remaining);
    }
}
