package com.safesignal.route.dto;

import com.safesignal.routing.dto.CoordinatePoint;

import java.util.List;

public record RouteResponse(
        Long id,
        String name,
        Integer estimatedDurationMinutes,
        Integer distanceMeters,
        LocationSummaryResponse origin,
        LocationSummaryResponse destination,
        List<RouteSegmentResponse> segments,
        List<CoordinatePoint> geometry
) {
}