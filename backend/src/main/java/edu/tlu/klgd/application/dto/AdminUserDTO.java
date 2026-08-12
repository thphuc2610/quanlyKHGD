package edu.tlu.klgd.application.dto;

import edu.tlu.klgd.domain.entity.UserRole;
import java.util.Set;

public record AdminUserDTO(
    Long id,
    String username,
    String fullName,
    String teacherName,
    String email,
    String phone,
    boolean enabled,
    Set<UserRole> roles
) {
}
