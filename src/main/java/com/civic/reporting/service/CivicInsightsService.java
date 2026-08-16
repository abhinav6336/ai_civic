package com.civic.reporting.service;

import com.civic.reporting.dto.response.CivicInsightsResponse;

public interface CivicInsightsService {
    /**
     * Generates comprehensive analytics distinguishing ground-truth OBSERVED DATA
     * from AI/ALGORITHMIC DERIVED INSIGHTS.
     */
    CivicInsightsResponse getCivicInsights();
}
