package edu.tlu.klgd.infracstructure.security;

import edu.tlu.klgd.application.dto.*;
import edu.tlu.klgd.domain.common.ApiMessage;
import edu.tlu.klgd.domain.common.util.TextNormalizer;
import edu.tlu.klgd.domain.entity.AppUser;
import edu.tlu.klgd.domain.entity.UserRole;
import edu.tlu.klgd.domain.repository.AppUserRepository;
import edu.tlu.klgd.domain.service.AuthService;
import edu.tlu.klgd.infracstructure.exception.BadRequestException;
import edu.tlu.klgd.infracstructure.exception.ConflictException;
import edu.tlu.klgd.infracstructure.exception.ResourceNotFoundException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {
    private static final int RESET_TOKEN_BOUND = 1_000_000;
    private static final String RESET_TOKEN_FORMAT = "%06d";
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(15);
    private final SecureRandom secureRandom = new SecureRandom();
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO request) {
        AppUser user = appUserRepository.findByUsername(request.username())
            .filter(AppUser::isEnabled)
            .orElseThrow(() -> new BadCredentialsException(ApiMessage.INVALID_USERNAME_OR_PASSWORD));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException(ApiMessage.INVALID_USERNAME_OR_PASSWORD);
        }
        return new LoginResponseDTO(jwtService.generate(user), SecurityConstant.BEARER_TOKEN_TYPE, jwtService.expirationSeconds(), toDTO(user));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthUserDTO currentUser(String username) {
        AppUser user = appUserRepository.findByUsername(username)
            .orElseThrow(() -> new BadCredentialsException(ApiMessage.USER_NOT_FOUND));
        return toDTO(user);
    }

    @Override
    @Transactional
    public AuthUserDTO updateProfile(String username, UpdateProfileRequestDTO request) {
        AppUser user = appUserRepository.findByUsername(username)
            .orElseThrow(() -> new BadCredentialsException(ApiMessage.USER_NOT_FOUND));
        user.setFullName(normalizeRequired(request.fullName()));
        user.setEmail(normalizeOptional(request.email()));
        user.setPhone(normalizeOptional(request.phone()));
        user.setAvatarUrl(normalizeOptional(request.avatarUrl()));
        return toDTO(appUserRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequestDTO request) {
        AppUser user = appUserRepository.findByUsername(username)
            .orElseThrow(() -> new BadCredentialsException(ApiMessage.USER_NOT_FOUND));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException(ApiMessage.CURRENT_PASSWORD_INVALID);
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException(ApiMessage.PASSWORD_CONFIRM_NOT_MATCHED);
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserDTO> users() {
        return appUserRepository.findAll().stream()
            .sorted(Comparator.comparing(AppUser::getUsername, String.CASE_INSENSITIVE_ORDER))
            .map(AuthServiceImpl::toAdminDTO)
            .toList();
    }

    @Override
    @Transactional
    public AdminUserDTO createUser(CreateUserRequestDTO request) {
        String username = normalizeRequired(request.username());
        if (appUserRepository.existsByUsername(username)) {
            throw new ConflictException(ApiMessage.USERNAME_ALREADY_EXISTS);
        }
        validateTeacherLink(request.roles(), request.teacherName());
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(normalizeRequired(request.fullName()));
        user.setTeacherName(normalizeTeacherName(request.teacherName()));
        user.setRoles(request.roles());
        user.setEnabled(request.enabled());
        return toAdminDTO(appUserRepository.save(user));
    }

    @Override
    @Transactional
    public AdminUserDTO updateUser(Long id, UpdateUserRequestDTO request) {
        validateTeacherLink(request.roles(), request.teacherName());
        AppUser user = appUserRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ApiMessage.RESOURCE_USER, id));
        user.setFullName(normalizeRequired(request.fullName()));
        user.setTeacherName(normalizeTeacherName(request.teacherName()));
        user.setRoles(request.roles());
        user.setEnabled(request.enabled());
        return toAdminDTO(appUserRepository.save(user));
    }

    @Override
    @Transactional
    public ForgotPasswordResponseDTO forgotPassword(ForgotPasswordRequestDTO request) {
        AppUser user = appUserRepository.findByUsername(request.username())
            .filter(AppUser::isEnabled)
            .orElseThrow(() -> new BadCredentialsException(ApiMessage.USER_NOT_FOUND));
        String resetToken = generateResetToken();
        Instant expiresAt = Instant.now().plus(RESET_TOKEN_TTL);
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetExpiresAt(expiresAt);
        appUserRepository.save(user);
        return new ForgotPasswordResponseDTO(ApiMessage.PASSWORD_RESET_TOKEN_CREATED, resetToken, expiresAt);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        AppUser user = appUserRepository.findByPasswordResetToken(request.resetToken())
            .orElseThrow(() -> new BadRequestException(ApiMessage.PASSWORD_RESET_TOKEN_INVALID));
        if (user.getPasswordResetExpiresAt() == null || user.getPasswordResetExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException(ApiMessage.PASSWORD_RESET_TOKEN_EXPIRED);
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException(ApiMessage.PASSWORD_CONFIRM_NOT_MATCHED);
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        appUserRepository.save(user);
    }

    private String generateResetToken() {
        return String.format(RESET_TOKEN_FORMAT, secureRandom.nextInt(RESET_TOKEN_BOUND));
    }

    private static String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeTeacherName(String value) {
        String teacherName = TextNormalizer.cleanTeacherName(value);
        return teacherName.isBlank() ? null : teacherName;
    }

    private static AuthUserDTO toDTO(AppUser user) {
        return new AuthUserDTO(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            normalizeTeacherName(user.getTeacherName()),
            user.getEmail(),
            user.getPhone(),
            user.getAvatarUrl(),
            user.getRoles()
        );
    }

    private static AdminUserDTO toAdminDTO(AppUser user) {
        return new AdminUserDTO(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            normalizeTeacherName(user.getTeacherName()),
            user.getEmail(),
            user.getPhone(),
            user.isEnabled(),
            user.getRoles()
        );
    }

    private static void validateTeacherLink(java.util.Set<UserRole> roles, String teacherName) {
        if (roles != null && roles.contains(UserRole.GIANG_VIEN) && normalizeTeacherName(teacherName) == null) {
            throw new BadRequestException(ApiMessage.TEACHER_ACCOUNT_REQUIRES_LINK);
        }
    }
}
