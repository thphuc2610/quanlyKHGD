package edu.tlu.klgd.domain.service;

import edu.tlu.klgd.application.dto.*;

public interface AuthService {
    LoginResponseDTO login(LoginRequestDTO request);
    AuthUserDTO currentUser(String username);
    AuthUserDTO updateProfile(String username, UpdateProfileRequestDTO request);
    void changePassword(String username, ChangePasswordRequestDTO request);
    java.util.List<AdminUserDTO> users();
    AdminUserDTO createUser(CreateUserRequestDTO request);
    AdminUserDTO updateUser(Long id, UpdateUserRequestDTO request);
    ForgotPasswordResponseDTO forgotPassword(ForgotPasswordRequestDTO request);
    void resetPassword(ResetPasswordRequestDTO request);
}
