package com.safesignal.route.service;

import com.safesignal.location.entity.DemoLocation;
import com.safesignal.location.repository.DemoLocationRepository;
import com.safesignal.route.dto.LocationSummaryResponse;
import com.safesignal.route.dto.RouteResponse;
import com.safesignal.route.dto.RouteSegmentResponse;
import com.safesignal.route.dto.StreetSegmentResponse;
import com.safesignal.route.entity.Route;
import com.safesignal.route.entity.RouteSegment;
import com.safesignal.route.repository.RouteRepository;
import com.safesignal.route.repository.RouteSegmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteCatalogService {

    private final RouteRepository routeRepository;
    private final RouteSegmentRepository routeSegmentRepository;
    private final DemoLocationRepository locationRepository;

    public RouteCatalogService(
            RouteRepository routeRepository,
            RouteSegmentRepository routeSegmentRepository,
            DemoLocationRepository locationRepository) {

        this.routeRepository = routeRepository;
        this.routeSegmentRepository = routeSegmentRepository;
        this.locationRepository = locationRepository;
    }

    public List<RouteResponse> getRoutes(Long originId, Long destinationId) {

        List<Route> routes =
                routeRepository.findByOriginIdAndDestinationIdAndActiveTrue(
                        originId,
                        destinationId
                );

        return routes.stream()
                .map(this::toRouteResponse)
                .toList();
    }

    private RouteResponse toRouteResponse(Route route) {

        List<RouteSegment> routeSegments =
                routeSegmentRepository.findByRouteIdOrderBySequenceNumberAsc(
                        route.getId()
                );

        List<RouteSegmentResponse> segments = routeSegments.stream()
                .map(this::toRouteSegmentResponse)
                .toList();

        return new RouteResponse(
                route.getId(),
                route.getName(),
                route.getEstimatedDurationMinutes(),
                route.getDistanceMeters(),
                toLocationResponse(route.getOrigin()),
                toLocationResponse(route.getDestination()),
                segments,
                List.of()
        );
    }

    private RouteSegmentResponse toRouteSegmentResponse(
            RouteSegment routeSegment) {

        var streetSegment = routeSegment.getStreetSegment();

        StreetSegmentResponse streetSegmentResponse =
                new StreetSegmentResponse(
                        streetSegment.getId(),
                        streetSegment.getName(),
                        streetSegment.getStartLatitude().doubleValue(),
                        streetSegment.getStartLongitude().doubleValue(),
                        streetSegment.getEndLatitude().doubleValue(),
                        streetSegment.getEndLongitude().doubleValue()
                );

        return new RouteSegmentResponse(
                routeSegment.getSequenceNumber(),
                streetSegmentResponse
        );
    }

    private LocationSummaryResponse toLocationResponse(
            DemoLocation location) {

        return new LocationSummaryResponse(
                location.getId(),
                location.getName(),
                location.getLatitude().doubleValue(),
                location.getLongitude().doubleValue()
        );
    }
}