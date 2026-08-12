package edu.tlu.klgd.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequestDTO(
    @NotBlank String resetToken,
    @NotBlank String newPassword,
    @NotBlank String confirmPassword
) {
}
