package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AlbumDetailActivityTest {
    @Test public void albumNameSavesAsItIsEdited() {
        Context context = RuntimeEnvironment.getApplication();
        new RegisteredAlbumStore(context).save(List.of(
                new RegisteredAlbum("album-1", "", "")));
        AlbumDetailActivity activity = Robolectric.buildActivity(AlbumDetailActivity.class,
                new Intent(context, AlbumDetailActivity.class)
                        .putExtra(AlbumDetailActivity.EXTRA_ALBUM_ID, "album-1")).setup().get();

        activity.<EditText>findViewById(R.id.album_detail_name).setText("Family adventures");

        assertEquals("Family adventures",
                new RegisteredAlbumStore(activity).load().get(0).albumName());
    }
}
