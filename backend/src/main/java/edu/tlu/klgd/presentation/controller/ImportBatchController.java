package edu.tlu.klgd.presentation.controller;

import edu.tlu.klgd.application.dto.CalculationSettingsDTO;
import edu.tlu.klgd.domain.common.ApiField;
import edu.tlu.klgd.domain.common.ApiMessage;
import edu.tlu.klgd.domain.common.ApiURL;
import edu.tlu.klgd.domain.common.util.ExcelHelper;
import edu.tlu.klgd.domain.service.ImportBatchService;
import edu.tlu.klgd.domain.service.WorkloadService;
import edu.tlu.klgd.infracstructure.exception.BadRequestException;
import edu.tlu.klgd.infracstructure.security.SecurityConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin(origins = SecurityConstant.ALL_ORIGINS, maxAge = SecurityConstant.CORS_MAX_AGE_SECONDS)
@RestController
@RequestMapping(ApiURL.IMPORT_BATCH)
@Validated
public class ImportBatchController {
    private final ImportBatchService importBatchService;
    private final WorkloadService workloadService;

    public ImportBatchController(ImportBatchService importBatchService, WorkloadService workloadService) {
        this.importBatchService = importBatchService;
        this.workloadService = workloadService;
    }

    @GetMapping
    public ResponseEntity<?> findAll() {
        return ResponseEntity.ok(importBatchService.findAll());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
        @RequestPart(ApiField.FILE) MultipartFile file,
        @RequestParam @NotBlank String academicYear,
        @RequestParam @NotBlank String semester,
        HttpServletRequest request
    ) throws IOException {
        if (!ExcelHelper.hasSupportedImportFormat(file)) {
            throw new BadRequestException(ApiMessage.EXCEL_FILE_REQUIRED);
        }
        return new ResponseEntity<>(importBatchService.importWorkbook(file, academicYear, semester), HttpStatus.CREATED);
    }

    @GetMapping(ApiURL.ID_RECORDS)
    public ResponseEntity<?> records(@PathVariable Long id) {
        return ResponseEntity.ok(importBatchService.findRecords(id));
    }

    @DeleteMapping(ApiURL.ID)
    public ResponseEntity<?> delete(@PathVariable Long id) {
        importBatchService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(ApiURL.ID_CALCULATE)
    public ResponseEntity<?> calculate(
        @PathVariable Long id,
        @RequestBody(required = false) CalculationSettingsDTO settings
    ) {
        int rows = workloadService.calculate(id, settings == null ? CalculationSettingsDTO.defaults() : settings);
        return ResponseEntity.ok(Map.of(ApiField.CALCULATED_ROWS, rows));
    }
}
