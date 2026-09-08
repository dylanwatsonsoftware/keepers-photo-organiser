package com.keepers.photoorganiser;

public record AlbumAssignment(String photoId, String personId, String personName,
        String albumName, boolean suggested) {}
