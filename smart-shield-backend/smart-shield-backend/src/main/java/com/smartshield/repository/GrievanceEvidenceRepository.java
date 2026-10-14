package com.smartshield.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartshield.model.GrievanceEvidence;

public interface GrievanceEvidenceRepository extends JpaRepository<GrievanceEvidence, Long> {

    List<GrievanceEvidence> findByGrievanceId(Long grievanceId);

    List<GrievanceEvidence> findByGrievanceIdOrderByUploadedAtAsc(Long grievanceId);
}
