package com.civic.reporting.service.impl;

import com.civic.reporting.ai.AiAnalysisResult;
import com.civic.reporting.ai.CivicAiEngine;
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
import com.civic.reporting.service.FileStorageService;
import com.civic.reporting.service.IssueService;
import com.civic.reporting.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final FileStorageService fileStorageService;
    private final CivicAiEngine aiEngine;

    public IssueServiceImpl(IssueRepository issueRepository,
                            IssueUpdateRepository issueUpdateRepository,
                            DepartmentRepository departmentRepository,
                            UserRepository userRepository,
                            UserService userService,
                            FileStorageService fileStorageService,
                            CivicAiEngine aiEngine) {
        this.issueRepository = issueRepository;
        this.issueUpdateRepository = issueUpdateRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.aiEngine = aiEngine;
    }

    @Override
    public IssueResponse createIssue(IssueCreateRequest request) {
        return createIssue(request, null);
    }

    @Override
    public IssueResponse createIssue(IssueCreateRequest request, MultipartFile imageFile) {
        User citizen = userService.getOrCreateCitizen(
                request.getCitizenId(),
                request.getCitizenName(),
                request.getCitizenEmail(),
                request.getCitizenPhone()
        );

        IssueCategory category = request.getCategory() != null ? request.getCategory() : IssueCategory.OTHER;

        String title = (request.getTitle() != null && !request.getTitle().trim().isEmpty())
                ? request.getTitle().trim()
                : generateDefaultTitle(category, request.getAddress());

        String description = (request.getDescription() != null && !request.getDescription().trim().isEmpty())
                ? request.getDescription().trim()
                : "Civic issue reported for " + category.getDisplayName() + (request.getAddress() != null ? " at " + request.getAddress() : ".");

        // 1. Execute AI Pipeline (NLP classification, hazard urgency, duplicate check, ETA prediction)
        AiAnalysisResult aiResult = aiEngine.analyze(
                title,
                description,
                request.getLatitude(),
                request.getLongitude(),
                request.getAddress(),
                category,
                null,
                imageFile
        );

        // Auto-assign category if citizen selected OTHER and AI is confident
        IssueCategory finalCategory = category;
        if ((category == IssueCategory.OTHER || category == null) && aiResult.getPredictedCategory() != null && aiResult.getPredictedCategory() != IssueCategory.OTHER) {
            finalCategory = aiResult.getPredictedCategory();
        }

        Issue issue = new Issue();
        issue.setTrackingNumber(generateTrackingNumber());
        issue.setTitle(title);
        issue.setDescription(description);
        issue.setCategory(finalCategory);
        issue.setStatus(IssueStatus.REPORTED);
        issue.setLatitude(request.getLatitude());
        issue.setLongitude(request.getLongitude());
        issue.setAddress(request.getAddress());
        issue.setCitizen(citizen);

        // Populate AI fields
        issue.setAiConfidence(aiResult.getConfidenceScore());
        issue.setAiSuggestedCategory(aiResult.getPredictedCategory() != null ? aiResult.getPredictedCategory().name() : null);
        issue.setPriority(aiResult.getPriority());
        issue.setUrgencyScore(aiResult.getUrgencyScore());
        issue.setEstimatedResolutionHours(aiResult.getEstimatedResolutionHours());
        issue.setIsDuplicate(aiResult.isDuplicate());
        issue.setDuplicateOfTrackingNumber(aiResult.getDuplicateOfTrackingNumber());

        // Handle image file storage
        if (imageFile != null && !imageFile.isEmpty()) {
            String storedImageUrl = fileStorageService.storeIssueImage(imageFile);
            issue.setImageUrl(storedImageUrl);
        } else if (request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty()) {
            // Only accept valid URL paths (do not accept huge Base64 data strings)
            String rawUrl = request.getImageUrl().trim();
            if (!rawUrl.startsWith("data:image")) {
                issue.setImageUrl(rawUrl.length() > 500 ? rawUrl.substring(0, 500) : rawUrl);
            }
        }

        // Pre-route to department if category is provided
        if (issue.getCategory() != null && issue.getCategory() != IssueCategory.OTHER) {
            String deptCode = mapCategoryToDepartmentCode(issue.getCategory());
            departmentRepository.findByCodeIgnoreCase(deptCode).ifPresent(issue::setAssignedDepartment);
        }

        Issue savedIssue = issueRepository.save(issue);

        // Save AI classification audit record
        aiEngine.recordClassification(savedIssue, aiResult);

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

        // Record AI triage update log
        IssueUpdate aiUpdate = new IssueUpdate(
                savedIssue,
                null,
                IssueStatus.REPORTED,
                IssueStatus.AI_CLASSIFIED,
                "AI_TRIAGE",
                aiResult.getRationale()
        );
        issueUpdateRepository.save(aiUpdate);

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
        int year = LocalDate.now().getYear();
        int randomNum = 10000 + (int)(Math.random() * 90000);
        return "CIV-" + year + "-" + randomNum;
    }

    private String generateDefaultTitle(IssueCategory category, String address) {
        String catName = switch (category) {
            case ROADS -> "Road Maintenance Issue";
            case ELECTRICITY -> "Streetlight / Electrical Problem";
            case GARBAGE_SANITATION -> "Garbage / Sanitation Concern";
            case WATER -> "Water Pipeline / Supply Issue";
            case DRAINAGE -> "Drainage / Sewage Overflow";
            default -> "Civic Infrastructure Concern";
        };
        if (address != null && !address.trim().isEmpty()) {
            return catName + " (" + address.trim() + ")";
        }
        return catName;
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
