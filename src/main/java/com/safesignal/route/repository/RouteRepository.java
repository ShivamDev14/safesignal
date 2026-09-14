package com.safesignal.route.repository;

import com.safesignal.route.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findByOriginIdAndDestinationIdAndActiveTrue(
            Long originId,
            Long destinationId
    );
}