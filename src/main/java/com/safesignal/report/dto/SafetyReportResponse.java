package com.safesignal.report.dto;

import com.safesignal.report.model.IncidentType;
import com.safesignal.report.model.ReportStatus;

import java.time.LocalDateTime;

public record SafetyReportResponse(
        Long id,
        Long streetSegmentId,
        IncidentType incidentType,
        Integer severity,
        LocalDateTime occurredAt,
        LocalDateTime reportedAt,
        String description,
        ReportStatus status
) {
}
