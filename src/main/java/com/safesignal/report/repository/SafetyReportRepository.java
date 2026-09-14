package com.safesignal.report.repository;

import com.safesignal.report.entity.SafetyReport;
import com.safesignal.report.model.ReportStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SafetyReportRepository
        extends JpaRepository<SafetyReport, Long> {

    @EntityGraph(attributePaths = {"streetSegment"})
    List<SafetyReport> findByStreetSegmentIdAndStatus(
            Long streetSegmentId,
            ReportStatus status
    );

    @Override
    @EntityGraph(attributePaths = {"streetSegment"})
    List<SafetyReport> findAll();
}