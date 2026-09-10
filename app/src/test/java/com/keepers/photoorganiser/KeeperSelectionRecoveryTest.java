package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class KeeperSelectionRecoveryTest {
    @Test public void restoresLovedAndReviewedPhotosButNotExplicitRejections() {
        Context context = RuntimeEnvironment.getApplication();
        String loved = "content://media/photo/recover-loved";
        String reviewed = "content://media/photo/recover-reviewed";
        String rejected = "content://media/photo/recover-rejected";
        RecommendationFeedbackStore feedback = new RecommendationFeedbackStore(context);
        feedback.save(RecommendationFeedback.from(features(loved), RecommendationFeedback.LOVED, ""));
        feedback.save(RecommendationFeedback.from(features(rejected),
                RecommendationFeedback.NOT_FOR_ME, ""));
        ReviewedPhotoStore reviewedPhotos = new ReviewedPhotoStore(context);
        reviewedPhotos.mark(reviewed);
        reviewedPhotos.mark(rejected);

        KeeperSelectionRecovery.runOnce(context);

        assertEquals(Set.of(loved, reviewed), new KeeperSelectionStore(context).load());
    }

    @Test public void leavesAnExistingKeeperSelectionUntouched() {
        Context context = RuntimeEnvironment.getApplication();
        String existing = "content://media/photo/existing-keeper";
        String historical = "content://media/photo/historical-loved";
        new KeeperSelectionStore(context).replace(Set.of(existing));
        new RecommendationFeedbackStore(context).save(RecommendationFeedback.from(
                features(historical), RecommendationFeedback.LOVED, ""));

        KeeperSelectionRecovery.runOnce(context);

        assertEquals(Set.of(existing), new KeeperSelectionStore(context).load());
    }

    private static PhotoFeatures features(String id) {
        return new PhotoFeatures(id, 0, 0, .5, .5, .5, .5, .5, 0, -1, -1);
    }
}
