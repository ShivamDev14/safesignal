package com.safesignal.safety.dto;

import com.safesignal.safety.model.SafetyCategory;

import java.time.LocalDateTime;
import java.util.List;

public record SafetySummaryResponse(
        Long streetSegmentId,
        String streetSegmentName,
        LocalDateTime evaluatedAt,
        int safetyScore,
        SafetyCategory safetyCategory,
        int relevantReportCount,
        List<RelevantReportResponse> relevantReports,
        StreetlightSummaryResponse streetlightSummary,
        List<String> reasons,
        String disclaimer
) {
}
