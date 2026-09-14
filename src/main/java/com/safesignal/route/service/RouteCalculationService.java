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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RouteCalculationService {

    private final OsrmRoutingService osrmRoutingService;

    private final SafetyReportRepository safetyReportRepository;


    public RouteCalculationService(
            OsrmRoutingService osrmRoutingService,
            SafetyReportRepository safetyReportRepository) {

        this.osrmRoutingService =
                osrmRoutingService;

        this.safetyReportRepository =
                safetyReportRepository;
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
    // CALCULATE SAFETY CONCERN SCORE
    // ============================================================

    public double calculateSafetyScore(
            RouteResponse route,
            int travelHour) {

        List<SafetyReport> reports =
                safetyReportRepository.findAll();


        double totalScore = 0.0;


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


            /*
             * Reports within 2 hours of the travel time
             * are considered highly relevant.
             *
             * Reports 3-4 hours away still contribute,
             * but with lower weight.
             */

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


            totalScore +=
                    reportContribution;

        }


        return Math.round(
                totalScore * 100.0
        ) / 100.0;
    }


    // ============================================================
    // CHECK WHETHER REPORT IS CLOSE TO ROUTE
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


        /*
         * 100 metres is used as the matching radius.
         *
         * This works well for our prototype because
         * reports are created from a map click.
         */

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