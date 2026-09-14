package com.safesignal.streetlight.repository;

import com.safesignal.streetlight.entity.StreetSegment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreetSegmentRepository extends JpaRepository<StreetSegment, Long> {
}
