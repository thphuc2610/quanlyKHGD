package edu.tlu.klgd.domain.repository;

import edu.tlu.klgd.domain.entity.ImportBatch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {
    List<ImportBatch> findByAcademicYearAndSemester(String academicYear, String semester);
    List<ImportBatch> findByStatus(edu.tlu.klgd.domain.entity.ImportStatus status);
}
