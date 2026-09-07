package com.keepers.photoorganiser;

public record PhotoActionPlan(Kind kind, boolean mayCreateDuplicate) {
    public enum Kind {
        OPEN_EXISTING,
        SHARE_EXPERIMENT
    }
}
