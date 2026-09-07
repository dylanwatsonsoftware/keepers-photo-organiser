package com.keepers.photoorganiser;

public record CarouselTransform(float currentX, float adjacentX) {
    public static CarouselTransform from(float dragX, float width, float gutter) {
        float pageDistance = width + gutter;
        float adjacentX = dragX < 0 ? pageDistance + dragX : -pageDistance + dragX;
        return new CarouselTransform(dragX, adjacentX);
    }
}
