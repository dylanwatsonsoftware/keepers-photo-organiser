package com.keepers.photoorganiser;

public record VideoFeatures(int schemaVersion, String id, long durationMillis, int sampledFrames,
        double detail, double focus, double exposure, double composition, double frameStability,
        double blackFrameRate, double frozenFrameRate) {
    public static final int SCHEMA_VERSION = 1;

    public int score() {
        double core = (detail + focus + exposure + composition + frameStability) / 5d;
        double penalized = core - blackFrameRate * .35 - frozenFrameRate * .08;
        return (int) Math.round(Math.max(0, Math.min(1, penalized)) * 100);
    }
}
