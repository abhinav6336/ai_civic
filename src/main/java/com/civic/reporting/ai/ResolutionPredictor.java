package com.civic.reporting.ai;

import com.civic.reporting.enums.IssueCategory;
import com.civic.reporting.enums.IssuePriority;
import com.civic.reporting.enums.IssueStatus;
import com.civic.reporting.repository.IssueRepository;
import org.springframework.stereotype.Component;

/**
 * Predictive model for estimating resolution turnaround hours (ETA)
 * based on category complexity, priority SLA urgency, and departmental queue backlog.
 */
@Component
public class ResolutionPredictor {

    private final IssueRepository issueRepository;

    public ResolutionPredictor(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    public int predictResolutionHours(IssueCategory category, IssuePriority priority, Long departmentId) {
        // 1. Base category resolution standard (hours)
        int baseHours = switch (category != null ? category : IssueCategory.OTHER) {
            case GARBAGE_SANITATION -> 14;
            case ELECTRICITY -> 20;
            case WATER -> 28;
            case DRAINAGE -> 34;
            case ROADS -> 42;
            default -> 48;
        };

        // 2. Priority Acceleration Multiplier
        double priorityMultiplier = switch (priority != null ? priority : IssuePriority.MEDIUM) {
            case CRITICAL -> 0.25; // Expedited 4-8 hour crew dispatch
            case HIGH -> 0.60;
            case MEDIUM -> 1.00;
            case LOW -> 1.40;
        };

        int adjustedHours = (int) Math.round(baseHours * priorityMultiplier);

        // 3. Department Backlog Workload Adjustment
        long pendingInDept = 0;
        if (departmentId != null) {
            pendingInDept = issueRepository.countByAssignedDepartmentIdAndStatusNot(departmentId, IssueStatus.RESOLVED);
        } else {
            pendingInDept = issueRepository.countByStatusNot(IssueStatus.RESOLVED);
        }

        // Add 2 hours per active pending issue in queue (max +36 hours)
        int backlogDelay = (int) Math.min(36, pendingInDept * 2);

        int totalEstimatedHours = Math.max(4, adjustedHours + backlogDelay);
        return totalEstimatedHours;
    }
}
