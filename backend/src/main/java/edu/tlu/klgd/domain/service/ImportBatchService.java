package edu.tlu.klgd.domain.service;

import edu.tlu.klgd.application.dto.ClassRecordDTO;
import edu.tlu.klgd.application.dto.ImportBatchDTO;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ImportBatchService {
    ImportBatchDTO importWorkbook(MultipartFile file, String academicYear, String semester) throws IOException;
    List<ImportBatchDTO> findAll();
    List<ClassRecordDTO> findRecords(Long importBatchId);
    void delete(Long importBatchId);
}
