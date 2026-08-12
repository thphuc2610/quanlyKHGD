package edu.tlu.klgd.application.dto;

public record SubjectRuleConfigDTO(
    Long id,
    String code,
    String name,
    String coefficientTheoryFormula,
    String coefficientPracticeFormula,
    String formula,
    String note
) {
}
