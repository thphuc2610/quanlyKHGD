package edu.tlu.klgd.application.dto;

public record TeacherWorkloadDTO(
    String teacherName,
    String departmentPh,
    long classCount,
    double totalCredits,
    long totalStudents,
    double totalStandardHours
) {
}
