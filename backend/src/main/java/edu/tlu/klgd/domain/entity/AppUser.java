package edu.tlu.klgd.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_user")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    @Column(name = "teacher_name")
    private String teacherName;
    @Column(name = "email")
    private String email;
    @Column(name = "phone")
    private String phone;
    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
    @Column(name = "password_reset_token")
    private String passwordResetToken;
    @Column(name = "password_reset_expires_at")
    private Instant passwordResetExpiresAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<UserRole> roles = new HashSet<>();

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getPasswordResetToken() { return passwordResetToken; }
    public void setPasswordResetToken(String passwordResetToken) { this.passwordResetToken = passwordResetToken; }
    public Instant getPasswordResetExpiresAt() { return passwordResetExpiresAt; }
    public void setPasswordResetExpiresAt(Instant passwordResetExpiresAt) { this.passwordResetExpiresAt = passwordResetExpiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Set<UserRole> getRoles() { return roles; }
    public void setRoles(Set<UserRole> roles) { this.roles = roles; }
}
