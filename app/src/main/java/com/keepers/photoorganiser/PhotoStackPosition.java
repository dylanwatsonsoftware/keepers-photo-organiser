package com.keepers.photoorganiser;

public record PhotoStackPosition(int stackNumber, int position, int size) {
    public String label() {
        return "Stack " + stackNumber + " · " + position + "/" + size;
    }
}
