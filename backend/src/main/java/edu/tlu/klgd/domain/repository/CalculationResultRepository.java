package edu.tlu.klgd.domain.repository;

import edu.tlu.klgd.domain.entity.CalculationResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalculationResultRepository extends JpaRepository<CalculationResult, Long> {
    List<CalculationResult> findByImportBatchId(Long importBatchId);
    void deleteByImportBatchId(Long importBatchId);
}
