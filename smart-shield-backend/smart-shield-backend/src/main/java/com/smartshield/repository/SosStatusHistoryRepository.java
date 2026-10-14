package com.smartshield.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartshield.model.SosStatusHistory;

@Repository
public interface SosStatusHistoryRepository extends JpaRepository<SosStatusHistory, Long> {

    List<SosStatusHistory> findBySosAlertIdOrderByChangedAtAsc(Long sosAlertId);
}
