package edu.tlu.klgd.infracstructure.persistence.mapper;

import edu.tlu.klgd.application.dto.ImportBatchDTO;
import edu.tlu.klgd.domain.entity.ImportBatch;
import org.springframework.stereotype.Component;

@Component
public class ImportBatchMapper {
    public ImportBatchDTO toDto(ImportBatch batch) {
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
