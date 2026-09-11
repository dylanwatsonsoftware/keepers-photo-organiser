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
                "Looking at camera", "Every child looks good", "Action or emotional significance",
                "Composition", "Exposure quality", "Motion blur", "Variety among selected moments",
                "Your previous Keeper choices"),
                assessment.rules().stream().map(AssessmentRule::name).toList());
        assertTrue(assessment.explanation().contains("— Smiles and expressions — Not assessed yet"));
        assertTrue(assessment.explanation().contains("★ Focus — 80%"));
    }

    @Test public void detectedExpressionsReplacePendingSmileAndEyeRules() {
        PhotoFeatures features = new PhotoFeatures("photo", 1, 2, 0.7,
                0.8, 0.75, 0.6, 0.9, 3, 0.84, 0.92);

        String explanation = PhotoAssessment.from(features, null, false).explanation();

        assertTrue(explanation.contains("★ Smiles and expressions — 84%"));
        assertTrue(explanation.contains("★ Closed eyes — 92%"));
    }

    @Test public void smileIsDescriptiveRatherThanUniversalQuality() {
        PhotoFeatures smiling = new PhotoFeatures("smiling", 1, 2, .7,
                .8, .75, .6, .9, 1, 1, .9, .8);
        PhotoFeatures candid = new PhotoFeatures("candid", 1, 2, .7,
                .8, .75, .6, .9, 1, 0, .9, .8);

        assertEquals(PhotoAssessment.from(candid, null, false).score(),
                PhotoAssessment.from(smiling, null, false).score());
    }

    @Test public void explainsWhetherEveryDetectedPersonFacesTheCamera() {
        PhotoFeatures portrait = new PhotoFeatures("portrait", 1, 2, .7,
                .8, .75, .6, .9, 2, .4, .95, .82);

        String explanation = PhotoAssessment.from(portrait, null, false).explanation();

        assertTrue(explanation.contains("★ Looking at camera — 82%"));
        assertTrue(explanation.contains("Least forward-facing person across 2 detected faces"));
    }
}
