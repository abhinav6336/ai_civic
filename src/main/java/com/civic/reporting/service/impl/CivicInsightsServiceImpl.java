package com.civic.reporting.service.impl;

import com.civic.reporting.dto.response.CivicInsightsResponse;
import com.civic.reporting.dto.response.CivicInsightsResponse.*;
import com.civic.reporting.entity.Department;
import com.civic.reporting.entity.Issue;
import com.civic.reporting.enums.IssueCategory;
import com.civic.reporting.enums.IssueStatus;
import com.civic.reporting.repository.DepartmentRepository;
import com.civic.reporting.repository.IssueRepository;
import com.civic.reporting.service.CivicInsightsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CivicInsightsServiceImpl implements CivicInsightsService {

    private final IssueRepository issueRepository;
    private final DepartmentRepository departmentRepository;

    public CivicInsightsServiceImpl(IssueRepository issueRepository, DepartmentRepository departmentRepository) {
        this.issueRepository = issueRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    public CivicInsightsResponse getCivicInsights() {
        List<Issue> allIssues = issueRepository.findAll();
        List<Department> departments = departmentRepository.findAll();

        ObservedData observed = computeObservedData(allIssues, departments);
        AlgorithmicInsights algorithmic = computeAlgorithmicInsights(allIssues, departments, observed);

        return new CivicInsightsResponse(observed, algorithmic);
    }

    // =========================================================================
    // 1. OBSERVED DATA COMPUTATION (Exact Historical Data)
    // =========================================================================
    private ObservedData computeObservedData(List<Issue> issues, List<Department> departments) {
        ObservedData data = new ObservedData();

        long total = issues.size();
        long resolved = issues.stream().filter(i -> i.getStatus() == IssueStatus.RESOLVED).count();
        long inProgress = issues.stream().filter(i -> i.getStatus() == IssueStatus.IN_PROGRESS).count();
        long pending = issues.stream().filter(i -> i.getStatus() != IssueStatus.RESOLVED && i.getStatus() != IssueStatus.REJECTED).count();

        data.setTotalComplaints(total);
        data.setResolvedComplaints(resolved);
        data.setInProgressComplaints(inProgress);
        data.setPendingComplaints(pending);
        data.setOverallResolutionRate(total > 0 ? Math.round((resolved * 100.0 / total) * 10.0) / 10.0 : 0.0);

        // Status counts
        Map<String, Long> statusMap = new LinkedHashMap<>();
        for (IssueStatus status : IssueStatus.values()) {
            long c = issues.stream().filter(i -> i.getStatus() == status).count();
            statusMap.put(status.name(), c);
        }
        data.setStatusCounts(statusMap);

        // Category breakdown
        List<CategoryMetric> catMetrics = new ArrayList<>();
        for (IssueCategory cat : IssueCategory.values()) {
            long count = issues.stream().filter(i -> i.getCategory() == cat).count();
            double pct = total > 0 ? Math.round((count * 100.0 / total) * 10.0) / 10.0 : 0.0;
            catMetrics.add(new CategoryMetric(cat.name(), cat.getDisplayName(), count, pct));
        }
        // Sort descending by count
        catMetrics.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));
        data.setCategoryBreakdown(catMetrics);

        // Department workloads
        List<DepartmentWorkloadMetric> deptMetrics = new ArrayList<>();
        for (Department dept : departments) {
            DepartmentWorkloadMetric dm = new DepartmentWorkloadMetric();
            dm.setDepartmentId(dept.getId());
            dm.setDepartmentName(dept.getName());
            dm.setCode(dept.getCode());

            List<Issue> deptIssues = issues.stream()
                    .filter(i -> i.getAssignedDepartment() != null && i.getAssignedDepartment().getId().equals(dept.getId()))
                    .toList();

            long deptTotal = deptIssues.size();
            long deptResolved = deptIssues.stream().filter(i -> i.getStatus() == IssueStatus.RESOLVED).count();
            long deptInProg = deptIssues.stream().filter(i -> i.getStatus() == IssueStatus.IN_PROGRESS).count();
            long deptPending = deptIssues.stream().filter(i -> i.getStatus() != IssueStatus.RESOLVED && i.getStatus() != IssueStatus.REJECTED).count();

            dm.setTotalCount(deptTotal);
            dm.setResolvedCount(deptResolved);
            dm.setInProgressCount(deptInProg);
            dm.setPendingCount(deptPending);
            dm.setWorkloadSharePercent(pending > 0 ? Math.round((deptPending * 100.0 / pending) * 10.0) / 10.0 : 0.0);
            dm.setResolutionRate(deptTotal > 0 ? Math.round((deptResolved * 100.0 / deptTotal) * 10.0) / 10.0 : 0.0);

            deptMetrics.add(dm);
        }
        deptMetrics.sort((a, b) -> Long.compare(b.getPendingCount(), a.getPendingCount()));
        data.setDepartmentWorkloads(deptMetrics);

        // Frequent Locations
        Map<String, List<Issue>> byLocation = issues.stream()
                .filter(i -> i.getAddress() != null && !i.getAddress().trim().isEmpty())
                .collect(Collectors.groupingBy(i -> normalizeLocation(i.getAddress())));

        List<LocationFrequencyMetric> locMetrics = new ArrayList<>();
        for (Map.Entry<String, List<Issue>> entry : byLocation.entrySet()) {
            List<Issue> locIssues = entry.getValue();
            long locTotal = locIssues.size();
            long locPending = locIssues.stream().filter(i -> i.getStatus() != IssueStatus.RESOLVED).count();
            long locResolved = locIssues.stream().filter(i -> i.getStatus() == IssueStatus.RESOLVED).count();

            // Find primary category for this location
            String primaryCat = locIssues.stream()
                    .collect(Collectors.groupingBy(i -> i.getCategory() != null ? i.getCategory().getDisplayName() : "Other", Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Civic");

            locMetrics.add(new LocationFrequencyMetric(
                    locIssues.get(0).getAddress(),
                    locTotal,
                    locPending,
                    locResolved,
                    primaryCat
            ));
        }
        locMetrics.sort((a, b) -> Long.compare(b.getTotalCount(), a.getTotalCount()));
        data.setFrequentLocations(locMetrics.stream().limit(10).collect(Collectors.toList()));

        return data;
    }

    // =========================================================================
    // 2. ALGORITHMIC INSIGHTS COMPUTATION (Clustering, Patterns, Bottlenecks)
    // =========================================================================
    private AlgorithmicInsights computeAlgorithmicInsights(List<Issue> issues, List<Department> departments, ObservedData observed) {
        AlgorithmicInsights insights = new AlgorithmicInsights();

        // A. Spatial Clustering (Distance-Based DBSCAN-style clustering)
        List<SpatialCluster> clusters = buildSpatialClusters(issues);
        insights.setSpatialClusters(clusters);

        // B. Recurring Issue Patterns
        List<RecurringIssuePattern> recurringPatterns = detectRecurringPatterns(issues);
        insights.setRecurringPatterns(recurringPatterns);

        // C. Workload Bottlenecks
        List<WorkloadBottleneck> bottlenecks = detectWorkloadBottlenecks(observed.getDepartmentWorkloads(), observed.getPendingComplaints());
        insights.setWorkloadBottlenecks(bottlenecks);

        // D. Unresolved Complaint Patterns
        List<UnresolvedComplaintPattern> unresolvedPatterns = detectUnresolvedPatterns(issues);
        insights.setUnresolvedPatterns(unresolvedPatterns);

        // E. Strategic Actionable Recommendations
        List<ActionableRecommendation> recommendations = generateRecommendations(clusters, bottlenecks, recurringPatterns, observed);
        insights.setRecommendations(recommendations);

        return insights;
    }

    /**
     * Builds geographic clusters of complaints using Haversine distance (< 1.2 km).
     */
    private List<SpatialCluster> buildSpatialClusters(List<Issue> issues) {
        List<Issue> geoIssues = issues.stream()
                .filter(i -> i.getLatitude() != null && i.getLongitude() != null)
                .toList();

        List<List<Issue>> groupedClusters = new ArrayList<>();
        boolean[] visited = new boolean[geoIssues.size()];

        for (int i = 0; i < geoIssues.size(); i++) {
            if (visited[i]) continue;
            List<Issue> currentCluster = new ArrayList<>();
            currentCluster.add(geoIssues.get(i));
            visited[i] = true;

            for (int j = i + 1; j < geoIssues.size(); j++) {
                if (visited[j]) continue;
                double distKm = haversineDistance(
                        geoIssues.get(i).getLatitude(), geoIssues.get(i).getLongitude(),
                        geoIssues.get(j).getLatitude(), geoIssues.get(j).getLongitude()
                );
                // Group points within 2.0 km into the same spatial cluster
                if (distKm <= 2.0) {
                    currentCluster.add(geoIssues.get(j));
                    visited[j] = true;
                }
            }
            groupedClusters.add(currentCluster);
        }

        List<SpatialCluster> result = new ArrayList<>();
        int clusterIdx = 1;
        for (List<Issue> clusterGroup : groupedClusters) {
            SpatialCluster sc = new SpatialCluster();
            sc.setClusterId("CLUSTER-" + clusterIdx);

            double avgLat = clusterGroup.stream().mapToDouble(Issue::getLatitude).average().orElse(0.0);
            double avgLon = clusterGroup.stream().mapToDouble(Issue::getLongitude).average().orElse(0.0);
            sc.setLatitude(Math.round(avgLat * 10000.0) / 10000.0);
            sc.setLongitude(Math.round(avgLon * 10000.0) / 10000.0);
            sc.setComplaintCount(clusterGroup.size());

            // Primary category
            String domCat = clusterGroup.stream()
                    .collect(Collectors.groupingBy(i -> i.getCategory() != null ? i.getCategory().getDisplayName() : "Other", Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Civic");
            sc.setDominantCategory(domCat);

            // Risk Level
            long unresolvedInCluster = clusterGroup.stream().filter(i -> i.getStatus() != IssueStatus.RESOLVED).count();
            if (unresolvedInCluster >= 2 || clusterGroup.size() >= 3) {
                sc.setRiskLevel("HIGH");
            } else if (clusterGroup.size() >= 2) {
                sc.setRiskLevel("MEDIUM");
            } else {
                sc.setRiskLevel("LOW");
            }

            // Cluster Name & Tracking Numbers
            String sampleAddress = clusterGroup.get(0).getAddress();
            if (sampleAddress != null && sampleAddress.contains(",")) {
                sc.setClusterName("Zone: " + sampleAddress.split(",")[0].trim());
            } else if (sampleAddress != null && !sampleAddress.isEmpty()) {
                sc.setClusterName("Zone: " + sampleAddress);
            } else {
                sc.setClusterName("Zone " + clusterIdx + " (" + domCat + ")");
            }

            sc.setTrackingNumbers(clusterGroup.stream().map(Issue::getTrackingNumber).collect(Collectors.toList()));
            sc.setSummary(clusterGroup.size() + " complaints recorded in this zone (" + unresolvedInCluster + " currently pending). Dominant issue: " + domCat + ".");

            result.add(sc);
            clusterIdx++;
        }

        result.sort((a, b) -> Integer.compare(b.getComplaintCount(), a.getComplaintCount()));
        return result;
    }

    /**
     * Detects locations with multiple recurring complaints of the same or related categories.
     */
    private List<RecurringIssuePattern> detectRecurringPatterns(List<Issue> issues) {
        Map<String, List<Issue>> byLocationAndCat = issues.stream()
                .filter(i -> i.getAddress() != null && !i.getAddress().trim().isEmpty())
                .collect(Collectors.groupingBy(i -> normalizeLocation(i.getAddress()) + "::" + (i.getCategory() != null ? i.getCategory().name() : "OTHER")));

        List<RecurringIssuePattern> patterns = new ArrayList<>();
        for (Map.Entry<String, List<Issue>> entry : byLocationAndCat.entrySet()) {
            List<Issue> group = entry.getValue();
            if (group.size() >= 2) {
                Issue sample = group.get(0);
                String catName = sample.getCategory() != null ? sample.getCategory().getDisplayName() : "General";

                RecurringIssuePattern pattern = new RecurringIssuePattern();
                pattern.setLocation(sample.getAddress());
                pattern.setCategory(catName);
                pattern.setOccurrences(group.size());
                pattern.setSeverity(group.size() >= 3 ? "HIGH" : "MEDIUM");
                pattern.setRecurrenceType("CHRONIC_HOTSPOT");
                pattern.setDiagnosis("Multiple (" + group.size() + ") " + catName + " reports clustered at this address indicate chronic structural or maintenance failure rather than an isolated incident.");
                pattern.setRecommendation("Conduct a comprehensive municipal site audit and preventive overhaul at " + sample.getAddress() + ".");

                patterns.add(pattern);
            }
        }

        // If no strict exact matches, check category-wide recurrence across streets
        if (patterns.isEmpty()) {
            Map<IssueCategory, List<Issue>> byCat = issues.stream()
                    .filter(i -> i.getCategory() != null)
                    .collect(Collectors.groupingBy(Issue::getCategory));

            for (Map.Entry<IssueCategory, List<Issue>> entry : byCat.entrySet()) {
                if (entry.getValue().size() >= 2) {
                    RecurringIssuePattern p = new RecurringIssuePattern();
                    p.setLocation("Citywide Corridor");
                    p.setCategory(entry.getKey().getDisplayName());
                    p.setOccurrences(entry.getValue().size());
                    p.setSeverity("MEDIUM");
                    p.setRecurrenceType("SYSTEMIC_CATEGORY_VOLUME");
                    p.setDiagnosis(entry.getValue().size() + " incidents reported for " + entry.getKey().getDisplayName() + ". Represents a primary municipal workload driver.");
                    p.setRecommendation("Allocate dedicated rapid response crews for " + entry.getKey().getDisplayName() + " maintenance.");
                    patterns.add(p);
                }
            }
        }

        return patterns;
    }

    /**
     * Detects departments that have disproportionate backlog loads.
     */
    private List<WorkloadBottleneck> detectWorkloadBottlenecks(List<DepartmentWorkloadMetric> deptWorkloads, long totalPending) {
        List<WorkloadBottleneck> bottlenecks = new ArrayList<>();

        for (DepartmentWorkloadMetric dw : deptWorkloads) {
            WorkloadBottleneck wb = new WorkloadBottleneck();
            wb.setDepartmentName(dw.getDepartmentName());
            wb.setCode(dw.getCode());
            wb.setPendingBacklog(dw.getPendingCount());

            double share = dw.getWorkloadSharePercent();
            if (share >= 40.0 && dw.getPendingCount() >= 2) {
                wb.setBottleneckSeverity("CRITICAL");
                wb.setWorkloadPressureIndex(Math.round((share / 20.0) * 10.0) / 10.0);
                wb.setRecommendation("Critical workload accumulation (" + share + "% of municipal pending issues). Immediate dispatch of reserve field crews required.");
            } else if (share >= 25.0 || dw.getPendingCount() >= 2) {
                wb.setBottleneckSeverity("ELEVATED");
                wb.setWorkloadPressureIndex(Math.round((share / 20.0) * 10.0) / 10.0);
                wb.setRecommendation("Elevated pending volume (" + dw.getPendingCount() + " issues). Prioritize unresolved complaints older than 48 hours.");
            } else {
                wb.setBottleneckSeverity("NORMAL");
                wb.setWorkloadPressureIndex(1.0);
                wb.setRecommendation("Workload within standard municipal operational capacity.");
            }
            bottlenecks.add(wb);
        }

        bottlenecks.sort((a, b) -> Long.compare(b.getPendingBacklog(), a.getPendingBacklog()));
        return bottlenecks;
    }

    /**
     * Identifies systemic patterns in unresolved complaints.
     */
    private List<UnresolvedComplaintPattern> detectUnresolvedPatterns(List<Issue> issues) {
        List<UnresolvedComplaintPattern> patterns = new ArrayList<>();

        // Unassigned or newly reported backlog
        long unassignedReported = issues.stream().filter(i -> i.getStatus() == IssueStatus.REPORTED).count();
        if (unassignedReported > 0) {
            patterns.add(new UnresolvedComplaintPattern(
                    "Pending Initial Dispatch",
                    "NEW_DISPATCH_BACKLOG",
                    (int) unassignedReported,
                    unassignedReported + " newly reported complaint(s) require departmental triage and officer assignment.",
                    "Assign these complaints to field officers to initiate inspection."
            ));
        }

        // In Progress duration
        long inProgressCount = issues.stream().filter(i -> i.getStatus() == IssueStatus.IN_PROGRESS).count();
        if (inProgressCount > 0) {
            patterns.add(new UnresolvedComplaintPattern(
                    "Active Field Operations",
                    "ACTIVE_REPAIRS",
                    (int) inProgressCount,
                    inProgressCount + " complaint(s) currently marked 'In Progress' with active repair teams on site.",
                    "Verify field completion logs and capture resolution photos."
            ));
        }

        // Multi-issue road/water coordination
        long roadAndWaterPending = issues.stream()
                .filter(i -> (i.getCategory() == IssueCategory.ROADS || i.getCategory() == IssueCategory.WATER || i.getCategory() == IssueCategory.DRAINAGE) && i.getStatus() != IssueStatus.RESOLVED)
                .count();

        if (roadAndWaterPending >= 2) {
            patterns.add(new UnresolvedComplaintPattern(
                    "Underground Utility & Pavement Overlap",
                    "UTILITY_PAVEMENT_OVERLAP",
                    (int) roadAndWaterPending,
                    "Multiple open complaints involve road surfacing or water/drainage infrastructure.",
                    "Coordinate water valve repairs before final asphalt sealing to prevent re-excavation."
            ));
        }

        return patterns;
    }

    /**
     * Generates prioritized strategic recommendations.
     */
    private List<ActionableRecommendation> generateRecommendations(
            List<SpatialCluster> clusters,
            List<WorkloadBottleneck> bottlenecks,
            List<RecurringIssuePattern> recurring,
            ObservedData observed) {

        List<ActionableRecommendation> recs = new ArrayList<>();

        // 1. Hotspot Cluster Action
        Optional<SpatialCluster> topCluster = clusters.stream()
                .filter(c -> "HIGH".equals(c.getRiskLevel()) || c.getComplaintCount() >= 2)
                .findFirst();

        topCluster.ifPresent(sc -> recs.add(new ActionableRecommendation(
                "REC-01",
                "Deploy Targeted Task Force to " + sc.getClusterName(),
                "SPATIAL_CLUSTER",
                "URGENT",
                sc.getComplaintCount() + " complaints detected in close geographic proximity with dominant issue '" + sc.getDominantCategory() + "'.",
                "Dispatch a joint inspection unit to " + sc.getClusterName() + " to resolve multiple proximate issues in a single municipal run."
        )));

        // 2. Department Bottleneck Action
        Optional<WorkloadBottleneck> topBottleneck = bottlenecks.stream()
                .filter(b -> "CRITICAL".equals(b.getBottleneckSeverity()) || "ELEVATED".equals(b.getBottleneckSeverity()))
                .findFirst();

        topBottleneck.ifPresent(wb -> recs.add(new ActionableRecommendation(
                "REC-02",
                "Re-allocate Field Crews to " + wb.getDepartmentName(),
                "WORKLOAD_BALANCE",
                "HIGH",
                wb.getDepartmentName() + " holds " + wb.getPendingBacklog() + " unresolved complaints (" + wb.getBottleneckSeverity() + " backlog level).",
                "Temporarily shift auxiliary repair personnel to " + wb.getDepartmentName() + " to clear pending tickets."
        )));

        // 3. Chronic Preventive Action
        if (!recurring.isEmpty()) {
            RecurringIssuePattern p = recurring.get(0);
            recs.add(new ActionableRecommendation(
                    "REC-03",
                    "Schedule Preventive Overhaul at " + p.getLocation(),
                    "PREVENTIVE_MAINTENANCE",
                    "MEDIUM",
                    "Location has experienced " + p.getOccurrences() + " recurring " + p.getCategory() + " complaints.",
                    "Perform root-cause structural remediation rather than emergency reactive repairs."
            ));
        }

        // 4. Overall Resolution Momentum
        if (observed.getOverallResolutionRate() < 50.0) {
            recs.add(new ActionableRecommendation(
                    "REC-04",
                    "Accelerate Pending Ticket Closures",
                    "RESOLUTION_RATE",
                    "MEDIUM",
                    "Current municipal resolution rate is " + observed.getOverallResolutionRate() + "% across " + observed.getTotalComplaints() + " total complaints.",
                    "Audit open tickets in 'ASSIGNED' state and request field status updates from assigned officers."
            ));
        }

        return recs;
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDist = Math.toRadians(lat2 - lat1);
        double lonDist = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDist / 2) * Math.sin(latDist / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDist / 2) * Math.sin(lonDist / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String normalizeLocation(String address) {
        if (address == null) return "UNKNOWN";
        return address.trim().toLowerCase().replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ");
    }
}
