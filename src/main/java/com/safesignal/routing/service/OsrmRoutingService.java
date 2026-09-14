package com.safesignal.routing.service;

import com.safesignal.routing.dto.CoordinatePoint;
import com.safesignal.routing.dto.CoordinateRouteResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
public class OsrmRoutingService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OsrmRoutingService() {

        this.restClient = RestClient.builder()
                .defaultHeader("Accept-Encoding", "identity")
                .build();

        this.objectMapper = new ObjectMapper();
    }

    // ============================================================
    // GET MULTIPLE GENUINE ROUTES
    // ============================================================

    public List<CoordinateRouteResponse> getRoutes(
            double originLat,
            double originLon,
            double destinationLat,
            double destinationLon) {

        List<CoordinateRouteResponse> result =
                new ArrayList<>();

        // ========================================================
        // STEP 1
        // Ask OSRM for its own alternative routes.
        // ========================================================

        String normalUrl =
                buildRouteUrl(
                        originLat,
                        originLon,
                        destinationLat,
                        destinationLon,
                        null
                );

        System.out.println(
                "Requesting primary routes from OSRM:"
        );

        System.out.println(normalUrl);

        try {

            addRoutesFromOsrm(
                    normalUrl,
                    result
            );

        } catch (Exception e) {

            System.out.println(
                    "Primary OSRM request failed: "
                            + e.getMessage()
            );
        }

        System.out.println(
                "Primary OSRM search produced "
                        + result.size()
                        + " route(s)."
        );


        // ========================================================
        // STEP 2
        // If OSRM did not provide enough alternatives,
        // try genuine routes through different road corridors.
        //
        // These are WAYPOINTS, not fake route coordinates.
        // OSRM still calculates the complete route on its
        // actual road network.
        // ========================================================

        if (result.size() < 3) {

            List<Waypoint> candidateWaypoints =
                    buildCandidateWaypoints(
                            originLat,
                            originLon,
                            destinationLat,
                            destinationLon
                    );

            int candidateNumber = 1;

            for (Waypoint waypoint :
                    candidateWaypoints) {

                if (result.size() >= 3) {
                    break;
                }

                String waypointUrl =
                        buildRouteUrl(
                                originLat,
                                originLon,
                                destinationLat,
                                destinationLon,
                                waypoint
                        );

                System.out.println(
                        "Trying alternative road corridor "
                                + candidateNumber
                                + ":"
                );

                System.out.println(waypointUrl);

                try {

                    addRoutesFromOsrm(
                            waypointUrl,
                            result
                    );

                } catch (Exception e) {

                    System.out.println(
                            "Alternative corridor failed: "
                                    + e.getMessage()
                    );
                }

                candidateNumber++;
            }
        }


        // ========================================================
        // FINAL VALIDATION
        // ========================================================

        if (result.isEmpty()) {

            throw new RuntimeException(
                    "OSRM did not return any valid routes."
            );
        }

        System.out.println(
                "SafeSignal will use "
                        + result.size()
                        + " genuine route(s)."
        );

        return result;
    }


    // ============================================================
    // BUILD OSRM URL
    // ============================================================

    private String buildRouteUrl(
            double originLat,
            double originLon,
            double destinationLat,
            double destinationLon,
            Waypoint waypoint) {

        String coordinates =
                originLon + "," + originLat;

        if (waypoint != null) {

            coordinates +=
                    ";"
                            + waypoint.longitude
                            + ","
                            + waypoint.latitude;
        }

        coordinates +=
                ";"
                        + destinationLon
                        + ","
                        + destinationLat;

        return
                "https://router.project-osrm.org/route/v1/driving/"
                        + coordinates
                        + "?alternatives=true"
                        + "&overview=full"
                        + "&geometries=geojson";
    }


    // ============================================================
    // GET AND PROCESS ROUTES FROM ONE OSRM REQUEST
    // ============================================================

    private void addRoutesFromOsrm(
            String url,
            List<CoordinateRouteResponse> result) {

        String response =
                restClient.get()
                        .uri(url)
                        .header(
                                "Accept-Encoding",
                                "identity"
                        )
                        .header(
                                "User-Agent",
                                "SafeSignal/1.0"
                        )
                        .retrieve()
                        .body(String.class);

        if (
                response == null
                        || response.isBlank()
        ) {

            throw new RuntimeException(
                    "OSRM returned an empty response."
            );
        }


        try {

            JsonNode root =
                    objectMapper.readTree(response);

            JsonNode code =
                    root.get("code");

            if (
                    code == null
                            || !"Ok".equals(code.asText())
            ) {

                throw new RuntimeException(
                        "OSRM returned an invalid response."
                );
            }


            JsonNode routes =
                    root.get("routes");

            if (
                    routes == null
                            || !routes.isArray()
            ) {

                return;
            }


            System.out.println(
                    "OSRM returned "
                            + routes.size()
                            + " route(s) for this request."
            );


            for (JsonNode route :
                    routes) {

                CoordinateRouteResponse parsedRoute =
                        parseRoute(route);

                if (parsedRoute == null) {
                    continue;
                }


                if (
                        isDuplicateRoute(
                                result,
                                parsedRoute
                        )
                ) {

                    System.out.println(
                            "Skipping duplicate/near-identical route."
                    );

                    continue;
                }


                result.add(parsedRoute);


                System.out.println(
                        "Accepted route: "
                                + result.size()
                                + " | "
                                + Math.round(
                                parsedRoute.distanceMeters()
                        )
                                + " m | "
                                + Math.round(
                                parsedRoute.durationSeconds()
                        )
                                + " sec | "
                                + parsedRoute.coordinates().size()
                                + " points"
                );
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Could not parse OSRM response: "
                            + e.getMessage(),
                    e
            );
        }
    }


    // ============================================================
    // CONVERT ONE OSRM ROUTE
    // ============================================================

    private CoordinateRouteResponse parseRoute(
            JsonNode route) {

        JsonNode distanceNode =
                route.get("distance");

        JsonNode durationNode =
                route.get("duration");

        JsonNode geometryNode =
                route.get("geometry");

        if (
                distanceNode == null
                        || durationNode == null
                        || geometryNode == null
        ) {

            return null;
        }


        double distanceMeters =
                distanceNode.asDouble();

        double durationSeconds =
                durationNode.asDouble();


        JsonNode routeCoordinates =
                geometryNode.get("coordinates");

        if (
                routeCoordinates == null
                        || !routeCoordinates.isArray()
        ) {

            return null;
        }


        List<CoordinatePoint> coordinates =
                new ArrayList<>();


        // ========================================================
        // GeoJSON:
        // [longitude, latitude]
        //
        // SafeSignal:
        // CoordinatePoint(latitude, longitude)
        // ========================================================

        for (JsonNode coordinate :
                routeCoordinates) {

            if (
                    !coordinate.isArray()
                            || coordinate.size() < 2
            ) {

                continue;
            }


            double longitude =
                    coordinate
                            .get(0)
                            .asDouble();

            double latitude =
                    coordinate
                            .get(1)
                            .asDouble();


            if (
                    latitude < -90
                            || latitude > 90
                            || longitude < -180
                            || longitude > 180
            ) {

                continue;
            }


            coordinates.add(
                    new CoordinatePoint(
                            latitude,
                            longitude
                    )
            );
        }


        if (coordinates.size() < 2) {
            return null;
        }


        return new CoordinateRouteResponse(
                distanceMeters,
                durationSeconds,
                coordinates
        );
    }


    // ============================================================
    // BUILD CANDIDATE ROAD CORRIDORS
    // ============================================================
    //
    // These points are only WAYPOINTS.
    //
    // OSRM is responsible for finding the actual road from
    // origin -> waypoint -> destination.
    //
    // We deliberately use several points around the direct
    // corridor rather than drawing our own lines.
    // ============================================================

    private List<Waypoint> buildCandidateWaypoints(
            double originLat,
            double originLon,
            double destinationLat,
            double destinationLon) {

        List<Waypoint> waypoints =
                new ArrayList<>();


        double midLat =
                (originLat + destinationLat) / 2.0;

        double midLon =
                (originLon + destinationLon) / 2.0;


        double latDifference =
                destinationLat - originLat;

        double lonDifference =
                destinationLon - originLon;


        double length =
                Math.sqrt(
                        latDifference * latDifference
                                + lonDifference * lonDifference
                );


        if (length == 0) {
            return waypoints;
        }


        // --------------------------------------------------------
        // Unit vector perpendicular to the origin-destination
        // direction.
        // --------------------------------------------------------

        double perpendicularLat =
                -lonDifference / length;

        double perpendicularLon =
                latDifference / length;


        // --------------------------------------------------------
        // Candidate 1:
        // Slightly north of the direct corridor.
        // --------------------------------------------------------

        waypoints.add(
                new Waypoint(
                        midLat
                                + perpendicularLat * 0.025,
                        midLon
                                + perpendicularLon * 0.025
                )
        );


        // --------------------------------------------------------
        // Candidate 2:
        // Slightly south of the direct corridor.
        // --------------------------------------------------------

        waypoints.add(
                new Waypoint(
                        midLat
                                - perpendicularLat * 0.025,
                        midLon
                                - perpendicularLon * 0.025
                )
        );


        // --------------------------------------------------------
        // Candidate 3:
        // Larger northern deviation.
        // --------------------------------------------------------

        waypoints.add(
                new Waypoint(
                        midLat
                                + perpendicularLat * 0.045,
                        midLon
                                + perpendicularLon * 0.045
                )
        );


        // --------------------------------------------------------
        // Candidate 4:
        // Larger southern deviation.
        // --------------------------------------------------------

        waypoints.add(
                new Waypoint(
                        midLat
                                - perpendicularLat * 0.045,
                        midLon
                                - perpendicularLon * 0.045
                )
        );


        return waypoints;
    }


    // ============================================================
    // DUPLICATE ROUTE CHECK
    // ============================================================

    private boolean isDuplicateRoute(
            List<CoordinateRouteResponse> existingRoutes,
            CoordinateRouteResponse newRoute) {

        for (
                CoordinateRouteResponse existing :
                existingRoutes
        ) {

            double existingDistance =
                    existing.distanceMeters();

            double newDistance =
                    newRoute.distanceMeters();


            // ----------------------------------------------------
            // Distance check
            // ----------------------------------------------------

            double distanceDifference =
                    Math.abs(
                            existingDistance
                                    - newDistance
                    );


            double distanceTolerance =
                    Math.max(
                            100.0,
                            newDistance * 0.01
                    );


            if (
                    distanceDifference
                            > distanceTolerance
            ) {

                continue;
            }


            // ----------------------------------------------------
            // Geometry similarity check
            // ----------------------------------------------------

            List<CoordinatePoint> existingCoordinates =
                    existing.coordinates();

            List<CoordinatePoint> newCoordinates =
                    newRoute.coordinates();


            if (
                    existingCoordinates.isEmpty()
                            || newCoordinates.isEmpty()
            ) {

                continue;
            }


            int samples =
                    Math.min(
                            10,
                            Math.min(
                                    existingCoordinates.size(),
                                    newCoordinates.size()
                            )
                    );


            int matchingPoints = 0;


            for (
                    int i = 0;
                    i < samples;
                    i++
            ) {

                int existingIndex =
                        (int) (
                                (long) i
                                        * (
                                        existingCoordinates.size() - 1
                                )
                                        / Math.max(
                                        1,
                                        samples - 1
                                )
                        );


                int newIndex =
                        (int) (
                                (long) i
                                        * (
                                        newCoordinates.size() - 1
                                )
                                        / Math.max(
                                        1,
                                        samples - 1
                                )
                        );


                CoordinatePoint existingPoint =
                        existingCoordinates.get(
                                existingIndex
                        );

                CoordinatePoint newPoint =
                        newCoordinates.get(
                                newIndex
                        );


                double distance =
                        distanceInMeters(
                                existingPoint.latitude(),
                                existingPoint.longitude(),
                                newPoint.latitude(),
                                newPoint.longitude()
                        );


                if (
                        distance <= 100.0
                ) {

                    matchingPoints++;
                }
            }


            double similarity =
                    (double) matchingPoints
                            / samples;


            if (
                    similarity >= 0.8
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
    // WAYPOINT RECORD
    // ============================================================

    private static class Waypoint {

        private final double latitude;
        private final double longitude;

        private Waypoint(
                double latitude,
                double longitude) {

            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}