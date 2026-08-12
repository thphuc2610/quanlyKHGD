package edu.tlu.klgd.presentation.controller;

import edu.tlu.klgd.domain.common.ApiURL;
import edu.tlu.klgd.domain.common.ReportConstant;
import edu.tlu.klgd.domain.service.ReportService;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Locale;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiURL.REPORT)
@CrossOrigin
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping(ApiURL.WORKLOAD_EXCEL)
    public ResponseEntity<byte[]> exportWorkloadReport(
        @RequestParam(required = false) String academicYear,
        @RequestParam(required = false) String semester
    ) throws IOException {
        byte[] workbook = reportService.exportWorkloadWorkbook(academicYear, semester);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(ReportConstant.WORKLOAD_EXCEL_MEDIA_TYPE))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(buildFileName(academicYear, semester))
                .build()
                .toString())
            .body(workbook);
    }

    private static String buildFileName(String academicYear, String semester) {
        return "bao-cao-khoi-luong-giang-day_%s_%s.xlsx".formatted(
            filePart(academicYear),
            filePart(semester)
        );
    }

    private static String filePart(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .toLowerCase(Locale.ROOT)
            .trim()
            .replaceAll("[^a-z0-9-]+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
        return normalized.isBlank() ? "tat-ca" : normalized;
    }
}
