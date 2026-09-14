package com.safesignal.route.dto;

public record RouteSegmentResponse(
        Integer sequenceOrder,
        StreetSegmentResponse streetSegment
) {
}