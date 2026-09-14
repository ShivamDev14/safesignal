package com.safesignal.route.dto;

public record LocationSummaryResponse(
        Long id,
        String name,
        double latitude,
        double longitude
) {
}