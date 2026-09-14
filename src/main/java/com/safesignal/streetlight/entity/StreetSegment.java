package com.safesignal.streetlight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Entity
@Table(name = "street_segments")
public class StreetSegment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false, length = 120)
    private String name;
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal startLatitude;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal startLongitude;
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal endLatitude;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal endLongitude;
    @NotNull @Positive @Column(nullable = false)
    private Integer lengthMeters;
    @NotBlank @Column(nullable = false, length = 120)
    private String areaName;

    protected StreetSegment() { }
    public StreetSegment(String name, BigDecimal startLatitude, BigDecimal startLongitude, BigDecimal endLatitude,
                         BigDecimal endLongitude, Integer lengthMeters, String areaName) {
        this.name = name; this.startLatitude = startLatitude; this.startLongitude = startLongitude;
        this.endLatitude = endLatitude; this.endLongitude = endLongitude; this.lengthMeters = lengthMeters; this.areaName = areaName;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getStartLatitude() { return startLatitude; }
    public BigDecimal getStartLongitude() { return startLongitude; }
    public BigDecimal getEndLatitude() { return endLatitude; }
    public BigDecimal getEndLongitude() { return endLongitude; }
    public Integer getLengthMeters() { return lengthMeters; }
    public String getAreaName() { return areaName; }
}
