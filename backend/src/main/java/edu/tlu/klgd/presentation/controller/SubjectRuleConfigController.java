package edu.tlu.klgd.presentation.controller;

import edu.tlu.klgd.application.dto.SubjectRuleConfigDTO;
import edu.tlu.klgd.domain.service.SubjectRuleConfigService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workloads/subject-rules")
public class SubjectRuleConfigController {
    private final SubjectRuleConfigService service;

    public SubjectRuleConfigController(SubjectRuleConfigService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<SubjectRuleConfigDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @PutMapping
    public ResponseEntity<List<SubjectRuleConfigDTO>> saveAll(@RequestBody List<SubjectRuleConfigDTO> rules) {
        return ResponseEntity.ok(service.saveAll(rules));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        service.deleteByCode(code);
        return ResponseEntity.noContent().build();
    }
}
