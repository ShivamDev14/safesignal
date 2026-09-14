package com.safesignal.streetlight.entity;

import com.safesignal.common.model.DataSource;
import com.safesignal.streetlight.model.StreetlightCondition;
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
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "streetlights")
public class Streetlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "street_segment_id", nullable = false)
    private StreetSegment streetSegment;

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "light_condition", nullable = false, length = 20)
    private StreetlightCondition condition;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime lastObservedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DataSource source;

    @Size(max = 500)
    @Column(length = 500)
    private String notes;

    protected Streetlight() {
    }

    public Streetlight(
            StreetSegment streetSegment,
            BigDecimal latitude,
            BigDecimal longitude,
            StreetlightCondition condition,
            LocalDateTime lastObservedAt,
            DataSource source,
            String notes) {

        this.streetSegment = streetSegment;
        this.latitude = latitude;
        this.longitude = longitude;
        this.condition = condition;
        this.lastObservedAt = lastObservedAt;
        this.source = source;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public StreetSegment getStreetSegment() {
        return streetSegment;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public StreetlightCondition getCondition() {
        return condition;
    }

    public LocalDateTime getLastObservedAt() {
        return lastObservedAt;
    }

    public DataSource getSource() {
        return source;
    }

    public String getNotes() {
        return notes;
    }
}