package edu.tlu.klgd.domain.repository;

import edu.tlu.klgd.domain.entity.ImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {
}
