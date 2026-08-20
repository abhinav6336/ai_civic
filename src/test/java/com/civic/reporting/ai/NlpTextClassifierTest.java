package com.civic.reporting.ai;

import com.civic.reporting.enums.IssueCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NlpTextClassifierTest {

    private NlpTextClassifier classifier;

    @BeforeEach
    public void setup() {
        classifier = new NlpTextClassifier();
    }

    @Test
    public void testClassifyRoadPothole() {
        NlpTextClassifier.ClassificationOutput out = classifier.classify(
                "Big pothole on main avenue",
                "Deep crater and damaged asphalt causing vehicles to swerve dangerously."
        );

        assertEquals(IssueCategory.ROADS, out.getCategory());
        assertTrue(out.getConfidence() >= 0.70, "Confidence should be high for clear pothole text");
        assertTrue(out.getProbabilities().containsKey("ROADS"));
    }

    @Test
    public void testClassifyElectricityHazard() {
        NlpTextClassifier.ClassificationOutput out = classifier.classify(
                "Sparking transformer and dangling live wire",
                "Street light pole is dark and high voltage wire is sparking."
        );

        assertEquals(IssueCategory.ELECTRICITY, out.getCategory());
        assertTrue(out.getConfidence() >= 0.70);
    }

    @Test
    public void testClassifyGarbageSanitation() {
        NlpTextClassifier.ClassificationOutput out = classifier.classify(
                "Overflowing trash bin",
                "Rotten garbage dump accumulating on sidewalk with foul smell."
        );

        assertEquals(IssueCategory.GARBAGE_SANITATION, out.getCategory());
        assertTrue(out.getConfidence() >= 0.70);
    }

    @Test
    public void testClassifyWaterLeak() {
        NlpTextClassifier.ClassificationOutput out = classifier.classify(
                "Water pipeline leak",
                "Drinking water supply pipe burst and gushing onto road."
        );

        assertEquals(IssueCategory.WATER, out.getCategory());
        assertTrue(out.getConfidence() >= 0.70);
    }

    @Test
    public void testClassifyDrainageSewage() {
        NlpTextClassifier.ClassificationOutput out = classifier.classify(
                "Open manhole and overflowing sewer",
                "Blocked stormwater drain causing waterlogging and sewage overflow."
        );

        assertEquals(IssueCategory.DRAINAGE, out.getCategory());
        assertTrue(out.getConfidence() >= 0.70);
    }

    @Test
    public void testEmptyOrGenericText() {
        NlpTextClassifier.ClassificationOutput out = classifier.classify("", "");
        assertEquals(IssueCategory.OTHER, out.getCategory());
    }
}
