package com.safesignal.safety.service;

import com.safesignal.report.entity.SafetyReport;
import com.safesignal.report.model.ReportStatus;
import com.safesignal.report.repository.SafetyReportRepository;
import com.safesignal.safety.dto.RelevantReportResponse;
import com.safesignal.safety.dto.SafetySummaryResponse;
import com.safesignal.safety.dto.StreetlightSummaryResponse;
import com.safesignal.safety.exception.StreetSegmentNotFoundException;
import com.safesignal.safety.model.SafetyCategory;
import com.safesignal.streetlight.entity.StreetSegment;
import com.safesignal.streetlight.entity.Streetlight;
import com.safesignal.streetlight.model.StreetlightCondition;
import com.safesignal.streetlight.repository.StreetSegmentRepository;
import com.safesignal.streetlight.repository.StreetlightRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Profile({"mysql", "default", "h2", "tidb"})
public class SafetyAnalysisService {

    private static final String DISCLAIMER = "This is an illustrative prototype score based on community reports and streetlight data. It does not predict crime or guarantee safety.";

    private final StreetSegmentRepository streetSegmentRepository;
    private final SafetyReportRepository safetyReportRepository;
    private final StreetlightRepository streetlightRepository;

    public SafetyAnalysisService(StreetSegmentRepository streetSegmentRepository,
                                 SafetyReportRepository safetyReportRepository,
                                 StreetlightRepository streetlightRepository) {
        this.streetSegmentRepository = streetSegmentRepository;
        this.safetyReportRepository = safetyReportRepository;
        this.streetlightRepository = streetlightRepository;
    }

    public SafetySummaryResponse analyze(Long segmentId, LocalDateTime evaluatedAt) {
        StreetSegment segment = streetSegmentRepository.findById(segmentId)
                .orElseThrow(() -> new StreetSegmentNotFoundException(segmentId));

        List<SafetyReport> reports = safetyReportRepository
                .findByStreetSegmentIdAndStatus(segmentId, ReportStatus.APPROVED);
        List<RelevantReportResponse> relevantReports = reports.stream()
                .filter(report -> !report.getOccurredAt().isAfter(evaluatedAt))
                .map(report -> toRelevantReport(report, evaluatedAt))
                .filter(report -> report.scoreContribution() > 0)
                .toList();

        int reportScore = relevantReports.stream().mapToInt(RelevantReportResponse::scoreContribution).sum();
        StreetlightSummaryResponse streetlightSummary = summarizeStreetlights(
                streetlightRepository.findByStreetSegmentId(segmentId));
        int safetyScore = Math.clamp(reportScore + streetlightSummary.scoreAdjustment(), 0, 100);

        List<String> reasons = buildReasons(relevantReports, streetlightSummary);
        return new SafetySummaryResponse(
                segmentId,
                segment.getName(),
                evaluatedAt,
                safetyScore,
                categoryFor(safetyScore),
                relevantReports.size(),
                relevantReports,
                streetlightSummary,
                reasons,
                DISCLAIMER
        );
    }

    private RelevantReportResponse toRelevantReport(SafetyReport report, LocalDateTime evaluatedAt) {
        long minutesFromRequestedTime = circularMinutesBetween(report.getOccurredAt(), evaluatedAt);
        long daysOld = ChronoUnit.DAYS.between(report.getOccurredAt().toLocalDate(), evaluatedAt.toLocalDate());
        double contribution = report.getSeverity() * 5.0 * timeRelevance(minutesFromRequestedTime) * recencyWeight(daysOld);
        return new RelevantReportResponse(
                report.getIncidentType(), report.getSeverity(), report.getOccurredAt(),
                minutesFromRequestedTime, daysOld, (int) Math.round(contribution));
    }

    private StreetlightSummaryResponse summarizeStreetlights(List<Streetlight> streetlights) {
        int working = 0;
        int dim = 0;
        int notWorking = 0;
        int unknown = 0;

        for (Streetlight streetlight : streetlights) {
            if (streetlight.getCondition() == StreetlightCondition.WORKING) working++;
            else if (streetlight.getCondition() == StreetlightCondition.DIM) dim++;
            else if (streetlight.getCondition() == StreetlightCondition.NOT_WORKING) notWorking++;
            else unknown++;
        }

        int adjustment = (working * -2) + (dim * 6) + (notWorking * 15) + (unknown * 3);
        return new StreetlightSummaryResponse(streetlights.size(), working, dim, notWorking, unknown, adjustment);
    }

    private List<String> buildReasons(List<RelevantReportResponse> reports, StreetlightSummaryResponse lights) {
        List<String> reasons = new ArrayList<>();
        if (reports.isEmpty()) {
            reasons.add("No approved reports were relevant to the requested travel time.");
        } else {
            reasons.add(reports.size() + " report" + plural(reports.size()) + " were relevant to the requested travel time.");
            reports.forEach(report -> reasons.add("A severity-" + report.severity() + " " + report.incidentType()
                    + " report occurred " + report.minutesFromRequestedTime() + " minutes from the requested travel time."));
        }
        if (lights.totalLights() == 0) reasons.add("No streetlight data is available for this street segment.");
        if (lights.workingLights() > 0) reasons.add(lights.workingLights() + " streetlight" + plural(lights.workingLights()) + " are working.");
        if (lights.dimLights() > 0) reasons.add(lights.dimLights() + " streetlight" + plural(lights.dimLights()) + " are dim.");
        if (lights.notWorkingLights() > 0) reasons.add(lights.notWorkingLights() + " streetlight" + plural(lights.notWorkingLights()) + " are not working.");
        if (lights.unknownLights() > 0) reasons.add(lights.unknownLights() + " streetlight" + plural(lights.unknownLights()) + " have unknown conditions.");
        reasons.add("Higher safety scores indicate higher reported concern in this prototype.");
        return reasons;
    }

    private String plural(int count) {
        return count == 1 ? "" : "s";
    }

    private long circularMinutesBetween(LocalDateTime occurredAt, LocalDateTime evaluatedAt) {
        long difference = Math.abs(ChronoUnit.MINUTES.between(occurredAt.toLocalTime(), evaluatedAt.toLocalTime()));
        return Math.min(difference, (24 * 60) - difference);
    }

    private double timeRelevance(long minutesFromRequestedTime) {
        if (minutesFromRequestedTime <= 60) return 1.0;
        if (minutesFromRequestedTime <= 120) return 0.75;
        if (minutesFromRequestedTime <= 180) return 0.5;
        if (minutesFromRequestedTime <= 360) return 0.25;
        return 0.0;
    }

    private double recencyWeight(long daysOld) {
        if (daysOld <= 7) return 1.0;
        if (daysOld <= 30) return 0.8;
        if (daysOld <= 90) return 0.6;
        return 0.4;
    }

    private SafetyCategory categoryFor(int safetyScore) {
        if (safetyScore >= 50) return SafetyCategory.HIGHER_REPORTED_CONCERN;
        if (safetyScore >= 20) return SafetyCategory.MODERATE_REPORTED_CONCERN;
        return SafetyCategory.LOWER_REPORTED_CONCERN;
    }
}
