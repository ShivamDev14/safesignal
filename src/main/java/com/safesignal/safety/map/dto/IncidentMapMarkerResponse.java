package com.safesignal.safety.map.dto;

import java.time.LocalDateTime;

public record IncidentMapMarkerResponse(
        Long reportId,
        Long segmentId,
        String streetName,
        double latitude,
        double longitude,
        String incidentType,
        int severity,
        LocalDateTime occurredAt,
        String description
) {
}