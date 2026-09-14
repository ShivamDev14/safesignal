package com.safesignal.route.entity;

import com.safesignal.common.model.DataSource;
import com.safesignal.location.entity.DemoLocation;
import jakarta.persistence.*;

@Entity
@Table(name = "routes")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_id", nullable = false)
    private DemoLocation origin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_id", nullable = false)
    private DemoLocation destination;

    @Column(nullable = false)
    private Integer estimatedDurationMinutes;

    @Column(nullable = false)
    private Integer distanceMeters;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DataSource dataSource;

    @Column(nullable = false)
    private boolean active = true;

    protected Route() {
    }

    public Route(String name,
                 DemoLocation origin,
                 DemoLocation destination,
                 Integer estimatedDurationMinutes,
                 Integer distanceMeters,
                 DataSource dataSource,
                 boolean active) {

        this.name = name;
        this.origin = origin;
        this.destination = destination;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.distanceMeters = distanceMeters;
        this.dataSource = dataSource;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DemoLocation getOrigin() {
        return origin;
    }

    public DemoLocation getDestination() {
        return destination;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public Integer getDistanceMeters() {
        return distanceMeters;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public boolean isActive() {
        return active;
    }
}