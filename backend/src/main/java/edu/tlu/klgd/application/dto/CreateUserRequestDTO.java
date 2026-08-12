package edu.tlu.klgd.application.dto;

import edu.tlu.klgd.domain.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record CreateUserRequestDTO(
    @NotBlank
    @Size(max = 100)
    String username,

    @NotBlank
    @Size(min = 6, max = 100)
    String password,

    @NotBlank
    @Size(max = 255)
    String fullName,

    @Size(max = 255)
    String teacherName,

    @NotEmpty
    Set<UserRole> roles,

    boolean enabled
) {
}
