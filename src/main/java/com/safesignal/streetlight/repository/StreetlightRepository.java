package com.safesignal.streetlight.repository;

import com.safesignal.streetlight.entity.Streetlight;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StreetlightRepository
        extends JpaRepository<Streetlight, Long> {

    @EntityGraph(attributePaths = {"streetSegment"})
    List<Streetlight> findByStreetSegmentId(
            Long streetSegmentId
    );

    @Override
    @EntityGraph(attributePaths = {"streetSegment"})
    List<Streetlight> findAll();
}
