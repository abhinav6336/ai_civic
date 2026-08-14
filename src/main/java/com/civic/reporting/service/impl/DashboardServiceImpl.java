package com.civic.reporting.service.impl;

import com.civic.reporting.dto.response.DashboardStatsResponse;
import com.civic.reporting.entity.Department;
import com.civic.reporting.enums.IssueCategory;
import com.civic.reporting.enums.IssueStatus;
import com.civic.reporting.repository.DepartmentRepository;
import com.civic.reporting.repository.IssueRepository;
import com.civic.reporting.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final IssueRepository issueRepository;
    private final DepartmentRepository departmentRepository;

    public DashboardServiceImpl(IssueRepository issueRepository, DepartmentRepository departmentRepository) {
        this.issueRepository = issueRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    public DashboardStatsResponse getDashboardStats() {
        DashboardStatsResponse stats = new DashboardStatsResponse();
        
        stats.setTotalIssues(issueRepository.count());
        stats.setReportedIssues(issueRepository.countByStatus(IssueStatus.REPORTED));
        stats.setAiClassifiedIssues(issueRepository.countByStatus(IssueStatus.AI_CLASSIFIED));
        stats.setAssignedIssues(issueRepository.countByStatus(IssueStatus.ASSIGNED));
        stats.setInProgressIssues(issueRepository.countByStatus(IssueStatus.IN_PROGRESS));
        stats.setResolvedIssues(issueRepository.countByStatus(IssueStatus.RESOLVED));
        stats.setRejectedIssues(issueRepository.countByStatus(IssueStatus.REJECTED));

        // Category distribution
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (IssueCategory cat : IssueCategory.values()) {
            byCategory.put(cat.name(), issueRepository.countByCategory(cat));
        }
        stats.setIssuesByCategory(byCategory);

        // Department distribution
        Map<String, Long> byDept = new LinkedHashMap<>();
        List<Department> departments = departmentRepository.findAll();
        for (Department dept : departments) {
            byDept.put(dept.getName(), issueRepository.countByAssignedDepartmentId(dept.getId()));
        }
        stats.setIssuesByDepartment(byDept);

        return stats;
    }
}
