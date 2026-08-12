package edu.tlu.klgd.application.dto;

import edu.tlu.klgd.domain.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UpdateUserRequestDTO(
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
