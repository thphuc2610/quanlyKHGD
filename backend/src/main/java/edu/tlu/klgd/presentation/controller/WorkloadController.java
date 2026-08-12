package edu.tlu.klgd.presentation.controller;

import edu.tlu.klgd.domain.common.ApiURL;
import edu.tlu.klgd.application.dto.CalculationSettingsDTO;
import edu.tlu.klgd.domain.service.WorkloadService;
import edu.tlu.klgd.infracstructure.security.SecurityConstant;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = SecurityConstant.ALL_ORIGINS, maxAge = SecurityConstant.CORS_MAX_AGE_SECONDS)
@RestController
@RequestMapping(ApiURL.WORKLOAD)
public class WorkloadController {
    private final WorkloadService workloadService;

    public WorkloadController(WorkloadService workloadService) {
        this.workloadService = workloadService;
    }

    @GetMapping(ApiURL.TEACHERS)
    public ResponseEntity<?> teachers(Principal principal) {
        return ResponseEntity.ok(workloadService.teachers(principal.getName()));
    }

    @GetMapping(ApiURL.TEACHER_DETAILS)
    public ResponseEntity<?> teacherDetails(Principal principal) {
        return ResponseEntity.ok(workloadService.teacherDetails(principal.getName()));
    }

    @GetMapping(ApiURL.DEPARTMENTS)
    public ResponseEntity<?> departments(Principal principal) {
        return ResponseEntity.ok(workloadService.departments(principal.getName()));
    }

    @GetMapping(ApiURL.RULES)
    public ResponseEntity<?> rules(Principal principal) {
        return ResponseEntity.ok(workloadService.rules(principal.getName()));
    }

    @PostMapping(ApiURL.RECALCULATE)
    public ResponseEntity<?> recalculate(@RequestBody CalculationSettingsDTO settings) {
        return ResponseEntity.ok(Map.of("calculatedRows", workloadService.recalculate(settings)));
    }
}
