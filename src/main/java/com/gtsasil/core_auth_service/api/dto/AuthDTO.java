package com.gtsasil.core_auth_service.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthDTO(
        @NotBlank @Email String email,
        @NotBlank String password

) {

}
