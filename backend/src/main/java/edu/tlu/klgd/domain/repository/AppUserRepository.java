package edu.tlu.klgd.domain.repository;

import edu.tlu.klgd.domain.entity.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findByPasswordResetToken(String passwordResetToken);
    boolean existsByUsername(String username);
}
