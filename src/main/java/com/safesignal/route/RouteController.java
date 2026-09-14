package com.safesignal.route;

import com.safesignal.route.dto.RouteResponse;
import com.safesignal.route.service.RouteCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class RouteController {

    @Autowired
    private RouteCalculationService routeCalculationService;


    // ============================================================
    // LEGACY ROUTE EVALUATION ENDPOINT
    // ============================================================

    @GetMapping("/api/routes/evaluate")
    public ResponseEntity<List<RouteResponse>> evaluateRoutes(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam int hour) {

        List<RouteResponse> routes =
                routeCalculationService
                        .calculateRoutesForCoordinates(
                                19.0760,
                                72.8777,
                                19.0800,
                                72.8900,
                                hour
                        );


        return ResponseEntity.ok(
                routes
        );
    }


    // ============================================================
    // ROUTE RECOMMENDATIONS
    // ============================================================

    @GetMapping("/api/route-recommendations")
    public ResponseEntity<Map<String, Object>>
    getRouteRecommendations(

            @RequestParam double originLat,

            @RequestParam double originLon,

            @RequestParam double destinationLat,

            @RequestParam double destinationLon,

            @RequestParam String at) {


        LocalDateTime dateTime =
                LocalDateTime.parse(at);


        int hour =
                dateTime.getHour();


        // --------------------------------------------------------
        // GET REAL OSRM ROUTES
        // --------------------------------------------------------

        List<RouteResponse> evaluatedRoutes =
                routeCalculationService
                        .calculateRoutesForCoordinates(
                                originLat,
                                originLon,
                                destinationLat,
                                destinationLon,
                                hour
                        );


        // --------------------------------------------------------
        // CALCULATE SAFETY SCORES
        // --------------------------------------------------------

        List<RouteScore> scoredRoutes =
                new ArrayList<>();


        for (RouteResponse route :
                evaluatedRoutes) {

            double score =
                    routeCalculationService
                            .calculateSafetyScore(
                                    route,
                                    hour
                            );


            scoredRoutes.add(
                    new RouteScore(
                            route,
                            score
                    )
            );
        }


        // --------------------------------------------------------
        // FIND SAFEST ROUTE
        //
        // Lowest concern score wins.
        // If scores are equal, shorter route wins.
        // --------------------------------------------------------

        RouteScore safestRoute =
                scoredRoutes.stream()
                        .min(
                                Comparator
                                        .comparingDouble(
                                                RouteScore::score
                                        )
                                        .thenComparingInt(
                                                value ->
                                                        value.route()
                                                                .estimatedDurationMinutes()
                                        )
                        )
                        .orElse(null);


        // --------------------------------------------------------
        // FORMAT RESPONSE
        // --------------------------------------------------------

        List<Map<String, Object>> formattedRoutes =
                scoredRoutes.stream()
                        .map(
                                scoredRoute -> {

                                    RouteResponse route =
                                            scoredRoute.route();


                                    double score =
                                            scoredRoute.score();


                                    boolean recommended =
                                            safestRoute != null
                                                    &&
                                                    route.id()
                                                            .equals(
                                                                    safestRoute
                                                                            .route()
                                                                            .id()
                                                            );


                                    String safetyCategory;


                                    if (score == 0) {

                                        safetyCategory =
                                                "LOW_REPORTED_CONCERN";

                                    }

                                    else if (score <= 5) {

                                        safetyCategory =
                                                "MODERATE_REPORTED_CONCERN";

                                    }

                                    else {

                                        safetyCategory =
                                                "HIGHER_REPORTED_CONCERN";

                                    }


                                    String explanation;


                                    if (recommended) {

                                        if (score == 0) {

                                            explanation =
                                                    "Recommended: No nearby community safety reports matched this travel time.";

                                        }

                                        else {

                                            explanation =
                                                    "Recommended: Lowest reported safety concern for the selected travel time.";

                                        }

                                    }

                                    else if (score == 0) {

                                        explanation =
                                                "No nearby community safety reports matched this travel time.";

                                    }

                                    else {

                                        explanation =
                                                "Nearby community safety reports increase concern for this route at the selected time.";

                                    }


                                    Map<String, Object> result =
                                            new HashMap<>();


                                    result.put(
                                            "routeId",
                                            route.id()
                                    );


                                    result.put(
                                            "routeName",
                                            route.name()
                                    );


                                    result.put(
                                            "estimatedDurationMinutes",
                                            route.estimatedDurationMinutes()
                                    );


                                    result.put(
                                            "distanceMeters",
                                            route.distanceMeters()
                                    );


                                    result.put(
                                            "reportedConcernScore",
                                            score
                                    );


                                    result.put(
                                            "safetyCategory",
                                            safetyCategory
                                    );


                                    result.put(
                                            "recommended",
                                            recommended
                                    );


                                    result.put(
                                            "explanation",
                                            explanation
                                    );


                                    result.put(
                                            "segments",
                                            route.segments()
                                    );


                                    result.put(
                                            "geometry",
                                            route.geometry()
                                    );


                                    return result;
                                }
                        )
                        .toList();


        return ResponseEntity.ok(
                Map.of(
                        "routes",
                        formattedRoutes
                )
        );
    }


    // ============================================================
    // INTERNAL ROUTE SCORE
    // ============================================================

    private record RouteScore(
            RouteResponse route,
            double score
    ) {
    }
}