package edu.tlu.klgd.application.dto;

public record RuleResultDTO(
    String ruleCode,
    String ruleName,
    Double coefficientK,
    Double coefficientTheory,
    Double coefficientPractice,
    double standardHours,
    String explanation
) {
}
