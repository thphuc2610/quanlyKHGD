package edu.tlu.klgd.presentation.controller;

import edu.tlu.klgd.domain.common.ApiURL;
import edu.tlu.klgd.domain.service.WorkloadService;
import edu.tlu.klgd.infracstructure.security.SecurityConstant;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = SecurityConstant.ALL_ORIGINS, maxAge = SecurityConstant.CORS_MAX_AGE_SECONDS)
@RestController
@RequestMapping(ApiURL.DASHBOARD)
public class DashboardController {
    private final WorkloadService workloadService;

    public DashboardController(WorkloadService workloadService) {
        this.workloadService = workloadService;
    }

    @GetMapping(ApiURL.OVERVIEW)
    public ResponseEntity<?> overview(Principal principal) {
        return ResponseEntity.ok(workloadService.overview(principal.getName()));
    }
}
