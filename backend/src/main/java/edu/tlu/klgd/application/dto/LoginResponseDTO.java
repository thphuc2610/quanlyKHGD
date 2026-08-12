package edu.tlu.klgd.application.dto;

public record LoginResponseDTO(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    AuthUserDTO user
) {
}
