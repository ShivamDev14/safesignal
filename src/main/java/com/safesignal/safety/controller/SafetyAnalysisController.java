package com.safesignal.safety.controller;

import com.safesignal.safety.dto.SafetySummaryResponse;
import com.safesignal.safety.service.SafetyAnalysisService;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@Profile("mysql")
@RequestMapping("/api/street-segments")
public class SafetyAnalysisController {

    private final SafetyAnalysisService safetyAnalysisService;

    public SafetyAnalysisController(SafetyAnalysisService safetyAnalysisService) {
        this.safetyAnalysisService = safetyAnalysisService;
    }

    @GetMapping("/{segmentId}/safety-summary")
    public SafetySummaryResponse getSafetySummary(
            @PathVariable Long segmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at) {
        return safetyAnalysisService.analyze(segmentId, at);
    }
}
