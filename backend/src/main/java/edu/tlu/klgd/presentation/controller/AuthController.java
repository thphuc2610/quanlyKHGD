package edu.tlu.klgd.presentation.controller;

import edu.tlu.klgd.application.dto.*;
import edu.tlu.klgd.domain.common.ApiMessage;
import edu.tlu.klgd.domain.common.ApiURL;
import edu.tlu.klgd.domain.common.MessageResponse;
import edu.tlu.klgd.domain.service.AuthService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiURL.AUTH)
@CrossOrigin
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(ApiURL.LOGIN)
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping(ApiURL.ME)
    public ResponseEntity<AuthUserDTO> me(Principal principal) {
        return ResponseEntity.ok(authService.currentUser(principal.getName()));
    }

    @PutMapping(ApiURL.ME)
    public ResponseEntity<AuthUserDTO> updateProfile(
        Principal principal,
        @Valid @RequestBody UpdateProfileRequestDTO request
    ) {
        return ResponseEntity.ok(authService.updateProfile(principal.getName(), request));
    }

    @PostMapping(ApiURL.CHANGE_PASSWORD)
    public ResponseEntity<MessageResponse> changePassword(
        Principal principal,
        @Valid @RequestBody ChangePasswordRequestDTO request
    ) {
        authService.changePassword(principal.getName(), request);
        return ResponseEntity.ok(new MessageResponse(HttpStatus.OK.value(), ApiMessage.PASSWORD_CHANGE_SUCCESS, ApiURL.AUTH_CHANGE_PASSWORD));
    }

    @PostMapping(ApiURL.FORGOT_PASSWORD)
    public ResponseEntity<ForgotPasswordResponseDTO> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequestDTO request
    ) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping(ApiURL.RESET_PASSWORD)
    public ResponseEntity<MessageResponse> resetPassword(
        @Valid @RequestBody ResetPasswordRequestDTO request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse(HttpStatus.OK.value(), ApiMessage.PASSWORD_RESET_SUCCESS, ApiURL.AUTH_RESET_PASSWORD));
    }
}
