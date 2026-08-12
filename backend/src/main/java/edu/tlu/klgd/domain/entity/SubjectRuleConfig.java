package edu.tlu.klgd.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "subject_rule_config")
public class SubjectRuleConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "rule_code", nullable = false, unique = true)
    private String code;

    @Column(name = "subject_name", nullable = false)
    private String name;

    @Column(name = "formula", nullable = false, length = 2000)
    private String formula;

    @Column(name = "coefficient_theory_formula", length = 1000)
    private String coefficientTheoryFormula;

    @Column(name = "coefficient_practice_formula", length = 1000)
    private String coefficientPracticeFormula;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFormula() { return formula; }
    public void setFormula(String formula) { this.formula = formula; }
    public String getCoefficientTheoryFormula() { return coefficientTheoryFormula; }
    public void setCoefficientTheoryFormula(String coefficientTheoryFormula) { this.coefficientTheoryFormula = coefficientTheoryFormula; }
    public String getCoefficientPracticeFormula() { return coefficientPracticeFormula; }
    public void setCoefficientPracticeFormula(String coefficientPracticeFormula) { this.coefficientPracticeFormula = coefficientPracticeFormula; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
