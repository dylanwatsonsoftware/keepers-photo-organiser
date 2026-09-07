package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class PhotoActionPlannerTest {
    @Test
    public void onePhotoUsesSafeViewAction() {
        PhotoActionPlan plan = PhotoActionPlanner.forPhotos(
                Collections.singletonList("content://media/photo/1"));

        assertEquals(PhotoActionPlan.Kind.OPEN_EXISTING, plan.kind());
        assertFalse(plan.mayCreateDuplicate());
    }

    @Test
    public void severalPhotosUseExplicitDuplicateRiskExperiment() {
        PhotoActionPlan plan = PhotoActionPlanner.forPhotos(Arrays.asList(
                "content://media/photo/1", "content://media/photo/2"));

        assertEquals(PhotoActionPlan.Kind.SHARE_EXPERIMENT, plan.kind());
        assertTrue(plan.mayCreateDuplicate());
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptySelectionCannotProduceAction() {
        PhotoActionPlanner.forPhotos(Collections.emptyList());
    }
}
