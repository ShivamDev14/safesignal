package com.safesignal.report.dto;

import com.safesignal.report.model.IncidentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateSafetyReportRequest(
        @NotNull(message = "streetSegmentId is required") Long streetSegmentId,
        @NotNull(message = "incidentType is required") IncidentType incidentType,
        @NotNull(message = "severity is required") @Min(value = 1, message = "severity must be between 1 and 5") @Max(value = 5, message = "severity must be between 1 and 5") Integer severity,
        @NotNull(message = "occurredAt is required") LocalDateTime occurredAt,
        @NotBlank(message = "description is required") @Size(max = 1000, message = "description must be at most 1000 characters") String description
) {
}
