package com.safesignal.safety.dto;

import com.safesignal.report.model.IncidentType;

import java.time.LocalDateTime;

public record RelevantReportResponse(
        IncidentType incidentType,
        Integer severity,
        LocalDateTime occurredAt,
        long minutesFromRequestedTime,
        long daysOld,
        int scoreContribution
) {
}
