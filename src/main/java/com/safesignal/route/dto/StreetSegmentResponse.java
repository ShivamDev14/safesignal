package com.safesignal.route.dto;

public record StreetSegmentResponse(
        Long id,
        String name,
        double startLatitude,
        double startLongitude,
        double endLatitude,
        double endLongitude
) {
}