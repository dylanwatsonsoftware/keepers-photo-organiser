package com.keepers.photoorganiser;

public record GestureCoordinates(float startRawX, float startRawY) {
    public float deltaX(float currentRawX) { return currentRawX - startRawX; }
    public float deltaY(float currentRawY) { return currentRawY - startRawY; }
}
