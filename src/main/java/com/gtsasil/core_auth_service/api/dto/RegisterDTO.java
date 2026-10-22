package com.gtsasil.core_auth_service.api.dto;

import com.gtsasil.core_auth_service.domain.user.enums.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterDTO(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotNull UserRole role) {
}
