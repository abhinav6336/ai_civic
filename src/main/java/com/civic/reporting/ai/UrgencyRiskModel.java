package com.civic.reporting.ai;

import com.civic.reporting.enums.IssueCategory;
import com.civic.reporting.enums.IssuePriority;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Machine learning heuristic & rule-weighted model for assessing
 * public safety hazards, urgency priority scores (1-100), and municipal dispatch SLA.
 */
@Component
public class UrgencyRiskModel {

    // Critical life-safety triggers
    private static final Map<String, Integer> CRITICAL_HAZARD_KEYWORDS = Map.ofEntries(
            Map.entry("live wire", 35),
            Map.entry("exposed wire", 30),
            Map.entry("sparking", 30),
            Map.entry("short circuit", 28),
            Map.entry("electric shock", 35),
            Map.entry("open manhole", 35),
            Map.entry("deep sinkhole", 30),
            Map.entry("cave in", 30),
            Map.entry("pipe burst", 25),
            Map.entry("gushing water", 25),
            Map.entry("gas leak", 40),
            Map.entry("flooding houses", 30),
            Map.entry("accident", 25),
            Map.entry("fatal", 35),
            Map.entry("deadly", 30),
            Map.entry("fire hazard", 35),
            Map.entry("transformer blast", 40),
            Map.entry("collapse", 30)
    );

    // High urgency and vulnerable location indicators
    private static final Map<String, Integer> URGENCY_MODIFIERS = Map.ofEntries(
            Map.entry("emergency", 20),
            Map.entry("urgent", 15),
            Map.entry("immediately", 15),
            Map.entry("immediate", 15),
            Map.entry("dangerous", 15),
            Map.entry("severe", 12),
            Map.entry("critical", 15),
            Map.entry("school", 15),
            Map.entry("hospital", 18),
            Map.entry("highway", 12),
            Map.entry("busy road", 12),
            Map.entry("main road", 10),
            Map.entry("elderly", 10),
            Map.entry("children", 12),
            Map.entry("pedestrian crossing", 10),
            Map.entry("foul smell", 8),
            Map.entry("health hazard", 15),
            Map.entry("contaminated", 18),
            Map.entry("swerving", 12),
            Map.entry("risk", 8)
    );

    public UrgencyEvaluation evaluate(IssueCategory category, String title, String description) {
        String combined = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase();

        // 1. Establish Category Base Urgency
        int score = switch (category != null ? category : IssueCategory.OTHER) {
            case ELECTRICITY -> 50;
            case DRAINAGE -> 45;
            case WATER -> 42;
            case ROADS -> 38;
            case GARBAGE_SANITATION -> 32;
            default -> 30;
        };

        List<String> detectedTriggers = new ArrayList<>();

        // 2. Scan Critical Life-Safety Triggers
        for (Map.Entry<String, Integer> entry : CRITICAL_HAZARD_KEYWORDS.entrySet()) {
            if (combined.contains(entry.getKey())) {
                score += entry.getValue();
                detectedTriggers.add(entry.getKey());
            }
        }

        // 3. Scan Urgency & Vulnerable Location Modifiers
        for (Map.Entry<String, Integer> entry : URGENCY_MODIFIERS.entrySet()) {
            if (combined.contains(entry.getKey())) {
                score += entry.getValue();
                detectedTriggers.add(entry.getKey());
            }
        }

        // 4. Bound score between 1 and 100
        score = Math.min(100, Math.max(1, score));

        // 5. Derive Priority Category
        IssuePriority priority;
        if (score >= 80) {
            priority = IssuePriority.CRITICAL;
        } else if (score >= 60) {
            priority = IssuePriority.HIGH;
        } else if (score >= 38) {
            priority = IssuePriority.MEDIUM;
        } else {
            priority = IssuePriority.LOW;
        }

        // 6. Formulate Diagnostic Rationale
        String rationale;
        if (!detectedTriggers.isEmpty()) {
            rationale = "Assigned " + priority.getLabel() + " priority (Score " + score + "/100) due to detected safety factors: ["
                    + String.join(", ", detectedTriggers.stream().limit(4).toList()) + "].";
        } else {
            rationale = "Assigned " + priority.getLabel() + " priority (Score " + score + "/100) based on baseline "
                    + (category != null ? category.getDisplayName() : "general") + " municipal SLA.";
        }

        return new UrgencyEvaluation(priority, score, priority.getDefaultSlaHours(), rationale, detectedTriggers);
    }

    public static class UrgencyEvaluation {
        private final IssuePriority priority;
        private final int urgencyScore;
        private final int slaHours;
        private final String rationale;
        private final List<String> triggers;

        public UrgencyEvaluation(IssuePriority priority, int urgencyScore, int slaHours, String rationale, List<String> triggers) {
            this.priority = priority;
            this.urgencyScore = urgencyScore;
            this.slaHours = slaHours;
            this.rationale = rationale;
            this.triggers = triggers;
        }

        public IssuePriority getPriority() {
            return priority;
        }

        public int getUrgencyScore() {
            return urgencyScore;
        }

        public int getSlaHours() {
            return slaHours;
        }

        public String getRationale() {
            return rationale;
        }

        public List<String> getTriggers() {
            return triggers;
        }
    }
}
