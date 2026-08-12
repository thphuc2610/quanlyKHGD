package edu.tlu.klgd.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequestDTO(
    @NotBlank String username
) {
}
