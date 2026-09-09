package com.keepers.photoorganiser;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record PhotoMetadata(long takenAtMillis, String caption, String location,
        int width, int height, String mimeType, long sizeBytes) {
    public String formattedDate(ZoneId zone, Locale locale) {
        if (takenAtMillis <= 0) return "";
        return DateTimeFormatter.ofPattern("EEE, MMM d, yyyy • h:mm a", locale)
                .withZone(zone).format(Instant.ofEpochMilli(takenAtMillis));
    }

    public String technicalSummary(Locale locale) {
        List<String> parts = new ArrayList<>();
        if (width > 0 && height > 0) parts.add(width + " × " + height);
        String type = displayType(mimeType);
        if (!type.isBlank()) parts.add(type);
        if (sizeBytes > 0) parts.add(String.format(locale, "%.1f MB", sizeBytes / 1_000_000d));
        return String.join("  •  ", parts);
    }

    private static String displayType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) return "";
        int slash = mimeType.indexOf('/');
        String subtype = slash >= 0 ? mimeType.substring(slash + 1) : mimeType;
        if (subtype.equalsIgnoreCase("jpeg")) return "JPEG";
        return subtype.toUpperCase(Locale.ROOT);
    }
}
