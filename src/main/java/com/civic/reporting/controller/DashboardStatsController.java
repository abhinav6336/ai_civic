package com.civic.reporting.controller;

import com.civic.reporting.dto.response.ApiResponse;
import com.civic.reporting.dto.response.CivicInsightsResponse;
import com.civic.reporting.dto.response.DashboardStatsResponse;
import com.civic.reporting.service.CivicInsightsService;
import com.civic.reporting.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@CrossOrigin(origins = "*")
public class DashboardStatsController {

    private final DashboardService dashboardService;
    private final CivicInsightsService civicInsightsService;

    public DashboardStatsController(DashboardService dashboardService, CivicInsightsService civicInsightsService) {
        this.dashboardService = dashboardService;
        this.civicInsightsService = civicInsightsService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats() {
        DashboardStatsResponse stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.ok("Dashboard statistics retrieved", stats));
    }

    @GetMapping("/insights")
    public ResponseEntity<ApiResponse<CivicInsightsResponse>> getInsights() {
        CivicInsightsResponse insights = civicInsightsService.getCivicInsights();
        return ResponseEntity.ok(ApiResponse.ok("Civic insights and analytics generated", insights));
    }
}
