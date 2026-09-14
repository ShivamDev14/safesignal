package com.safesignal.route.entity;

import com.safesignal.streetlight.entity.StreetSegment;
import jakarta.persistence.*;

@Entity
@Table(name = "route_segments")
public class RouteSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "street_segment_id", nullable = false)
    private StreetSegment streetSegment;

    @Column(nullable = false)
    private Integer sequenceNumber;

    @Column(nullable = false)
    private Integer estimatedTravelSeconds;

    protected RouteSegment() {
    }

    public RouteSegment(
            Route route,
            StreetSegment streetSegment,
            Integer sequenceNumber,
            Integer estimatedTravelSeconds) {

        this.route = route;
        this.streetSegment = streetSegment;
        this.sequenceNumber = sequenceNumber;
        this.estimatedTravelSeconds = estimatedTravelSeconds;
    }

    public Long getId() {
        return id;
    }

    public Route getRoute() {
        return route;
    }

    public StreetSegment getStreetSegment() {
        return streetSegment;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public Integer getEstimatedTravelSeconds() {
        return estimatedTravelSeconds;
    }
}