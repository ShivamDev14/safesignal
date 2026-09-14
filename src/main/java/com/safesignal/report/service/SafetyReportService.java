package com.safesignal.report.service;

import com.safesignal.report.dto.CreateSafetyReportRequest;
import com.safesignal.report.dto.SafetyReportResponse;
import com.safesignal.report.entity.SafetyReport;
import com.safesignal.report.exception.SafetyReportNotFoundException;
import com.safesignal.report.model.ReportStatus;
import com.safesignal.report.repository.SafetyReportRepository;
import com.safesignal.streetlight.entity.StreetSegment;
import com.safesignal.streetlight.repository.StreetSegmentRepository;
import jakarta.transaction.Transactional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Profile("mysql")
public class SafetyReportService {

    private final SafetyReportRepository safetyReportRepository;
    private final StreetSegmentRepository streetSegmentRepository;

    public SafetyReportService(SafetyReportRepository safetyReportRepository, StreetSegmentRepository streetSegmentRepository) {
        this.safetyReportRepository = safetyReportRepository;
        this.streetSegmentRepository = streetSegmentRepository;
    }

    @Transactional
    public SafetyReportResponse createReport(CreateSafetyReportRequest request) {
        StreetSegment streetSegment = streetSegmentRepository.findById(request.streetSegmentId())
                .orElseThrow(() -> new SafetyReportNotFoundException(request.streetSegmentId()));

        SafetyReport report = new SafetyReport(
                streetSegment,
                request.incidentType(),
                request.severity(),
                request.occurredAt(),
                LocalDateTime.now(),
                request.description().trim(),
                ReportStatus.PENDING,
                UUID.randomUUID().toString()
        );

        return toResponse(safetyReportRepository.save(report));
    }

    @Transactional
    public List<SafetyReportResponse> getReports(Long segmentId, LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }

        return safetyReportRepository.findAll().stream()
                .filter(report -> segmentId == null || report.getStreetSegment().getId().equals(segmentId))
                .filter(report -> from == null || !report.getOccurredAt().isBefore(from))
                .filter(report -> to == null || !report.getOccurredAt().isAfter(to))
                .map(this::toResponse)
                .toList();
    }

    private SafetyReportResponse toResponse(SafetyReport report) {
        return new SafetyReportResponse(
                report.getId(),
                report.getStreetSegment().getId(),
                report.getIncidentType(),
                report.getSeverity(),
                report.getOccurredAt(),
                report.getReportedAt(),
                report.getDescription(),
                report.getStatus()
        );
    }
}
