package com.civic.reporting.ai;

import com.civic.reporting.enums.IssueCategory;
import com.civic.reporting.enums.IssuePriority;
import com.civic.reporting.repository.AIClassificationRepository;
import com.civic.reporting.repository.DepartmentRepository;
import com.civic.reporting.repository.IssueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

public class CivicAiEngineTest {

    private CivicAiEngine aiEngine;

    @BeforeEach
    public void setup() {
        NlpTextClassifier classifier = new NlpTextClassifier();
        UrgencyRiskModel urgencyModel = new UrgencyRiskModel();
        IssueRepository issueRepository = Mockito.mock(IssueRepository.class);
        DuplicateDetectionEngine duplicateDetector = new DuplicateDetectionEngine(issueRepository);
        ResolutionPredictor resolutionPredictor = new ResolutionPredictor(issueRepository);
        ImageQualityValidator imageValidator = new ImageQualityValidator();
        AIClassificationRepository aiClassificationRepository = Mockito.mock(AIClassificationRepository.class);
        DepartmentRepository departmentRepository = Mockito.mock(DepartmentRepository.class);

        aiEngine = new CivicAiEngine(
                classifier,
                urgencyModel,
                duplicateDetector,
                resolutionPredictor,
                imageValidator,
                aiClassificationRepository,
                departmentRepository
        );
    }

    @Test
    public void testCompleteAiAnalysisPipeline() {
        AiAnalysisResult result = aiEngine.analyze(
                "Sparking exposed electric line",
                "High voltage wire hanging low over sidewalk, immediate shock danger.",
                37.7749,
                -122.4194,
                "100 Market St",
                IssueCategory.OTHER,
                null,
                null
        );

        assertNotNull(result);
        assertEquals(IssueCategory.ELECTRICITY, result.getPredictedCategory());
        assertTrue(result.getConfidenceScore() >= 0.70);
        assertEquals(IssuePriority.CRITICAL, result.getPriority());
        assertTrue(result.getUrgencyScore() >= 80);
        assertTrue(result.getEstimatedResolutionHours() > 0);
        assertNotNull(result.getRationale());
    }
}
