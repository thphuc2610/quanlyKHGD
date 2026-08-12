package edu.tlu.klgd.application.dto;

import edu.tlu.klgd.domain.entity.ClassRecord;

public record ClassRecordDTO(
    Long id,
    String academicYear,
    String semester,
    String className,
    String subjectName,
    Double credits,
    String departmentHn,
    String departmentPh,
    Integer studentCount,
    String teacherName,
    String position,
    String degree,
    String academicTitle,
    boolean valid,
    String warningMessage
) {
    public static ClassRecordDTO from(ClassRecord record) {
        return new ClassRecordDTO(
            record.getId(),
            record.getAcademicYear(),
            record.getSemester(),
            record.getClassName(),
            record.getSubjectName(),
            record.getCredits(),
            record.getDepartmentHn(),
            record.getDepartmentPh(),
            record.getStudentCount(),
            record.getTeacherName(),
            record.getPosition(),
            record.getDegree(),
            record.getAcademicTitle(),
            record.isValid(),
            record.getWarningMessage()
        );
    }
}
