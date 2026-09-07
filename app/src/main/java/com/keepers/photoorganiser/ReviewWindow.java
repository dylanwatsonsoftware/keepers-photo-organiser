package com.keepers.photoorganiser;

public final class ReviewWindow {
    public static final int PAGE_SIZE = 60;
    private int limit = PAGE_SIZE;

    public int limit() { return limit; }
    public void expand() { limit += PAGE_SIZE; }
}
