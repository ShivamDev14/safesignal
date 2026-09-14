package com.safesignal.safety.map.dto;

import java.time.LocalDateTime;

public record StreetlightMapMarkerResponse(
        Long streetlightId,
        Long segmentId,
        String streetName,
        double latitude,
        double longitude,
        String condition,
        LocalDateTime lastObservedAt,
        String source,
        String notes
) {
}