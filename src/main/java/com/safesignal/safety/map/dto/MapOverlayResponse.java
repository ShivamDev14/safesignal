package com.safesignal.safety.map.dto;

import java.util.List;

public record MapOverlayResponse(
        List<IncidentMapMarkerResponse> incidents,
        List<StreetlightMapMarkerResponse> streetlights
) {
}