package com.safesignal.streetlight.controller;

import com.safesignal.streetlight.entity.Streetlight;
import com.safesignal.streetlight.repository.StreetlightRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Profile("mysql")
@RequestMapping("/api/streetlights")
public class StreetlightController {

    private final StreetlightRepository streetlightRepository;

    public StreetlightController(
            StreetlightRepository streetlightRepository) {

        this.streetlightRepository =
                streetlightRepository;
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
}