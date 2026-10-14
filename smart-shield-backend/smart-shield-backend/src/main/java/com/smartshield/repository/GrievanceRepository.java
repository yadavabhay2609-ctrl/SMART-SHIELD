package com.smartshield.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartshield.model.Grievance;

public interface GrievanceRepository extends JpaRepository<Grievance, Long> {

    Optional<Grievance> findByTrackingId(String trackingId);

    boolean existsByTrackingId(String trackingId);

    List<Grievance> findByCitizenId(Long citizenId);

    List<Grievance> findByCitizenIdOrderByCreatedAtDesc(Long citizenId);

    List<Grievance> findAllByOrderByCreatedAtDesc();
}
