package edu.tlu.klgd.domain.repository;

import edu.tlu.klgd.application.dto.TeacherOptionDTO;
import edu.tlu.klgd.domain.entity.ClassRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClassRecordRepository extends JpaRepository<ClassRecord, Long> {
    List<ClassRecord> findByImportBatchId(Long importBatchId);

    @Query("""
        select distinct new edu.tlu.klgd.application.dto.TeacherOptionDTO(record.teacherName, record.departmentPh)
        from ClassRecord record
        where record.valid = true
            and record.teacherName is not null
            and trim(record.teacherName) <> ''
        order by record.teacherName, record.departmentPh
        """)
    List<TeacherOptionDTO> findTeacherOptions();
}
