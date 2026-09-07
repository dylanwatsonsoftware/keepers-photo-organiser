package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AssessmentRuleTest {
    @Test public void strongSignalsAreStarredAndOtherStatesRemainDistinct() {
        assertEquals("★ Focus — 82%\nStrong", AssessmentRule.scored(
                "Focus", 0.82, "Strong").display());
        assertEquals("✓ Exposure — 54%\nMeasured", AssessmentRule.scored(
                "Exposure", 0.54, "Measured").display());
        assertEquals("— Smiles — Not assessed yet\nPending", AssessmentRule.pending(
                "Smiles", "Pending").display());
    }
}
