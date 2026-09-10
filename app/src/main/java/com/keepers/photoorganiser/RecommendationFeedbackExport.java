package com.keepers.photoorganiser;

import java.util.List;

public final class RecommendationFeedbackExport {
    private RecommendationFeedbackExport() {}

    public static String toJson(List<RecommendationFeedback> feedback, String appVersion) {
        StringBuilder json = new StringBuilder("{\"schemaVersion\":1,\"appVersion\":\"")
                .append(escape(appVersion)).append("\",\"feedback\":[");
        for (int index = 0; index < feedback.size(); index++) {
            if (index > 0) json.append(',');
            RecommendationFeedback item = feedback.get(index);
            PhotoFeatures f = item.features();
            json.append("{\"rating\":\"")
                    .append(item.rating() == RecommendationFeedback.LOVED ? "loved" : "not_for_me")
                    .append("\",\"comment\":\"").append(escape(item.comment()))
                    .append("\",\"signals\":{")
                    .append("\"detail\":").append(f.quality()).append(',')
                    .append("\"focus\":").append(f.focus()).append(',')
                    .append("\"exposure\":").append(f.exposure()).append(',')
                    .append("\"composition\":").append(f.composition()).append(',')
                    .append("\"motionStability\":").append(f.motionStability()).append(',')
                    .append("\"faceCount\":").append(f.faceCount()).append(',')
                    .append("\"smile\":").append(f.smile()).append(',')
                    .append("\"eyesOpen\":").append(f.eyesOpen()).append("}}");
        }
        return json.append("]}").toString();
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
