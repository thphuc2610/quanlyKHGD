package edu.tlu.klgd.application.dto;

public record TeacherWorkloadDetailDTO(
    String teacherName,
    String className,
    String subjectName,
    double credits,
    long studentCount,
    String departmentPh,
    String unitName,
    Double coefficientK,
    Double coefficientTheory,
    Double coefficientPractice,
    double standardHours,
    String ruleName,
    String academicYear,
    String semester
) {
}
