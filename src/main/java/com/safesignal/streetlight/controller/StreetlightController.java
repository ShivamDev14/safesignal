package com.safesignal.streetlight.controller;

import com.safesignal.common.model.DataSource;
import com.safesignal.streetlight.entity.StreetSegment;
import com.safesignal.streetlight.entity.Streetlight;
import com.safesignal.streetlight.model.StreetlightCondition;
import com.safesignal.streetlight.repository.StreetSegmentRepository;
import com.safesignal.streetlight.repository.StreetlightRepository;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Profile("mysql")
@RequestMapping("/api/streetlights")
public class StreetlightController {

    private final StreetlightRepository streetlightRepository;
    private final StreetSegmentRepository streetSegmentRepository;

    public StreetlightController(
            StreetlightRepository streetlightRepository,
            StreetSegmentRepository streetSegmentRepository) {

        this.streetlightRepository =
                streetlightRepository;

        this.streetSegmentRepository =
                streetSegmentRepository;
    }

    // ============================================================
    // GET ALL STREETLIGHTS
    // ============================================================

    @GetMapping
    public List<Map<String, Object>> getAllStreetlights() {

        List<Streetlight> streetlights =
                streetlightRepository.findAll();

        List<Map<String, Object>> response =
                new ArrayList<>();

        for (Streetlight streetlight : streetlights) {

            Map<String, Object> item =
                    new HashMap<>();

            item.put(
                    "id",
                    streetlight.getId()
            );

            item.put(
                    "latitude",
                    streetlight.getLatitude()
            );

            item.put(
                    "longitude",
                    streetlight.getLongitude()
            );

            item.put(
                    "condition",
                    streetlight.getCondition().name()
            );

            item.put(
                    "lastObservedAt",
                    streetlight.getLastObservedAt()
            );

            item.put(
                    "source",
                    streetlight.getSource().name()
            );

            item.put(
                    "notes",
                    streetlight.getNotes() == null
                            ? ""
                            : streetlight.getNotes()
            );

            item.put(
                    "streetSegmentId",
                    streetlight
                            .getStreetSegment()
                            .getId()
            );

            response.add(item);
        }

        return response;
    }

    // ============================================================
    // REPORT STREETLIGHT
    // ============================================================

    @PostMapping
    public Map<String, Object> reportStreetlight(
            @RequestBody Map<String, Object> request) {

        double latitude =
                ((Number) request.get("latitude"))
                        .doubleValue();

        double longitude =
                ((Number) request.get("longitude"))
                        .doubleValue();

        String streetName =
                String.valueOf(
                        request.get("streetName")
                );

        String areaName =
                String.valueOf(
                        request.get("areaName")
                );

        String conditionValue =
                String.valueOf(
                        request.get("condition")
                );

        String notes =
                request.get("notes") == null
                        ? ""
                        : String.valueOf(
                        request.get("notes")
                );

        // --------------------------------------------------------
        // CONVERT CONDITION STRING TO ENUM
        // --------------------------------------------------------

        StreetlightCondition condition =
                StreetlightCondition.valueOf(
                        conditionValue.toUpperCase()
                );

        // --------------------------------------------------------
        // FIND EXISTING STREET SEGMENT
        // --------------------------------------------------------

        StreetSegment segment =
                streetSegmentRepository.findAll()
                        .stream()
                        .filter(existing ->
                                existing.getName()
                                        .equalsIgnoreCase(streetName)
                                        &&
                                        existing.getAreaName()
                                                .equalsIgnoreCase(areaName)
                        )
                        .findFirst()
                        .orElseGet(() -> {

                            StreetSegment newSegment =
                                    new StreetSegment(
                                            streetName,
                                            BigDecimal.valueOf(
                                                    latitude
                                            ),
                                            BigDecimal.valueOf(
                                                    longitude
                                            ),
                                            BigDecimal.valueOf(
                                                    latitude
                                            ),
                                            BigDecimal.valueOf(
                                                    longitude
                                            ),
                                            1,
                                            areaName
                                    );

                            return streetSegmentRepository
                                    .save(newSegment);
                        });

        // --------------------------------------------------------
        // CREATE COMMUNITY STREETLIGHT REPORT
        // --------------------------------------------------------

        Streetlight streetlight =
                new Streetlight(
                        segment,
                        BigDecimal.valueOf(latitude),
                        BigDecimal.valueOf(longitude),
                        condition,
                        LocalDateTime.now(),
                        DataSource.COMMUNITY_REPORTED,
                        notes
                );

        Streetlight saved =
                streetlightRepository.save(
                        streetlight
                );

        // --------------------------------------------------------
        // RESPONSE
        // --------------------------------------------------------

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "id",
                saved.getId()
        );

        response.put(
                "latitude",
                saved.getLatitude()
        );

        response.put(
                "longitude",
                saved.getLongitude()
        );

        response.put(
                "condition",
                saved.getCondition().name()
        );

        response.put(
                "lastObservedAt",
                saved.getLastObservedAt()
        );

        response.put(
                "source",
                saved.getSource().name()
        );

        response.put(
                "notes",
                saved.getNotes()
        );

        response.put(
                "streetSegmentId",
                saved.getStreetSegment().getId()
        );

        return response;
    }
}