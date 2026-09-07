package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class PhotoAssessmentTest {
    @Test public void scoreUsesAvailableRulesAndNamesEveryRequestedRule() {
        PhotoFeatures features = new PhotoFeatures("photo", 1, 2, 0.7,
                0.8, 0.75, 0.6, 0.9);

        PhotoAssessment assessment = PhotoAssessment.from(features,
                new PhotoStackPosition(1, 4), true);

        assertEquals(75, assessment.score());
        assertEquals(List.of("Focus", "Useful detail", "Smiles and expressions", "Closed eyes",
                "Every child looks good", "Action or emotional significance", "Composition",
                "Exposure quality", "Motion blur", "Variety among selected moments",
                "Your previous Keeper choices"),
                assessment.rules().stream().map(AssessmentRule::name).toList());
        assertTrue(assessment.explanation().contains("— Smiles and expressions — Not assessed yet"));
        assertTrue(assessment.explanation().contains("★ Focus — 80%"));
    }
}
