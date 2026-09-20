package com.safesignal.route.service;

import com.safesignal.report.entity.SafetyReport;
import com.safesignal.report.repository.SafetyReportRepository;
import com.safesignal.route.dto.LocationSummaryResponse;
import com.safesignal.route.dto.RouteResponse;
import com.safesignal.route.dto.RouteSegmentResponse;
import com.safesignal.route.dto.StreetSegmentResponse;
import com.safesignal.routing.dto.CoordinatePoint;
import com.safesignal.routing.dto.CoordinateRouteResponse;
import com.safesignal.routing.service.OsrmRoutingService;
import com.safesignal.streetlight.entity.Streetlight;
import com.safesignal.streetlight.model.StreetlightCondition;
import com.safesignal.streetlight.repository.StreetlightRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RouteCalculationService {

    private final OsrmRoutingService osrmRoutingService;

    private final SafetyReportRepository safetyReportRepository;

    private final StreetlightRepository streetlightRepository;


    public RouteCalculationService(
            OsrmRoutingService osrmRoutingService,
            SafetyReportRepository safetyReportRepository,
            StreetlightRepository streetlightRepository) {

        this.osrmRoutingService =
                osrmRoutingService;

        this.safetyReportRepository =
                safetyReportRepository;

        this.streetlightRepository =
                streetlightRepository;
    }


    // ============================================================
    // CALCULATE REAL OSRM ROUTES
    // ============================================================

    public List<RouteResponse> calculateRoutesForCoordinates(
            double originLat,
            double originLon,
            double destinationLat,
            double destinationLon,
            int travelHour) {

        // Ask OSRM for real road routes
        List<CoordinateRouteResponse> osrmRoutes =
                osrmRoutingService.getRoutes(
                        originLat,
                        originLon,
                        destinationLat,
                        destinationLon
                );


        LocationSummaryResponse origin =
                new LocationSummaryResponse(
                        1L,
                        "Selected Origin",
                        originLat,
                        originLon
                );


        LocationSummaryResponse destination =
                new LocationSummaryResponse(
                        2L,
                        "Selected Destination",
                        destinationLat,
                        destinationLon
                );


        List<RouteResponse> routes =
                new ArrayList<>();


        long routeId = 101L;

        int routeNumber = 1;


        for (CoordinateRouteResponse osrmRoute : osrmRoutes) {

            List<CoordinatePoint> geometry =
                    osrmRoute.coordinates();


            List<RouteSegmentResponse> segments =
                    new ArrayList<>();


            // ----------------------------------------------------
            // Create a SafeSignal segment representing this route
            // ----------------------------------------------------

            if (geometry.size() >= 2) {

                CoordinatePoint first =
                        geometry.get(0);


                CoordinatePoint last =
                        geometry.get(
                                geometry.size() - 1
                        );


                StreetSegmentResponse street =
                        new StreetSegmentResponse(
                                routeId,
                                "OSRM Route " + routeNumber,
                                first.latitude(),
                                first.longitude(),
                                last.latitude(),
                                last.longitude()
                        );


                segments.add(
                        new RouteSegmentResponse(
                                1,
                                street
                        )
                );

            }


            int durationMinutes =
                    (int) Math.ceil(
                            osrmRoute.durationSeconds() / 60.0
                    );


            int distanceMeters =
                    (int) Math.round(
                            osrmRoute.distanceMeters()
                    );


            RouteResponse route =
                    new RouteResponse(
                            routeId,
                            "Route " + routeNumber,
                            durationMinutes,
                            distanceMeters,
                            origin,
                            destination,
                            segments,
                            geometry
                    );


            routes.add(route);


            routeId++;

            routeNumber++;
        }


        return routes;
    }


    // ============================================================
    // CALCULATE TOTAL SAFETY CONCERN SCORE
    // ============================================================

    public double calculateSafetyScore(
            RouteResponse route,
            int travelHour) {

        // ========================================================
        // PART 1 — SAFETY INCIDENT SCORE
        // ========================================================

        List<SafetyReport> reports =
                safetyReportRepository.findAll();


        double incidentScore = 0.0;


        for (SafetyReport report : reports) {

            // ----------------------------------------------------
            // Ignore reports that are not near this route
            // ----------------------------------------------------

            if (!isReportNearRoute(
                    report,
                    route.geometry()
            )) {

                continue;
            }


            // ----------------------------------------------------
            // Check time relevance
            // ----------------------------------------------------

            int reportHour =
                    report.getOccurredAt()
                            .getHour();


            int hourDifference =
                    circularHourDifference(
                            travelHour,
                            reportHour
                    );


            double timeWeight;


            if (hourDifference <= 1) {

                timeWeight = 1.0;

            }

            else if (hourDifference <= 2) {

                timeWeight = 0.75;

            }

            else if (hourDifference <= 4) {

                timeWeight = 0.35;

            }

            else {

                timeWeight = 0.0;

            }


            if (timeWeight == 0.0) {

                continue;

            }


            // ----------------------------------------------------
            // Severity contribution
            // ----------------------------------------------------

            double severityWeight =
                    report.getSeverity();


            // ----------------------------------------------------
            // Night-time multiplier
            // ----------------------------------------------------

            double nightMultiplier =
                    isNightTime(travelHour)
                            ? 1.25
                            : 1.0;


            double reportContribution =
                    severityWeight
                            * timeWeight
                            * nightMultiplier;


            incidentScore +=
                    reportContribution;
        }


        // ========================================================
        // PART 2 — STREETLIGHT SCORE
        // ========================================================

        double streetlightScore =
                calculateStreetlightScore(
                        route,
                        travelHour
                );


        // ========================================================
        // FINAL SCORE
        // ========================================================

        double totalScore =
                incidentScore
                        + streetlightScore;


        return Math.round(
                totalScore * 100.0
        ) / 100.0;
    }


    // ============================================================
    // CALCULATE STREETLIGHT CONCERN SCORE
    // ============================================================

    private double calculateStreetlightScore(
            RouteResponse route,
            int travelHour) {

        List<Streetlight> streetlights =
                streetlightRepository.findAll();


        double totalScore = 0.0;


        for (Streetlight streetlight : streetlights) {

            // ----------------------------------------------------
            // Ignore streetlights that are not near this route
            // ----------------------------------------------------

            if (!isStreetlightNearRoute(
                    streetlight,
                    route.geometry()
            )) {

                continue;
            }


            // ----------------------------------------------------
            // Working light
            // ----------------------------------------------------

            if (streetlight.getCondition()
                    == StreetlightCondition.WORKING) {

                continue;
            }


            // ----------------------------------------------------
            // Dim light
            // ----------------------------------------------------

            if (streetlight.getCondition()
                    == StreetlightCondition.DIM) {

                totalScore +=
                        isNightTime(travelHour)
                                ? 1.0
                                : 0.5;

            }


            // ----------------------------------------------------
            // Not working light
            // ----------------------------------------------------

            else if (streetlight.getCondition()
                    == StreetlightCondition.NOT_WORKING) {

                totalScore +=
                        isNightTime(travelHour)
                                ? 2.0
                                : 1.0;

            }

        }


        return totalScore;
    }


    // ============================================================
    // CHECK WHETHER SAFETY REPORT IS CLOSE TO ROUTE
    // ============================================================

    private boolean isReportNearRoute(
            SafetyReport report,
            List<CoordinatePoint> geometry) {

        if (
                geometry == null ||
                        geometry.isEmpty()
        ) {

            return false;
        }


        double reportLatitude =
                report.getStreetSegment()
                        .getStartLatitude()
                        .doubleValue();


        double reportLongitude =
                report.getStreetSegment()
                        .getStartLongitude()
                        .doubleValue();


        final double MAX_DISTANCE_METERS =
                100.0;


        for (CoordinatePoint point : geometry) {

            double distance =
                    distanceInMeters(
                            reportLatitude,
                            reportLongitude,
                            point.latitude(),
                            point.longitude()
                    );


            if (
                    distance <=
                            MAX_DISTANCE_METERS
            ) {

                return true;
            }
        }


        return false;
    }


    // ============================================================
    // CHECK WHETHER STREETLIGHT IS CLOSE TO ROUTE
    // ============================================================

    private boolean isStreetlightNearRoute(
            Streetlight streetlight,
            List<CoordinatePoint> geometry) {

        if (
                geometry == null ||
                        geometry.isEmpty()
        ) {

            return false;
        }


        double streetlightLatitude =
                streetlight.getLatitude()
                        .doubleValue();


        double streetlightLongitude =
                streetlight.getLongitude()
                        .doubleValue();


        final double MAX_DISTANCE_METERS =
                100.0;


        for (CoordinatePoint point : geometry) {

            double distance =
                    distanceInMeters(
                            streetlightLatitude,
                            streetlightLongitude,
                            point.latitude(),
                            point.longitude()
                    );


            if (
                    distance <=
                            MAX_DISTANCE_METERS
            ) {

                return true;
            }
        }


        return false;
    }


    // ============================================================
    // HAVERSINE DISTANCE
    // ============================================================

    private double distanceInMeters(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

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


    // ============================================================
    // CIRCULAR HOUR DIFFERENCE
    // ============================================================

    private int circularHourDifference(
            int hour1,
            int hour2) {

        int difference =
                Math.abs(
                        hour1 - hour2
                );


        return Math.min(
                difference,
                24 - difference
        );
    }


    // ============================================================
    // NIGHT TIME
    // ============================================================

    private boolean isNightTime(
            int hour) {

        return hour >= 22 ||
                hour <= 5;
    }
}