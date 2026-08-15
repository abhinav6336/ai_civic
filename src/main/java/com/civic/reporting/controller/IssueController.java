package com.civic.reporting.controller;

import com.civic.reporting.dto.request.IssueAssignRequest;
import com.civic.reporting.dto.request.IssueCreateRequest;
import com.civic.reporting.dto.request.IssueStatusUpdateRequest;
import com.civic.reporting.dto.response.ApiResponse;
import com.civic.reporting.dto.response.IssueResponse;
import com.civic.reporting.enums.IssueCategory;
import com.civic.reporting.enums.IssueStatus;
import com.civic.reporting.service.IssueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/issues")
@CrossOrigin(origins = "*")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    /**
     * Multipart form submission for citizen issue reporting with optional photo attachment.
     */
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ApiResponse<IssueResponse>> reportIssueMultipart(
            @ModelAttribute IssueCreateRequest request,
            @RequestParam(value = "image", required = false) MultipartFile imageFile) {
        IssueResponse created = issueService.createIssue(request, imageFile);
        return new ResponseEntity<>(ApiResponse.ok("Issue submitted successfully", created), HttpStatus.CREATED);
    }

    /**
     * JSON payload submission for API integrations.
     */
    @PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<ApiResponse<IssueResponse>> reportIssueJson(@Valid @RequestBody IssueCreateRequest request) {
        IssueResponse created = issueService.createIssue(request);
        return new ResponseEntity<>(ApiResponse.ok("Issue submitted successfully", created), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<IssueResponse>>> getIssues(
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) IssueCategory category,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String search) {
        List<IssueResponse> issues = issueService.getAllIssues(status, category, departmentId, search);
        return ResponseEntity.ok(ApiResponse.ok("Issues retrieved successfully", issues));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IssueResponse>> getIssueById(@PathVariable Long id) {
        IssueResponse issue = issueService.getIssueById(id);
        return ResponseEntity.ok(ApiResponse.ok(issue));
    }

    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<ApiResponse<IssueResponse>> trackIssue(@PathVariable String trackingNumber) {
        IssueResponse issue = issueService.getIssueByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(ApiResponse.ok("Issue tracking details retrieved", issue));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<IssueResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody IssueStatusUpdateRequest request) {
        IssueResponse updated = issueService.updateIssueStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Issue status updated successfully", updated));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<IssueResponse>> assignDepartment(
            @PathVariable Long id,
            @Valid @RequestBody IssueAssignRequest request) {
        IssueResponse assigned = issueService.assignIssue(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Issue assigned successfully", assigned));
    }

    @GetMapping("/citizen/{citizenId}")
    public ResponseEntity<ApiResponse<List<IssueResponse>>> getIssuesByCitizen(@PathVariable Long citizenId) {
        List<IssueResponse> issues = issueService.getIssuesByCitizen(citizenId);
        return ResponseEntity.ok(ApiResponse.ok(issues));
    }
}
