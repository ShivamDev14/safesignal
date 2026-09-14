package com.safesignal.safety.dto;

public record StreetlightSummaryResponse(
        int totalLights,
        int workingLights,
        int dimLights,
        int notWorkingLights,
        int unknownLights,
        int scoreAdjustment
) {
}
