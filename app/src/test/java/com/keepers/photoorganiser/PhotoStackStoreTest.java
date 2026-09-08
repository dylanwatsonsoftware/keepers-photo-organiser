package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PhotoStackStoreTest {
    @Test public void preservesOrderedStackMembers() {
        PhotoStackStore store = new PhotoStackStore(RuntimeEnvironment.getApplication());
        store.save(Map.of("one", List.of("one", "two"), "two", List.of("one", "two")));

        assertEquals(List.of("one", "two"), store.load("two"));
    }
}
