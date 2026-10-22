package com.gtsasil.core_auth_service.domain.user.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import com.gtsasil.core_auth_service.domain.user.entity.AppUser;

public interface UserRepository extends JpaRepository<AppUser, UUID> {
    UserDetails findByEmail(String email);
}
