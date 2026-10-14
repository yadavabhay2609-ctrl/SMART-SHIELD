package com.smartshield.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartshield.model.GrievanceStatusHistory;

public interface GrievanceStatusHistoryRepository extends JpaRepository<GrievanceStatusHistory, Long> {

    List<GrievanceStatusHistory> findByGrievanceIdOrderByChangedAtAsc(Long grievanceId);
}
