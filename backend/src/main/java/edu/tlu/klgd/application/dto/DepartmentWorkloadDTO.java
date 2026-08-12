package edu.tlu.klgd.application.dto;

public record DepartmentWorkloadDTO(
    String departmentPh,
    long teacherCount,
    long classCount,
    double totalStandardHours
) {
}
