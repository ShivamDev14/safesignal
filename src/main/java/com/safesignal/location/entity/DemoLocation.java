package com.safesignal.location.entity;

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

import java.math.BigDecimal;

@Entity
@Table(name = "demo_locations")
public class DemoLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String areaName;

    @Column(nullable = false)
    private boolean active = true;

    protected DemoLocation() { }

    public DemoLocation(String name, BigDecimal latitude, BigDecimal longitude, String areaName, boolean active) {
        this.name = name; this.latitude = latitude; this.longitude = longitude; this.areaName = areaName; this.active = active;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public String getAreaName() { return areaName; }
    public boolean isActive() { return active; }
}
