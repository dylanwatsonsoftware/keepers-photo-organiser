package com.keepers.photoorganiser;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

final class CarouselPagePair {
    private FrameLayout currentSurface;
    private ImageView currentImage;
    private FrameLayout adjacentSurface;
    private ImageView adjacentImage;

    CarouselPagePair(FrameLayout currentSurface, ImageView currentImage,
            FrameLayout adjacentSurface, ImageView adjacentImage) {
        this.currentSurface = currentSurface;
        this.currentImage = currentImage;
        this.adjacentSurface = adjacentSurface;
        this.adjacentImage = adjacentImage;
    }

    void promoteAdjacent() {
        FrameLayout previousSurface = currentSurface;
        ImageView previousImage = currentImage;
        currentSurface = adjacentSurface;
        currentImage = adjacentImage;
        adjacentSurface = previousSurface;
        adjacentImage = previousImage;

        currentSurface.setTranslationX(0);
        currentSurface.setTranslationY(0);
        currentSurface.setAlpha(1);
        currentSurface.setElevation(1);
        currentSurface.setVisibility(View.VISIBLE);
        adjacentSurface.setTranslationX(0);
        adjacentSurface.setTranslationY(0);
        adjacentSurface.setAlpha(1);
        adjacentSurface.setElevation(0);
        adjacentSurface.setVisibility(View.INVISIBLE);
    }

    FrameLayout currentSurface() { return currentSurface; }
    ImageView currentImage() { return currentImage; }
    FrameLayout adjacentSurface() { return adjacentSurface; }
    ImageView adjacentImage() { return adjacentImage; }
}
