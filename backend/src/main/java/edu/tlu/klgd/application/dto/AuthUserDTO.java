package edu.tlu.klgd.application.dto;

import edu.tlu.klgd.domain.entity.UserRole;
import java.util.Set;

public record AuthUserDTO(
    Long id,
    String username,
    String fullName,
    String teacherName,
    String email,
    String phone,
    String avatarUrl,
    Set<UserRole> roles
) {
}
