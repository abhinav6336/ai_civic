package com.civic.reporting.ai;

import com.civic.reporting.entity.Issue;
import com.civic.reporting.enums.IssueCategory;
import com.civic.reporting.enums.IssueStatus;
import com.civic.reporting.repository.IssueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class DuplicateDetectionEngineTest {

    private IssueRepository issueRepository;
    private DuplicateDetectionEngine engine;

    @BeforeEach
    public void setup() {
        issueRepository = Mockito.mock(IssueRepository.class);
        engine = new DuplicateDetectionEngine(issueRepository);
    }

    @Test
    public void testDetectDuplicateWithinProximity() {
        Issue existing = new Issue();
        existing.setTrackingNumber("CIV-2026-00100");
        existing.setTitle("Deep pothole in front of gate");
        existing.setDescription("Large pothole on Market St causing traffic hazard.");
        existing.setCategory(IssueCategory.ROADS);
        existing.setStatus(IssueStatus.REPORTED);
        existing.setLatitude(37.7749);
        existing.setLongitude(-122.4194);
        existing.setAddress("450 Market St");

        when(issueRepository.findAll()).thenReturn(List.of(existing));

        // Incoming complaint 50 meters away with matching text
        DuplicateDetectionEngine.DuplicateCheckResult result = engine.findDuplicate(
                IssueCategory.ROADS,
                "Big pothole near 450 Market St",
                "Deep pothole causing vehicle damage and traffic hazard.",
                37.7751,
                -122.4192,
                "452 Market St"
        );

        assertTrue(result.isDuplicate());
        assertEquals("CIV-2026-00100", result.getDuplicateOfTrackingNumber());
        assertTrue(result.getSimilarityScore() >= 0.55);
    }

    @Test
    public void testIgnoreFarAwayIssue() {
        Issue existing = new Issue();
        existing.setTrackingNumber("CIV-2026-00100");
        existing.setTitle("Pothole on Market St");
        existing.setDescription("Pothole on road");
        existing.setCategory(IssueCategory.ROADS);
        existing.setStatus(IssueStatus.REPORTED);
        existing.setLatitude(37.7749);
        existing.setLongitude(-122.4194);

        when(issueRepository.findAll()).thenReturn(List.of(existing));

        // Incoming complaint 10 km away
        DuplicateDetectionEngine.DuplicateCheckResult result = engine.findDuplicate(
                IssueCategory.ROADS,
                "Pothole on road",
                "Pothole on road",
                37.8500,
                -122.3000,
                "Distant Street"
        );

        assertFalse(result.isDuplicate());
    }
}
