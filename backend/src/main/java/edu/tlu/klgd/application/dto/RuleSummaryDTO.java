package edu.tlu.klgd.application.dto;

public record RuleSummaryDTO(
    String ruleCode,
    String ruleName,
    long classCount,
    double totalStandardHours
) {
}
