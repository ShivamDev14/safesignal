package com.safesignal.safety.map.service;

import com.safesignal.report.entity.SafetyReport;
import com.safesignal.report.repository.SafetyReportRepository;
import com.safesignal.safety.map.dto.IncidentMapMarkerResponse;
import com.safesignal.safety.map.dto.MapOverlayResponse;
import com.safesignal.safety.map.dto.StreetlightMapMarkerResponse;
import com.safesignal.streetlight.entity.Streetlight;
import com.safesignal.streetlight.repository.StreetlightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MapOverlayService {

    private final SafetyReportRepository safetyReportRepository;
    private final StreetlightRepository streetlightRepository;

    public MapOverlayService(
            SafetyReportRepository safetyReportRepository,
            StreetlightRepository streetlightRepository
    ) {
        this.safetyReportRepository =
                safetyReportRepository;

        this.streetlightRepository =
                streetlightRepository;
    }

    @Transactional(readOnly = true)
    public MapOverlayResponse getOverlay(
            double originLat,
            double originLon,
            double destinationLat,
            double destinationLon,
            LocalDateTime at
    ) {

        List<IncidentMapMarkerResponse> incidents =
                new ArrayList<>();

        List<StreetlightMapMarkerResponse> streetlights =
                new ArrayList<>();


        // =========================================================
        // SAFETY INCIDENTS
        // =========================================================

        List<SafetyReport> reports =
                safetyReportRepository.findAll();

        for (SafetyReport report : reports) {

            // Ignore reports that happened after the selected time
            if (report.getOccurredAt().isAfter(at)) {
                continue;
            }

            if (report.getStreetSegment() == null) {
                continue;
            }

            double latitude =
                    (
                            report.getStreetSegment()
                                    .getStartLatitude()
                                    .doubleValue()
                                    +
                                    report.getStreetSegment()
                                            .getEndLatitude()
                                            .doubleValue()
                    ) / 2.0;

            double longitude =
                    (
                            report.getStreetSegment()
                                    .getStartLongitude()
                                    .doubleValue()
                                    +
                                    report.getStreetSegment()
                                            .getEndLongitude()
                                            .doubleValue()
                    ) / 2.0;


            /*
             * Only display incidents reasonably close
             * to the selected journey area.
             */

            double originDistance =
                    distanceInMeters(
                            originLat,
                            originLon,
                            latitude,
                            longitude
                    );

            double destinationDistance =
                    distanceInMeters(
                            destinationLat,
                            destinationLon,
                            latitude,
                            longitude
                    );


            /*
             * If the incident is more than 5 km
             * from BOTH origin and destination,
             * don't display it.
             */

            if (
                    originDistance > 5000
                            &&
                            destinationDistance > 5000
            ) {
                continue;
            }


            incidents.add(
                    new IncidentMapMarkerResponse(
                            report.getId(),
                            report.getStreetSegment().getId(),
                            report.getStreetSegment().getName(),
                            latitude,
                            longitude,
                            report.getIncidentType().name(),
                            report.getSeverity(),
                            report.getOccurredAt(),
                            report.getDescription()
                    )
            );
        }


        // =========================================================
        // STREETLIGHTS
        // =========================================================

        List<Streetlight> lights =
                streetlightRepository.findAll();

        for (Streetlight light : lights) {

            if (light.getStreetSegment() == null) {
                continue;
            }

            double latitude =
                    light.getLatitude().doubleValue();

            double longitude =
                    light.getLongitude().doubleValue();


            double originDistance =
                    distanceInMeters(
                            originLat,
                            originLon,
                            latitude,
                            longitude
                    );

            double destinationDistance =
                    distanceInMeters(
                            destinationLat,
                            destinationLon,
                            latitude,
                            longitude
                    );


            /*
             * Only display streetlights reasonably close
             * to the selected journey area.
             */

            if (
                    originDistance > 5000
                            &&
                            destinationDistance > 5000
            ) {
                continue;
            }


            streetlights.add(
                    new StreetlightMapMarkerResponse(
                            light.getId(),
                            light.getStreetSegment().getId(),
                            light.getStreetSegment().getName(),
                            latitude,
                            longitude,
                            light.getCondition().name(),
                            light.getLastObservedAt(),
                            light.getSource().name(),
                            light.getNotes()
                    )
            );
        }


        // =========================================================
        // RETURN MAP OVERLAY
        // =========================================================

        return new MapOverlayResponse(
                incidents,
                streetlights
        );
    }


    // =============================================================
    // DISTANCE CALCULATION
    // =============================================================

    private double distanceInMeters(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {

        final double EARTH_RADIUS_METERS =
                6_371_000.0;

        double lat1Radians =
                Math.toRadians(lat1);

        double lat2Radians =
                Math.toRadians(lat2);

        double deltaLat =
                Math.toRadians(
                        lat2 - lat1
                );

        double deltaLon =
                Math.toRadians(
                        lon2 - lon1
                );

        double a =
                Math.sin(deltaLat / 2)
                        * Math.sin(deltaLat / 2)
                        +
                        Math.cos(lat1Radians)
                                * Math.cos(lat2Radians)
                                * Math.sin(deltaLon / 2)
                                * Math.sin(deltaLon / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );

        return EARTH_RADIUS_METERS * c;
    }
}