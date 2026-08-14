package com.civic.reporting.service.impl;

import com.civic.reporting.dto.request.IssueAssignRequest;
import com.civic.reporting.dto.request.IssueCreateRequest;
import com.civic.reporting.dto.request.IssueStatusUpdateRequest;
import com.civic.reporting.dto.response.IssueResponse;
import com.civic.reporting.entity.Department;
import com.civic.reporting.entity.Issue;
import com.civic.reporting.entity.IssueUpdate;
import com.civic.reporting.entity.User;
import com.civic.reporting.enums.IssueCategory;
import com.civic.reporting.enums.IssueStatus;
import com.civic.reporting.enums.UserRole;
import com.civic.reporting.exception.BadRequestException;
import com.civic.reporting.exception.ResourceNotFoundException;
import com.civic.reporting.repository.DepartmentRepository;
import com.civic.reporting.repository.IssueRepository;
import com.civic.reporting.repository.IssueUpdateRepository;
import com.civic.reporting.repository.UserRepository;
import com.civic.reporting.service.IssueService;
import com.civic.reporting.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class IssueServiceImpl implements IssueService {

    private final IssueRepository issueRepository;
    private final IssueUpdateRepository issueUpdateRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public IssueServiceImpl(IssueRepository issueRepository,
                            IssueUpdateRepository issueUpdateRepository,
                            DepartmentRepository departmentRepository,
                            UserRepository userRepository,
                            UserService userService) {
        this.issueRepository = issueRepository;
        this.issueUpdateRepository = issueUpdateRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public IssueResponse createIssue(IssueCreateRequest request) {
        User citizen = userService.getOrCreateCitizen(
                request.getCitizenId(),
                request.getCitizenName(),
                request.getCitizenEmail(),
                request.getCitizenPhone()
        );

        Issue issue = new Issue();
        issue.setTrackingNumber(generateTrackingNumber());
        issue.setTitle(request.getTitle().trim());
        issue.setDescription(request.getDescription().trim());
        issue.setCategory(request.getCategory() != null ? request.getCategory() : IssueCategory.OTHER);
        issue.setStatus(IssueStatus.REPORTED);
        issue.setLatitude(request.getLatitude());
        issue.setLongitude(request.getLongitude());
        issue.setAddress(request.getAddress());
        issue.setImageUrl(request.getImageUrl());
        issue.setCitizen(citizen);

        // Pre-route to department if category is provided
        if (issue.getCategory() != null && issue.getCategory() != IssueCategory.OTHER) {
            String deptCode = mapCategoryToDepartmentCode(issue.getCategory());
            departmentRepository.findByCodeIgnoreCase(deptCode).ifPresent(issue::setAssignedDepartment);
        }

        Issue savedIssue = issueRepository.save(issue);

        // Record initial submission update log
        IssueUpdate initialUpdate = new IssueUpdate(
                savedIssue,
                citizen,
                null,
                IssueStatus.REPORTED,
                "INITIAL_REPORT",
                "Issue reported by citizen via web portal."
        );
        issueUpdateRepository.save(initialUpdate);

        return IssueResponse.fromEntity(savedIssue, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssueResponse> getAllIssues(IssueStatus status, IssueCategory category, Long departmentId, String searchTerm) {
        String cleanSearch = (searchTerm != null && !searchTerm.trim().isEmpty()) ? searchTerm.trim() : null;
        List<Issue> issues = issueRepository.findWithFilters(status, category, departmentId, cleanSearch);
        return issues.stream()
                .map(IssueResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public IssueResponse getIssueByTrackingNumber(String trackingNumber) {
        Issue issue = issueRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with tracking number: " + trackingNumber));
        return IssueResponse.fromEntity(issue, true);
    }

    @Override
    @Transactional(readOnly = true)
    public IssueResponse getIssueById(Long id) {
        Issue issue = getIssueEntity(id);
        return IssueResponse.fromEntity(issue, true);
    }

    @Override
    public IssueResponse updateIssueStatus(Long id, IssueStatusUpdateRequest request) {
        Issue issue = getIssueEntity(id);
        IssueStatus previousStatus = issue.getStatus();
        IssueStatus newStatus = request.getNewStatus();

        if (previousStatus == newStatus) {
            throw new BadRequestException("Issue is already in status: " + newStatus.name());
        }

        issue.setStatus(newStatus);
        Issue updatedIssue = issueRepository.save(issue);

        User officer = null;
        if (request.getUpdatedByUserId() != null) {
            officer = userRepository.findById(request.getUpdatedByUserId()).orElse(null);
        }

        String note = (request.getNotes() != null && !request.getNotes().isBlank())
                ? request.getNotes().trim()
                : "Status changed from " + previousStatus.getLabel() + " to " + newStatus.getLabel();

        IssueUpdate updateLog = new IssueUpdate(
                updatedIssue,
                officer,
                previousStatus,
                newStatus,
                "STATUS_CHANGE",
                note
        );
        issueUpdateRepository.save(updateLog);

        return IssueResponse.fromEntity(updatedIssue, true);
    }

    @Override
    public IssueResponse assignIssue(Long id, IssueAssignRequest request) {
        Issue issue = getIssueEntity(id);
        Department dept = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        issue.setAssignedDepartment(dept);

        User officer = null;
        if (request.getOfficerId() != null) {
            officer = userRepository.findById(request.getOfficerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Officer not found with ID: " + request.getOfficerId()));
            if (officer.getRole() != UserRole.OFFICER && officer.getRole() != UserRole.ADMIN) {
                throw new BadRequestException("Assigned user must have OFFICER or ADMIN role");
            }
            issue.setAssignedOfficer(officer);
        }

        IssueStatus previousStatus = issue.getStatus();
        if (issue.getStatus() == IssueStatus.REPORTED || issue.getStatus() == IssueStatus.AI_CLASSIFIED) {
            issue.setStatus(IssueStatus.ASSIGNED);
        }

        Issue saved = issueRepository.save(issue);

        User assignedByUser = null;
        if (request.getAssignedByUserId() != null) {
            assignedByUser = userRepository.findById(request.getAssignedByUserId()).orElse(null);
        }

        String notes = (request.getNotes() != null && !request.getNotes().isBlank())
                ? request.getNotes().trim()
                : "Assigned to " + dept.getName() + (officer != null ? " (Officer: " + officer.getName() + ")" : "");

        IssueUpdate updateLog = new IssueUpdate(
                saved,
                assignedByUser,
                previousStatus,
                saved.getStatus(),
                "DEPARTMENT_ASSIGNMENT",
                notes
        );
        issueUpdateRepository.save(updateLog);

        return IssueResponse.fromEntity(saved, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssueResponse> getIssuesByCitizen(Long citizenId) {
        return issueRepository.findByCitizenIdOrderByCreatedAtDesc(citizenId).stream()
                .map(IssueResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Issue getIssueEntity(Long id) {
        return issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with ID: " + id));
    }

    private String generateTrackingNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "CIVIC-" + datePart + "-" + randomPart;
    }

    private String mapCategoryToDepartmentCode(IssueCategory category) {
        return switch (category) {
            case ROADS -> "ROADS";
            case ELECTRICITY -> "ELECTRICITY";
            case GARBAGE_SANITATION -> "SANITATION";
            case WATER -> "WATER";
            case DRAINAGE -> "DRAINAGE";
            default -> "ROADS";
        };
    }
}
