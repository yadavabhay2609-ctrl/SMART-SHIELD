package com.smartshield.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartshield.model.SosAlert;

@Repository
public interface SosAlertRepository extends JpaRepository<SosAlert, Long> {

    List<SosAlert> findByCitizenIdOrderByTriggeredAtDesc(Long citizenId);

    List<SosAlert> findAllByOrderByTriggeredAtDesc();
}
