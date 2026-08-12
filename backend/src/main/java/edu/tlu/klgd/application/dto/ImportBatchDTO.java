package edu.tlu.klgd.application.dto;

import edu.tlu.klgd.domain.entity.ImportBatch;
import edu.tlu.klgd.domain.entity.ImportStatus;
import java.time.Instant;

public record ImportBatchDTO(
    Long id,
    String fileName,
    String academicYear,
    String semester,
    ImportStatus status,
    Instant createdAt,
    int totalRows,
    int validRows,
    int warningRows
) {
    public static ImportBatchDTO from(ImportBatch batch) {
        return new ImportBatchDTO(
            batch.getId(),
            batch.getFileName(),
            batch.getAcademicYear(),
            batch.getSemester(),
            batch.getStatus(),
            batch.getCreatedAt(),
            batch.getTotalRows(),
            batch.getValidRows(),
            batch.getWarningRows()
        );
    }
}
