package com.safesignal.location.repository;

import com.safesignal.location.entity.DemoLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemoLocationRepository extends JpaRepository<DemoLocation, Long> {
}
