package com.safesignal.route.repository;

import com.safesignal.route.entity.RouteSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteSegmentRepository extends JpaRepository<RouteSegment, Long> {

    List<RouteSegment> findByRouteIdOrderBySequenceNumberAsc(Long routeId);
}