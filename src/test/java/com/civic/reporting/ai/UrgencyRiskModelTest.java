package com.civic.reporting.ai;

import com.civic.reporting.enums.IssueCategory;
import com.civic.reporting.enums.IssuePriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UrgencyRiskModelTest {

    private UrgencyRiskModel model;

    @BeforeEach
    public void setup() {
        model = new UrgencyRiskModel();
    }

    @Test
    public void testCriticalElectricalEmergency() {
        UrgencyRiskModel.UrgencyEvaluation eval = model.evaluate(
                IssueCategory.ELECTRICITY,
                "Live wire sparking near school",
                "Dangerous high voltage line hanging low, risk of fatal electric shock to children."
        );

        assertEquals(IssuePriority.CRITICAL, eval.getPriority());
        assertTrue(eval.getUrgencyScore() >= 80);
        assertEquals(4, eval.getSlaHours());
        assertFalse(eval.getTriggers().isEmpty());
    }

    @Test
    public void testCriticalOpenManhole() {
        UrgencyRiskModel.UrgencyEvaluation eval = model.evaluate(
                IssueCategory.DRAINAGE,
                "Open manhole on busy highway",
                "Deep open manhole without cover causing severe accident hazard."
        );

        assertTrue(eval.getPriority() == IssuePriority.CRITICAL || eval.getPriority() == IssuePriority.HIGH);
        assertTrue(eval.getUrgencyScore() >= 70);
    }

    @Test
    public void testRoutineGarbageCollection() {
        UrgencyRiskModel.UrgencyEvaluation eval = model.evaluate(
                IssueCategory.GARBAGE_SANITATION,
                "Garbage bin needs clearance",
                "Regular residential waste bin is full."
        );

        assertEquals(IssuePriority.LOW, eval.getPriority());
        assertTrue(eval.getUrgencyScore() < 40);
    }
}
