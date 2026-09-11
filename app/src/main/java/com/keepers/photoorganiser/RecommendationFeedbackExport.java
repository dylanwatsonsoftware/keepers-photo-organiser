package com.keepers.photoorganiser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RecommendationFeedbackExport {
    private RecommendationFeedbackExport() {}

    public static String toJson(List<RecommendationFeedback> feedback, String appVersion) {
        return toJson(feedback, List.of(), List.of(), appVersion);
    }

    public static String toJson(List<RecommendationFeedback> feedback,
            List<StackPreferenceComparison> comparisons, String appVersion) {
        return toJson(feedback, comparisons, List.of(), appVersion);
    }

    public static String toJson(List<RecommendationFeedback> feedback,
            List<StackPreferenceComparison> comparisons, List<PhotoFeatures> hiddenPhotos,
            String appVersion) {
        Set<String> hiddenIds = new HashSet<>();
        for (PhotoFeatures hidden : hiddenPhotos) hiddenIds.add(hidden.id());
        StringBuilder json = new StringBuilder("{\"schemaVersion\":3,\"appVersion\":\"")
                .append(escape(appVersion)).append("\",\"feedback\":[");
        boolean hasPrevious = false;
        for (RecommendationFeedback item : feedback) {
            if (hiddenIds.contains(item.features().id())) continue;
            if (hasPrevious) json.append(',');
            hasPrevious = true;
            PhotoFeatures f = item.features();
            json.append("{\"rating\":\"")
                    .append(item.rating() == RecommendationFeedback.LOVED ? "loved" : "not_for_me")
                    .append("\",\"comment\":\"").append(escape(item.comment()))
                    .append("\",\"signals\":");
            appendSignals(json, f).append('}');
        }
        json.append("],\"comparisons\":[");
        hasPrevious = false;
        for (StackPreferenceComparison comparison : comparisons) {
            if (hiddenIds.contains(comparison.preferred().id())
                    || hiddenIds.contains(comparison.alternative().id())) continue;
            if (hasPrevious) json.append(',');
            hasPrevious = true;
            json.append("{\"preferredSignals\":");
            appendSignals(json, comparison.preferred()).append(",\"alternativeSignals\":");
            appendSignals(json, comparison.alternative()).append('}');
        }
        json.append("],\"hiddenSignals\":[");
        hasPrevious = false;
        Set<String> exportedHiddenIds = new HashSet<>();
        for (PhotoFeatures hidden : hiddenPhotos) {
            if (!exportedHiddenIds.add(hidden.id())) continue;
            if (hasPrevious) json.append(',');
            hasPrevious = true;
            appendSignals(json, hidden);
        }
        return json.append("]}").toString();
    }

    private static StringBuilder appendSignals(StringBuilder json, PhotoFeatures f) {
        return json.append('{')
                .append("\"detail\":").append(f.quality()).append(',')
                .append("\"focus\":").append(f.focus()).append(',')
                .append("\"exposure\":").append(f.exposure()).append(',')
                .append("\"composition\":").append(f.composition()).append(',')
                .append("\"motionStability\":").append(f.motionStability()).append(',')
                .append("\"faceCount\":").append(f.faceCount()).append(',')
                .append("\"smile\":").append(f.smile()).append(',')
                .append("\"eyesOpen\":").append(f.eyesOpen()).append('}');
    }

    private static String escape(String value) {
        if (value == null) return "";
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) escaped.append(String.format("\\u%04x", (int) character));
                    else escaped.append(character);
                }
            }
        }
        return escaped.toString();
    }
}
