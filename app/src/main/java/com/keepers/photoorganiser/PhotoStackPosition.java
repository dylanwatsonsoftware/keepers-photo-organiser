package com.keepers.photoorganiser;

public record PhotoStackPosition(int position, int size) {
    public String label() {
        return position + "/" + size;
    }
}
