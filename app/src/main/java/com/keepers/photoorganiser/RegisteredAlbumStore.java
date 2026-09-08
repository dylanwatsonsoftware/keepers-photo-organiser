package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

public final class RegisteredAlbumStore {
    private final SharedPreferences preferences;

    public RegisteredAlbumStore(Context context) {
        preferences = context.getSharedPreferences("registered_albums", Context.MODE_PRIVATE);
    }

    public void save(List<RegisteredAlbum> albums) {
        SharedPreferences.Editor editor = preferences.edit().clear().putInt("count", albums.size());
        for (int index = 0; index < albums.size(); index++) {
            RegisteredAlbum album = albums.get(index);
            editor.putString("id_" + index, album.id());
            editor.putString("name_" + index, album.albumName());
            editor.putString("photo_" + index, album.featurePhotoId());
        }
        editor.apply();
    }

    public List<RegisteredAlbum> load() {
        int count = preferences.getInt("count", 0);
        ArrayList<RegisteredAlbum> albums = new ArrayList<>();
        for (int index = 0; index < count; index++) albums.add(new RegisteredAlbum(
                preferences.getString("id_" + index, "album-" + (index + 1)),
                preferences.getString("name_" + index, ""),
                preferences.getString("photo_" + index, "")));
        return List.copyOf(albums);
    }
}
