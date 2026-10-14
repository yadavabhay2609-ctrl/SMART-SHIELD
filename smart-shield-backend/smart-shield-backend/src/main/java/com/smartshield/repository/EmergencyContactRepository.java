package com.smartshield.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartshield.model.EmergencyContact;

@Repository
public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {

    List<EmergencyContact> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<EmergencyContact> findByUserIdAndActiveTrue(Long userId);

    Optional<EmergencyContact> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndPhoneNumber(Long userId, String phoneNumber);

    Optional<EmergencyContact> findByTelegramConnectionCode(String code);

    List<EmergencyContact> findByUserIdAndActiveTrueAndTelegramConnectedTrue(Long userId);
}
