package com.safesignal.safety.map.controller;

import com.safesignal.safety.map.dto.MapOverlayResponse;
import com.safesignal.safety.map.service.MapOverlayService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/map-overlays")
public class MapOverlayController {

    private final MapOverlayService mapOverlayService;

    public MapOverlayController(MapOverlayService mapOverlayService) {
        this.mapOverlayService = mapOverlayService;
    }

    @GetMapping
    public MapOverlayResponse getOverlay(
            @RequestParam Long originId,
            @RequestParam Long destinationId,
            @RequestParam LocalDateTime at
    ) {
        return mapOverlayService.getOverlay(
                originId,
                destinationId,
                at
        );
    }
}