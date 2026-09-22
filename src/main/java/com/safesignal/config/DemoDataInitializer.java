package com.safesignal.config;

import com.safesignal.common.model.DataSource;
import com.safesignal.location.entity.DemoLocation;
import com.safesignal.location.repository.DemoLocationRepository;
import com.safesignal.report.entity.SafetyReport;
import com.safesignal.report.model.IncidentType;
import com.safesignal.report.model.ReportStatus;
import com.safesignal.report.repository.SafetyReportRepository;
import com.safesignal.route.entity.Route;
import com.safesignal.route.entity.RouteSegment;
import com.safesignal.route.repository.RouteRepository;
import com.safesignal.route.repository.RouteSegmentRepository;
import com.safesignal.streetlight.entity.StreetSegment;
import com.safesignal.streetlight.entity.Streetlight;
import com.safesignal.streetlight.model.StreetlightCondition;
import com.safesignal.streetlight.repository.StreetSegmentRepository;
import com.safesignal.streetlight.repository.StreetlightRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@Profile({"mysql", "default", "h2", "tidb"})
public class DemoDataInitializer {

    @Bean
    CommandLineRunner seedDemoData(DemoLocationRepository locationRepository,
                                   StreetSegmentRepository streetSegmentRepository,
                                   RouteRepository routeRepository,
                                   RouteSegmentRepository routeSegmentRepository,
                                   StreetlightRepository streetlightRepository,
                                   SafetyReportRepository safetyReportRepository) {
        return args -> {
            if (locationRepository.count() > 0) {
                return;
            }

            DemoLocation centralMall = locationRepository.save(location("Central Mall", "12.9716000", "77.5946000", "Central District"));
            DemoLocation riversideApartments = locationRepository.save(location("Riverside Apartments", "12.9635000", "77.6012000", "Riverside"));
            locationRepository.save(location("City Station", "12.9750000", "77.5990000", "Station Quarter"));
            locationRepository.save(location("University Gate", "12.9680000", "77.5880000", "University Area"));

            StreetSegment centralAvenue = streetSegmentRepository.save(segment("Central Avenue", "12.9716000", "77.5946000", "12.9701000", "77.5961000", 260, "Central District"));
            StreetSegment marketLane = streetSegmentRepository.save(segment("Market Lane", "12.9701000", "77.5961000", "12.9678000", "77.5983000", 410, "Market District"));
            StreetSegment riversideRoad = streetSegmentRepository.save(segment("Riverside Road", "12.9678000", "77.5983000", "12.9635000", "77.6012000", 610, "Riverside"));
            StreetSegment parkAvenue = streetSegmentRepository.save(segment("Park Avenue", "12.9716000", "77.5946000", "12.9695000", "77.5919000", 380, "Central District"));
            StreetSegment libraryRoad = streetSegmentRepository.save(segment("Library Road", "12.9695000", "77.5919000", "12.9662000", "77.5957000", 530, "University Area"));
            StreetSegment stationRoad = streetSegmentRepository.save(segment("Station Road", "12.9716000", "77.5946000", "12.9739000", "77.5981000", 460, "Station Quarter"));
            StreetSegment oldMillStreet = streetSegmentRepository.save(segment("Old Mill Street", "12.9739000", "77.5981000", "12.9635000", "77.6012000", 790, "Riverside"));

            Route marketDirect = routeRepository.save(new Route("Market Direct", centralMall, riversideApartments, 18, 1280, DataSource.SIMULATED, true));
            Route parkRoute = routeRepository.save(new Route("Park Avenue Route", centralMall, riversideApartments, 22, 1520, DataSource.SIMULATED, true));
            Route stationRoute = routeRepository.save(new Route("Station Road Route", centralMall, riversideApartments, 25, 1860, DataSource.SIMULATED, true));

            routeSegmentRepository.saveAll(List.of(
                    new RouteSegment(marketDirect, centralAvenue, 1, 240),
                    new RouteSegment(marketDirect, marketLane, 2, 360),
                    new RouteSegment(marketDirect, riversideRoad, 3, 480),
                    new RouteSegment(parkRoute, parkAvenue, 1, 360),
                    new RouteSegment(parkRoute, libraryRoad, 2, 420),
                    new RouteSegment(parkRoute, riversideRoad, 3, 540),
                    new RouteSegment(stationRoute, stationRoad, 1, 390),
                    new RouteSegment(stationRoute, oldMillStreet, 2, 570),
                    new RouteSegment(stationRoute, riversideRoad, 3, 540)
            ));

            LocalDateTime observedAt = LocalDateTime.of(2026, 9, 8, 20, 0);
            streetlightRepository.saveAll(List.of(
                    light(centralAvenue, "12.9709000", "77.5953000", StreetlightCondition.WORKING, observedAt, "Simulated demo light"),
                    light(marketLane, "12.9693000", "77.5969000", StreetlightCondition.DIM, observedAt, "Simulated demo light: dim after dusk"),
                    light(marketLane, "12.9683000", "77.5978000", StreetlightCondition.NOT_WORKING, observedAt, "Simulated demo light: reported non-working"),
                    light(riversideRoad, "12.9656000", "77.5997000", StreetlightCondition.WORKING, observedAt, "Simulated demo light"),
                    light(parkAvenue, "12.9704000", "77.5932000", StreetlightCondition.WORKING, observedAt, "Simulated demo light"),
                    light(libraryRoad, "12.9679000", "77.5937000", StreetlightCondition.WORKING, observedAt, "Simulated demo light"),
                    light(stationRoad, "12.9727000", "77.5963000", StreetlightCondition.WORKING, observedAt, "Simulated demo light"),
                    light(oldMillStreet, "12.9692000", "77.5990000", StreetlightCondition.DIM, observedAt, "Simulated demo light: uneven coverage")
            ));

            safetyReportRepository.saveAll(List.of(
                    report(marketLane, IncidentType.HARASSMENT, 4, LocalDateTime.of(2026, 9, 5, 23, 0), LocalDateTime.of(2026, 9, 6, 9, 15), "Demo report: harassment observed near closing time.", "demo-market-2300"),
                    report(marketLane, IncidentType.UNWANTED_ATTENTION, 3, LocalDateTime.of(2026, 9, 3, 22, 30), LocalDateTime.of(2026, 9, 4, 8, 40), "Demo report: unwanted attention reported late at night.", "demo-market-2230"),
                    report(marketLane, IncidentType.POOR_VISIBILITY, 2, LocalDateTime.of(2026, 9, 7, 22, 45), LocalDateTime.of(2026, 9, 8, 7, 30), "Demo report: poor visibility near dim lights.", "demo-market-2245"),
                    report(parkAvenue, IncidentType.UNSAFE_CROWDING, 2, LocalDateTime.of(2026, 8, 30, 13, 0), LocalDateTime.of(2026, 8, 30, 15, 10), "Demo report: crowding observed during the afternoon.", "demo-park-1300"),
                    report(oldMillStreet, IncidentType.POOR_VISIBILITY, 3, LocalDateTime.of(2026, 9, 2, 23, 15), LocalDateTime.of(2026, 9, 3, 8, 0), "Demo report: lighting was uneven late at night.", "demo-mill-2315")
            ));
        };
    }

    private DemoLocation location(String name, String latitude, String longitude, String areaName) {
        return new DemoLocation(name, number(latitude), number(longitude), areaName, true);
    }

    private StreetSegment segment(String name, String startLatitude, String startLongitude, String endLatitude,
                                  String endLongitude, int lengthMeters, String areaName) {
        return new StreetSegment(name, number(startLatitude), number(startLongitude), number(endLatitude), number(endLongitude), lengthMeters, areaName);
    }

    private Streetlight light(StreetSegment segment, String latitude, String longitude, StreetlightCondition condition,
                              LocalDateTime observedAt, String notes) {
        return new Streetlight(segment, number(latitude), number(longitude), condition, observedAt, DataSource.SIMULATED, notes);
    }

    private SafetyReport report(StreetSegment segment, IncidentType type, int severity, LocalDateTime occurredAt,
                                LocalDateTime reportedAt, String description, String token) {
        return new SafetyReport(segment, type, severity, occurredAt, reportedAt, description, ReportStatus.APPROVED, token);
    }

    private BigDecimal number(String value) {
        return new BigDecimal(value);
    }
}
