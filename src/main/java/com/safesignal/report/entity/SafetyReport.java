package com.safesignal.report.entity;

import com.safesignal.report.model.IncidentType;
import com.safesignal.report.model.ReportStatus;
import com.safesignal.streetlight.entity.StreetSegment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "safety_reports")
public class SafetyReport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "street_segment_id", nullable = false)
    private StreetSegment streetSegment;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private IncidentType incidentType;
    @NotNull @Min(1) @Max(5) @Column(nullable = false)
    private Integer severity;
    @NotNull @Column(nullable = false)
    private LocalDateTime occurredAt;
    @NotNull @Column(nullable = false, updatable = false)
    private LocalDateTime reportedAt;
    @Size(max = 1000) @Column(length = 1000)
    private String description;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ReportStatus status;
    @NotNull @Column(nullable = false, unique = true, length = 64)
    private String anonymousReportToken;

    protected SafetyReport() { }
    public SafetyReport(StreetSegment streetSegment, IncidentType incidentType, Integer severity, LocalDateTime occurredAt,
                        LocalDateTime reportedAt, String description, ReportStatus status, String anonymousReportToken) {
        this.streetSegment = streetSegment; this.incidentType = incidentType; this.occurredAt = occurredAt;
        this.severity = severity;
        this.reportedAt = reportedAt; this.description = description; this.status = status;
        this.anonymousReportToken = anonymousReportToken;
    }
    @PrePersist
    void assignReportedAtWhenMissing() {
        if (reportedAt == null) reportedAt = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public StreetSegment getStreetSegment() { return streetSegment; }
    public IncidentType getIncidentType() { return incidentType; }
    public Integer getSeverity() { return severity; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public String getDescription() { return description; }
    public ReportStatus getStatus() { return status; }
}
