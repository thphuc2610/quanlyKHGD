package edu.tlu.klgd.infracstructure.security;

import edu.tlu.klgd.domain.common.AuthSeedConstant;
import edu.tlu.klgd.domain.entity.*;
import edu.tlu.klgd.domain.repository.AppUserRepository;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AuthDataInitializer implements CommandLineRunner {
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthDataInitializer(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createUserIfMissing(
            AuthSeedConstant.ADMIN_USERNAME,
            AuthSeedConstant.ADMIN_PASSWORD,
            AuthSeedConstant.ADMIN_FULL_NAME,
            null,
            UserRole.ADMIN
        );
        createUserIfMissing(
            AuthSeedConstant.TEACHER_USERNAME,
            AuthSeedConstant.TEACHER_PASSWORD,
            AuthSeedConstant.TEACHER_FULL_NAME,
            AuthSeedConstant.TEACHER_NAME,
            UserRole.GIANG_VIEN
        );
    }

    private void createUserIfMissing(String username, String password, String fullName, String teacherName, UserRole role) {
        if (appUserRepository.existsByUsername(username)) {
            return;
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setTeacherName(teacherName);
        user.setRoles(Set.of(role));
        appUserRepository.save(user);
    }
}
