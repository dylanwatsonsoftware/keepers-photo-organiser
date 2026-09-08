package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class FaceGroupAssignmentStoreTest {
    @Test public void savesCorrectionsBetweenFaceGroupsAndPeople() {
        FaceGroupAssignmentStore store = new FaceGroupAssignmentStore(
                RuntimeEnvironment.getApplication());

        store.save(Map.of("face-a", "person-1", "face-b", ""));

        assertEquals(Map.of("face-a", "person-1"), store.load());
    }
}
