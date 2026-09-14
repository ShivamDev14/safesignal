package com.safesignal.report.controller;

import com.safesignal.report.dto.CreateSafetyReportRequest;
import com.safesignal.report.dto.SafetyReportResponse;
import com.safesignal.report.service.SafetyReportService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@Profile("mysql")
@RequestMapping("/api/safety-reports")
public class SafetyReportController {

    private final SafetyReportService safetyReportService;

    public SafetyReportController(SafetyReportService safetyReportService) {
        this.safetyReportService = safetyReportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SafetyReportResponse createReport(@Valid @RequestBody CreateSafetyReportRequest request) {
        return safetyReportService.createReport(request);
    }

    @GetMapping
    public List<SafetyReportResponse> getReports(
            @RequestParam(required = false) Long segmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return safetyReportService.getReports(segmentId, from, to);
    }
}
