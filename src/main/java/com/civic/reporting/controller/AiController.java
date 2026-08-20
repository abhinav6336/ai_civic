package com.civic.reporting.controller;

import com.civic.reporting.ai.AiAnalysisResult;
import com.civic.reporting.ai.CivicAiEngine;
import com.civic.reporting.dto.request.AiAnalysisRequest;
import com.civic.reporting.dto.response.AiAnalysisResponse;
import com.civic.reporting.dto.response.ApiResponse;
import com.civic.reporting.entity.Issue;
import com.civic.reporting.service.IssueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@CrossOrigin(origins = "*")
public class AiController {

    private final CivicAiEngine aiEngine;
    private final IssueService issueService;

    public AiController(CivicAiEngine aiEngine, IssueService issueService) {
        this.aiEngine = aiEngine;
        this.issueService = issueService;
    }

    /**
     * Real-time triage endpoint for frontend smart category suggestion and risk assessment.
     */
    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<AiAnalysisResponse>> analyzeText(@RequestBody AiAnalysisRequest request) {
        AiAnalysisResult result = aiEngine.analyze(
                request.getTitle(),
                request.getDescription(),
                request.getLatitude(),
                request.getLongitude(),
                request.getAddress(),
                request.getCategory(),
                null,
                null
        );

        return ResponseEntity.ok(ApiResponse.ok("AI analysis completed successfully", AiAnalysisResponse.fromResult(result)));
    }

    /**
     * Retrieves AI diagnostics and classification insights for a specific issue.
     */
    @GetMapping("/diagnostics/{issueId}")
    public ResponseEntity<ApiResponse<AiAnalysisResponse>> getDiagnostics(@PathVariable Long issueId) {
        Issue issue = issueService.getIssueEntity(issueId);

        AiAnalysisResult result = aiEngine.analyze(
                issue.getTitle(),
                issue.getDescription(),
                issue.getLatitude(),
                issue.getLongitude(),
                issue.getAddress(),
                issue.getCategory(),
                issue.getAssignedDepartment() != null ? issue.getAssignedDepartment().getId() : null,
                null
        );

        return ResponseEntity.ok(ApiResponse.ok("AI diagnostics retrieved", AiAnalysisResponse.fromResult(result)));
    }
}
