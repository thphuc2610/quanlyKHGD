package edu.tlu.klgd.application.dto;

public record DashboardOverviewDTO(
    long teacherCount,
    long classCount,
    double totalStandardHours,
    long guestClassCount
) {
}
