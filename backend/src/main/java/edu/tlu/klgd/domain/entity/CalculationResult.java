package edu.tlu.klgd.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "calculation_result")
public class CalculationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_record_id")
    private ClassRecord classRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_batch_id")
    private ImportBatch importBatch;

    @Column(name = "rule_code")
    private String ruleCode;
    @Column(name = "rule_name")
    private String ruleName;
    @Column(name = "coefficient_k")
    private Double coefficientK;
    @Column(name = "coefficient_theory")
    private Double coefficientTheory;
    @Column(name = "coefficient_practice")
    private Double coefficientPractice;
    @Column(name = "standard_hours", nullable = false)
    private Double standardHours;
    @Column(name = "explanation", length = 1200)
    private String explanation;
    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt = Instant.now();

    public Long getId() { return id; }
    public ClassRecord getClassRecord() { return classRecord; }
    public void setClassRecord(ClassRecord classRecord) { this.classRecord = classRecord; }
    public ImportBatch getImportBatch() { return importBatch; }
    public void setImportBatch(ImportBatch importBatch) { this.importBatch = importBatch; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public Double getCoefficientK() { return coefficientK; }
    public void setCoefficientK(Double coefficientK) { this.coefficientK = coefficientK; }
    public Double getCoefficientTheory() { return coefficientTheory; }
    public void setCoefficientTheory(Double coefficientTheory) { this.coefficientTheory = coefficientTheory; }
    public Double getCoefficientPractice() { return coefficientPractice; }
    public void setCoefficientPractice(Double coefficientPractice) { this.coefficientPractice = coefficientPractice; }
    public Double getStandardHours() { return standardHours; }
    public void setStandardHours(Double standardHours) { this.standardHours = standardHours; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
