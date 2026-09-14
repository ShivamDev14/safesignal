package com.safesignal.routing.dto;

import java.util.List;

public record CoordinateRouteResponse(
        double distanceMeters,
        double durationSeconds,
        List<CoordinatePoint> coordinates
) {
}