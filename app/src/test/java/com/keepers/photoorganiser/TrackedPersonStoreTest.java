package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TrackedPersonStoreTest {
    @Test public void persistsChosenPeopleAndExactAlbumMappings() {
        TrackedPersonStore store = new TrackedPersonStore(RuntimeEnvironment.getApplication());
        List<TrackedPerson> people = List.of(
                new TrackedPerson("person-1", "Ada", "Ada Photos", true),
                new TrackedPerson("person-2", "Ben", "Ben Album", false));

        store.save(people);

        assertEquals(people, new TrackedPersonStore(
                RuntimeEnvironment.getApplication()).load());
    }
}
