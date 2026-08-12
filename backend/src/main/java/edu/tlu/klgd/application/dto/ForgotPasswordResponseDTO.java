package edu.tlu.klgd.application.dto;

import java.time.Instant;

public record ForgotPasswordResponseDTO(
    String message,
    String resetToken,
    Instant expiresAt
) {
}
