package com.safesignal.streetlight.controller;

import com.safesignal.streetlight.entity.StreetSegment;
import com.safesignal.streetlight.repository.StreetSegmentRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@Profile("mysql")
@RequestMapping("/api/street-segments")
public class StreetSegmentController {

    private final StreetSegmentRepository streetSegmentRepository;

    public StreetSegmentController(
            StreetSegmentRepository streetSegmentRepository) {
        this.streetSegmentRepository = streetSegmentRepository;
    }

    @PostMapping("/resolve")
    public ResponseEntity<Map<String, Object>> resolveStreetSegment(
            @RequestBody Map<String, Object> request) {

        double latitude =
                ((Number) request.get("latitude")).doubleValue();

        double longitude =
                ((Number) request.get("longitude")).doubleValue();

        String streetName =
                String.valueOf(request.get("streetName"));

        String areaName =
                String.valueOf(request.get("areaName"));

        StreetSegment segment = streetSegmentRepository
                .findAll()
                .stream()
                .filter(existing ->
                        existing.getName().equalsIgnoreCase(streetName)
                                && existing.getAreaName().equalsIgnoreCase(areaName))
                .findFirst()
                .orElseGet(() -> {

                    StreetSegment newSegment = new StreetSegment(
                            streetName,
                            BigDecimal.valueOf(latitude),
                            BigDecimal.valueOf(longitude),
                            BigDecimal.valueOf(latitude),
                            BigDecimal.valueOf(longitude),
                            1,
                            areaName
                    );

                    return streetSegmentRepository.save(newSegment);
                });

        return ResponseEntity.ok(Map.of(
                "id", segment.getId(),
                "name", segment.getName(),
                "areaName", segment.getAreaName()
        ));
    }
}