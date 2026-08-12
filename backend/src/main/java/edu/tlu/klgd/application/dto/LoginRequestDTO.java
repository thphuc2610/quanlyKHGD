package edu.tlu.klgd.application.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
    @NotBlank String username,
    @NotBlank String password
) {
}
