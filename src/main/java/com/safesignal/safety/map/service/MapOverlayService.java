package com.safesignal.safety.map.service;

import com.safesignal.report.entity.SafetyReport;
import com.safesignal.report.model.ReportStatus;
import com.safesignal.report.repository.SafetyReportRepository;
import com.safesignal.route.entity.Route;
import com.safesignal.route.entity.RouteSegment;
import com.safesignal.route.repository.RouteRepository;
import com.safesignal.route.repository.RouteSegmentRepository;
import com.safesignal.safety.map.dto.IncidentMapMarkerResponse;
import com.safesignal.safety.map.dto.MapOverlayResponse;
import com.safesignal.safety.map.dto.StreetlightMapMarkerResponse;
import com.safesignal.streetlight.entity.StreetSegment;
import com.safesignal.streetlight.entity.Streetlight;
import com.safesignal.streetlight.repository.StreetlightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MapOverlayService {

    private final RouteRepository routeRepository;
    private final RouteSegmentRepository routeSegmentRepository;
    private final SafetyReportRepository safetyReportRepository;
    private final StreetlightRepository streetlightRepository;

    public MapOverlayService(
            RouteRepository routeRepository,
            RouteSegmentRepository routeSegmentRepository,
            SafetyReportRepository safetyReportRepository,
            StreetlightRepository streetlightRepository
    ) {
        this.routeRepository = routeRepository;
        this.routeSegmentRepository = routeSegmentRepository;
        this.safetyReportRepository = safetyReportRepository;
        this.streetlightRepository = streetlightRepository;
    }

    @Transactional(readOnly = true)
    public MapOverlayResponse getOverlay(
            Long originId,
            Long destinationId,
            LocalDateTime at
    ) {

        List<Route> routes = routeRepository.findAll()
                .stream()
                .filter(Route::isActive)
                .filter(route -> route.getOrigin().getId().equals(originId))
                .filter(route -> route.getDestination().getId().equals(destinationId))
                .toList();

        List<IncidentMapMarkerResponse> incidents = new ArrayList<>();
        List<StreetlightMapMarkerResponse> streetlights = new ArrayList<>();

        // Prevent duplicate markers when multiple routes share the same street segment
        Set<Long> processedSegmentIds = new HashSet<>();

        for (Route route : routes) {

            List<RouteSegment> routeSegments =
                    routeSegmentRepository.findByRouteIdOrderBySequenceNumberAsc(route.getId());

            for (RouteSegment routeSegment : routeSegments) {

                StreetSegment segment = routeSegment.getStreetSegment();

                // Skip this segment if another route has already processed it
                if (!processedSegmentIds.add(segment.getId())) {
                    continue;
                }

                // =========================
                // SAFETY INCIDENTS
                // =========================

                List<SafetyReport> reports =
                        safetyReportRepository.findByStreetSegmentIdAndStatus(
                                segment.getId(),
                                ReportStatus.APPROVED
                        );

                for (SafetyReport report : reports) {

                    if (report.getOccurredAt().isAfter(at)) {
                        continue;
                    }

                    double latitude =
                            (segment.getStartLatitude().doubleValue()
                                    + segment.getEndLatitude().doubleValue()) / 2.0;

                    double longitude =
                            (segment.getStartLongitude().doubleValue()
                                    + segment.getEndLongitude().doubleValue()) / 2.0;

                    incidents.add(
                            new IncidentMapMarkerResponse(
                                    report.getId(),
                                    segment.getId(),
                                    segment.getName(),
                                    latitude,
                                    longitude,
                                    report.getIncidentType().name(),
                                    report.getSeverity(),
                                    report.getOccurredAt(),
                                    report.getDescription()
                            )
                    );
                }

                // =========================
                // STREETLIGHTS
                // =========================

                List<Streetlight> segmentStreetlights =
                        streetlightRepository.findByStreetSegmentId(segment.getId());

                for (Streetlight light : segmentStreetlights) {

                    streetlights.add(
                            new StreetlightMapMarkerResponse(
                                    light.getId(),
                                    segment.getId(),
                                    segment.getName(),
                                    light.getLatitude().doubleValue(),
                                    light.getLongitude().doubleValue(),
                                    light.getCondition().name(),
                                    light.getLastObservedAt(),
                                    light.getSource().name(),
                                    light.getNotes()
                            )
                    );
                }
            }
        }

        return new MapOverlayResponse(
                incidents,
                streetlights
        );
    }
}