package edu.tlu.klgd.presentation.controller;

import edu.tlu.klgd.application.dto.*;
import edu.tlu.klgd.domain.common.ApiURL;
import edu.tlu.klgd.domain.service.AuthService;
import edu.tlu.klgd.domain.service.WorkloadService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiURL.ADMIN)
@CrossOrigin
public class AdminUserController {
    private final AuthService authService;
    private final WorkloadService workloadService;

    public AdminUserController(AuthService authService, WorkloadService workloadService) {
        this.authService = authService;
        this.workloadService = workloadService;
    }

    @GetMapping(ApiURL.USERS)
    public ResponseEntity<?> users() {
        return ResponseEntity.ok(authService.users());
    }

    @PostMapping(ApiURL.USERS)
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createUser(request));
    }

    @PutMapping(ApiURL.USERS + ApiURL.ID)
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequestDTO request) {
        return ResponseEntity.ok(authService.updateUser(id, request));
    }

    @GetMapping(ApiURL.TEACHER_OPTIONS)
    public ResponseEntity<?> teacherOptions() {
        return ResponseEntity.ok(workloadService.teacherOptions());
    }
}
