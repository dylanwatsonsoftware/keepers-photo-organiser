package com.keepers.photoorganiser;

public enum PhotoContextType {
    GENERAL("General photo"),
    PORTRAIT("Portrait"),
    GROUP("Group photo"),
    PET("Pet"),
    LANDSCAPE("Landscape"),
    FOOD("Food"),
    ACTION("Action"),
    DOCUMENT("Document"),
    LOW_LIGHT("Low light");

    private final String displayName;

    PhotoContextType(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
}
