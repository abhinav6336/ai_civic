package com.civic.reporting.service;

import com.civic.reporting.dto.request.IssueAssignRequest;
import com.civic.reporting.dto.request.IssueCreateRequest;
import com.civic.reporting.dto.request.IssueStatusUpdateRequest;
import com.civic.reporting.dto.response.IssueResponse;
import com.civic.reporting.entity.Issue;
import com.civic.reporting.enums.IssueCategory;
import com.civic.reporting.enums.IssueStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IssueService {
    IssueResponse createIssue(IssueCreateRequest request);
    IssueResponse createIssue(IssueCreateRequest request, MultipartFile imageFile);
    List<IssueResponse> getAllIssues(IssueStatus status, IssueCategory category, Long departmentId, String searchTerm);
    IssueResponse getIssueByTrackingNumber(String trackingNumber);
    IssueResponse getIssueById(Long id);
    IssueResponse updateIssueStatus(Long id, IssueStatusUpdateRequest request);
    IssueResponse assignIssue(Long id, IssueAssignRequest request);
    List<IssueResponse> getIssuesByCitizen(Long citizenId);
    Issue getIssueEntity(Long id);
}
