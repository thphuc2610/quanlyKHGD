package edu.tlu.klgd.infracstructure.persistence.mapper;

import edu.tlu.klgd.application.dto.ClassRecordDTO;
import edu.tlu.klgd.domain.common.util.TextNormalizer;
import edu.tlu.klgd.domain.entity.ClassRecord;
import org.springframework.stereotype.Component;

@Component
public class ClassRecordMapper {
    public ClassRecordDTO toDto(ClassRecord record) {
        return new ClassRecordDTO(
            record.getId(),
            record.getAcademicYear(),
            record.getSemester(),
            record.getClassName(),
            record.getSubjectName(),
            record.getCredits(),
            TextNormalizer.cleanDepartmentName(record.getDepartmentHn()),
            TextNormalizer.cleanDepartmentName(record.getDepartmentPh()),
            record.getStudentCount(),
            TextNormalizer.cleanTeacherName(record.getTeacherName()),
            record.getPosition(),
            record.getDegree(),
            record.getAcademicTitle(),
            record.isValid(),
            record.getWarningMessage()
        );
    }
}
