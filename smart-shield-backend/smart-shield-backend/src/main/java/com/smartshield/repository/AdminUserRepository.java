package com.smartshield.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartshield.model.AdminUser;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByAdminId(String adminId);

    boolean existsByAdminId(String adminId);
}
