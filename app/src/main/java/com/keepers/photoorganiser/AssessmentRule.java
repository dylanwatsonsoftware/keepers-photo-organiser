package com.keepers.photoorganiser;

public record AssessmentRule(String name, Double value, String note) {
    public static AssessmentRule scored(String name, double value, String note) {
        return new AssessmentRule(name, value, note);
    }

    public static AssessmentRule pending(String name, String note) {
        return new AssessmentRule(name, null, note);
    }

    public String display() {
        String indicator = value == null ? "—" : value >= 0.65 ? "★" : "✓";
        return indicator + " " + name + " — " + (value == null ? "Not assessed yet"
                : Math.round(value * 100) + "%") + "\n" + note;
    }
}
