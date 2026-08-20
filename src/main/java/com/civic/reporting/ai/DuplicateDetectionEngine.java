package com.civic.reporting.ai;

import com.civic.reporting.entity.Issue;
import com.civic.reporting.enums.IssueCategory;
import com.civic.reporting.enums.IssueStatus;
import com.civic.reporting.repository.IssueRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Hybrid Geospatial + Semantic Text Similarity Engine for duplicate complaint detection.
 * Combines Haversine radius filtering (<= 250m) with Jaccard/Cosine token similarity.
 */
@Component
public class DuplicateDetectionEngine {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");
    private static final double MAX_PROXIMITY_METERS = 250.0;
    private static final double DUPLICATE_SIMILARITY_THRESHOLD = 0.55;

    private final IssueRepository issueRepository;

    public DuplicateDetectionEngine(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    public DuplicateCheckResult findDuplicate(IssueCategory category, String title, String description, Double lat, Double lon, String address) {
        List<Issue> activeIssues = issueRepository.findAll().stream()
                .filter(i -> i.getStatus() != IssueStatus.RESOLVED && i.getStatus() != IssueStatus.REJECTED)
                .toList();

        String incomingText = cleanText((title != null ? title : "") + " " + (description != null ? description : ""));
        Set<String> incomingTokens = tokenize(incomingText);

        Issue bestMatch = null;
        double highestSimilarity = 0.0;

        for (Issue existing : activeIssues) {
            double geoScore = 0.0;
            boolean geoAvailable = false;

            // 1. Check Geospatial Distance
            if (lat != null && lon != null && existing.getLatitude() != null && existing.getLongitude() != null) {
                geoAvailable = true;
                double distMeters = haversineMeters(lat, lon, existing.getLatitude(), existing.getLongitude());
                if (distMeters <= MAX_PROXIMITY_METERS) {
                    geoScore = Math.max(0.0, 1.0 - (distMeters / MAX_PROXIMITY_METERS));
                } else {
                    // Outside geographic radius -> not a physical duplicate
                    continue;
                }
            } else if (address != null && existing.getAddress() != null && !address.trim().isEmpty()) {
                // Fallback: Address string matching
                String addr1 = cleanText(address);
                String addr2 = cleanText(existing.getAddress());
                if (addr1.contains(addr2) || addr2.contains(addr1)) {
                    geoScore = 0.80;
                    geoAvailable = true;
                }
            }

            // 2. Semantic Text Similarity
            String existingText = cleanText((existing.getTitle() != null ? existing.getTitle() : "") + " " + (existing.getDescription() != null ? existing.getDescription() : ""));
            Set<String> existingTokens = tokenize(existingText);

            double textSim = computeJaccardSimilarity(incomingTokens, existingTokens);

            // 3. Category Match Weight
            boolean catMatch = (category != null && category == existing.getCategory());
            double catBonus = catMatch ? 0.20 : 0.0;

            // 4. Combined Similarity Index
            double combinedSim;
            if (geoAvailable) {
                combinedSim = (geoScore * 0.45) + (textSim * 0.40) + (catBonus * 0.15);
            } else {
                combinedSim = (textSim * 0.70) + (catBonus * 0.30);
            }

            if (combinedSim > highestSimilarity && combinedSim >= DUPLICATE_SIMILARITY_THRESHOLD) {
                highestSimilarity = combinedSim;
                bestMatch = existing;
            }
        }

        if (bestMatch != null) {
            double roundedSim = Math.round(highestSimilarity * 100.0) / 100.0;
            return new DuplicateCheckResult(true, bestMatch.getTrackingNumber(), roundedSim, bestMatch.getTitle());
        }

        return new DuplicateCheckResult(false, null, 0.0, null);
    }

    private double computeJaccardSimilarity(Set<String> setA, Set<String> setB) {
        if (setA.isEmpty() || setB.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        return (double) intersection.size() / union.size();
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth radius in meters
        double latDist = Math.toRadians(lat2 - lat1);
        double lonDist = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDist / 2) * Math.sin(latDist / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDist / 2) * Math.sin(lonDist / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String cleanText(String input) {
        if (input == null) return "";
        String lower = input.toLowerCase();
        String replaced = NON_ALPHANUMERIC.matcher(lower).replaceAll(" ");
        return MULTIPLE_SPACES.matcher(replaced).replaceAll(" ").trim();
    }

    private Set<String> tokenize(String cleaned) {
        if (cleaned.isEmpty()) return Collections.emptySet();
        return Arrays.stream(cleaned.split(" "))
                .filter(s -> s.length() >= 3)
                .collect(Collectors.toSet());
    }

    public static class DuplicateCheckResult {
        private final boolean isDuplicate;
        private final String duplicateOfTrackingNumber;
        private final double similarityScore;
        private final String matchingTitle;

        public DuplicateCheckResult(boolean isDuplicate, String duplicateOfTrackingNumber, double similarityScore, String matchingTitle) {
            this.isDuplicate = isDuplicate;
            this.duplicateOfTrackingNumber = duplicateOfTrackingNumber;
            this.similarityScore = similarityScore;
            this.matchingTitle = matchingTitle;
        }

        public boolean isDuplicate() {
            return isDuplicate;
        }

        public String getDuplicateOfTrackingNumber() {
            return duplicateOfTrackingNumber;
        }

        public double getSimilarityScore() {
            return similarityScore;
        }

        public String getMatchingTitle() {
            return matchingTitle;
        }
    }
}
