package edu.tlu.klgd.rules;

import static org.assertj.core.api.Assertions.assertThat;

import edu.tlu.klgd.application.dto.CalculationSettingsDTO;
import edu.tlu.klgd.application.dto.RuleResultDTO;
import edu.tlu.klgd.application.dto.SubjectRuleConfigDTO;
import edu.tlu.klgd.domain.entity.ClassRecord;
import edu.tlu.klgd.infracstructure.persistence.jpa.TeachingRuleServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;

class TeachingRuleEngineTest {
    private final TeachingRuleServiceImpl engine = new TeachingRuleServiceImpl();
    private final CalculationSettingsDTO settings = CalculationSettingsDTO.defaults();

    @Test
    void calculatesGuestTeacherWithoutK() {
        ClassRecord record = baseRecord("Môn bất kỳ", 3, 80);
        record.setTeacherOriginalName("Nguyễn Văn A (ThG)");

        RuleResultDTO result = engine.calculate(record, settings);

        assertThat(result.ruleCode()).isEqualTo("THINH_GIANG");
        assertThat(result.standardHours()).isEqualTo(45.0);
        assertThat(result.coefficientK()).isNull();
    }

    @Test
    void calculatesDefaultTheoryAndExerciseFromPdfFormula() {
        ClassRecord record = baseRecord("Kết cấu bê tông", 2, 45);

        RuleResultDTO result = engine.calculate(record, settings);

        assertThat(result.ruleCode()).isEqualTo("LY_THUYET_BAI_TAP");
        assertThat(result.coefficientK()).isEqualTo(1.05);
        assertThat(result.standardHours()).isEqualTo(31.5);
    }

    @Test
    void calculatesForeignLanguageFromPdfFormula() {
        ClassRecord record = baseRecord("Tiếng Anh 1", 3, 30);
        record.setDepartmentHn("Bộ môn Tiếng Anh");
        record.setDepartmentPh("KHCB");

        RuleResultDTO result = engine.calculate(record, settings);

        assertThat(result.ruleCode()).isEqualTo("NGOAI_NGU");
        assertThat(result.coefficientK()).isEqualTo(0.9);
        assertThat(result.standardHours()).isEqualTo(40.5);
    }

    @Test
    void calculatesSurveyingInternshipFromPdfFormula() {
        ClassRecord record = baseRecord("Thực tập trắc địa", 1, 30);

        RuleResultDTO result = engine.calculate(record, settings);

        assertThat(result.ruleCode()).isEqualTo("THUC_TAP_TRAC_DIA");
        assertThat(result.coefficientK()).isEqualTo(2.75);
        assertThat(result.standardHours()).isEqualTo(19.25);
    }

    @Test
    void calculatesGraduationInternshipWithCreditsAsWeeks() {
        ClassRecord record = baseRecord("Thực tập tốt nghiệp ngành kỹ thuật", 3, 18);

        RuleResultDTO result = engine.calculate(record, settings);

        assertThat(result.ruleCode()).isEqualTo("THUC_TAP_TOT_NGHIEP");
        assertThat(result.coefficientK()).isEqualTo(0.5);
        assertThat(result.standardHours()).isEqualTo(27.0);
    }

    @Test
    void calculatesProfessionalInternshipWithCreditsAsWeeks() {
        ClassRecord record = baseRecord("Thuc tap nghe nghiep", 3, 18);

        RuleResultDTO result = engine.calculate(record, settings);

        assertThat(result.ruleCode()).isEqualTo("THUC_TAP_TOT_NGHIEP");
        assertThat(result.coefficientK()).isEqualTo(0.5);
        assertThat(result.standardHours()).isEqualTo(27.0);
    }

    @Test
    void configuredBuiltInRulesStillClassifyProfessionalInternshipBeforeDefaultInternship() {
        ClassRecord record = baseRecord("Thuc tap nghe nghiep", 3, 18);
        CalculationSettingsDTO customSettings = settingsWithRules(
            new SubjectRuleConfigDTO(
                null,
                "THUC_TAP_TOT_NGHIEP",
                "Thuc tap tot nghiep",
                null,
                null,
                "K = 0.5; GC = K * SV * TC",
                ""
            ),
            new SubjectRuleConfigDTO(
                null,
                "THUC_TAP_TRUC_TIEP",
                "Thuc tap truc tiep",
                null,
                null,
                "Neu SV < 30 thi K = 2.0, nguoc lai K = 2.5 + (SV - 35) * 0.05; GC = K * N",
                ""
            )
        );

        RuleResultDTO result = engine.calculate(record, customSettings);

        assertThat(result.ruleCode()).isEqualTo("THUC_TAP_TOT_NGHIEP");
        assertThat(result.coefficientK()).isEqualTo(0.5);
        assertThat(result.standardHours()).isEqualTo(27.0);
    }

    @Test
    void calculatesIndustryInternshipWithCreditsAsWeeks() {
        ClassRecord record = baseRecord("Thuc tap nganh CNTT", 3, 20);

        RuleResultDTO result = engine.calculate(record, settings);

        assertThat(result.ruleCode()).isEqualTo("THUC_TAP_NGANH");
        assertThat(result.coefficientK()).isEqualTo(1.5);
        assertThat(result.standardHours()).isEqualTo(30.0);
    }

    @Test
    void calculatesGraduationProjectForTechnicalMajor() {
        ClassRecord record = baseRecord("Hoc phan tot nghiep", 1.0, 10);
        record.setUnitName("Ky thuat xay dung");

        RuleResultDTO result = engine.calculate(record, settings);

        assertThat(result.ruleCode()).isEqualTo("HPTN");
        assertThat(result.coefficientK()).isEqualTo(1.0);
        assertThat(result.standardHours()).isEqualTo(140.0);
    }

    @Test
    void calculatesGraduationProjectForSharedSocialMajorAdvisor() {
        ClassRecord record = baseRecord("HPTN", 0.5, 2);
        record.setUnitName("Quan tri kinh doanh");

        RuleResultDTO result = engine.calculate(record, settings);

        assertThat(result.ruleCode()).isEqualTo("HPTN");
        assertThat(result.coefficientK()).isEqualTo(0.8);
        assertThat(result.standardHours()).isEqualTo(22.4);
    }

    @Test
    void calculatesPhysicalEducationAsTheoryAndPractice() {
        ClassRecord record = baseRecord("Cầu lông", 1, 55);
        record.setDepartmentHn("Bộ môn Giáo dục thể chất");
        record.setDepartmentPh("KHCB");

        RuleResultDTO result = engine.calculate(record, settings);

        assertThat(result.ruleCode()).isEqualTo("GDTC");
        assertThat(result.coefficientTheory()).isEqualTo(1.05);
        assertThat(result.coefficientPractice()).isEqualTo(0.75);
        assertThat(result.standardHours()).isEqualTo(25.5);
    }

    @Test
    void calculatesConstructionMaterialsWithSeparateLabGroups() {
        ClassRecord record = baseRecord("Vật liệu xây dựng", 3, 40);

        RuleResultDTO result = engine.calculate(record, settings);

        assertThat(result.ruleCode()).isEqualTo("VAT_LIEU_XAY_DUNG");
        assertThat(result.coefficientTheory()).isEqualTo(1.0);
        assertThat(result.coefficientPractice()).isEqualTo(1.05);
        assertThat(result.standardHours()).isEqualTo(51.45);
    }

    @Test
    void calculatesSoilMechanicsWithSixLabHours() {
        ClassRecord record = baseRecord("Cơ học đất", 3, 40);

        RuleResultDTO result = engine.calculate(record, settings);

        assertThat(result.ruleCode()).isEqualTo("CO_HOC_DAT");
        assertThat(result.coefficientTheory()).isEqualTo(1.0);
        assertThat(result.coefficientPractice()).isEqualTo(1.05);
        assertThat(result.standardHours()).isEqualTo(45.3);
    }

    @Test
    void calculatesGeotechnicalEngineeringWithSixLabHours() {
        ClassRecord record = baseRecord("Địa kỹ thuật", 4, 40);

        RuleResultDTO result = engine.calculate(record, settings);

        assertThat(result.ruleCode()).isEqualTo("DIA_KY_THUAT");
        assertThat(result.coefficientTheory()).isEqualTo(1.0);
        assertThat(result.coefficientPractice()).isEqualTo(1.05);
        assertThat(result.standardHours()).isEqualTo(60.3);
    }

    @Test
    void detectsPhysicalEducationWhenSingleDepartmentColumnIsMappedToBothFields() {
        ClassRecord record = baseRecord("Bóng rổ", 1, 50);
        record.setDepartmentHn("Bộ môn Giáo dục thể chất");
        record.setDepartmentPh("Bộ môn Giáo dục thể chất");

        RuleResultDTO result = engine.calculate(record, settings);

        assertThat(result.ruleCode()).isEqualTo("GDTC");
        assertThat(result.standardHours()).isEqualTo(24.0);
    }

    @Test
    void configuredSubjectFormulaOverridesCodeWithoutDeveloperChange() {
        ClassRecord record = baseRecord("Custom subject", 3, 20);
        CalculationSettingsDTO customSettings = settingsWithRules(new SubjectRuleConfigDTO(
            null,
            "SUBJECT_CUSTOM",
            "Custom subject",
            null,
            null,
            "Gio chuan = 1.5 * SV",
            ""
        ));

        RuleResultDTO result = engine.calculate(record, customSettings);

        assertThat(result.ruleCode()).isEqualTo("SUBJECT_CUSTOM");
        assertThat(result.standardHours()).isEqualTo(30.0);
    }

    @Test
    void configuredBuiltInRuleAppliesToMatchingSubjectGroup() {
        ClassRecord record = baseRecord("Thuc tap tot nghiep nganh ky thuat", 3, 12);
        CalculationSettingsDTO customSettings = settingsWithRules(new SubjectRuleConfigDTO(
            null,
            "THUC_TAP_TOT_NGHIEP",
            "Thuc tap tot nghiep",
            null,
            null,
            "K = 0.6; Gio chuan = K * SV * TC",
            ""
        ));

        RuleResultDTO result = engine.calculate(record, customSettings);

        assertThat(result.ruleCode()).isEqualTo("THUC_TAP_TOT_NGHIEP");
        assertThat(result.coefficientK()).isEqualTo(0.6);
        assertThat(result.standardHours()).isEqualTo(21.6);
    }

    @Test
    void configuredGroupedLabFormulaUsesStudentGroups() {
        ClassRecord record = baseRecord("VLXD demo", 3, 40);
        CalculationSettingsDTO customSettings = settingsWithRules(new SubjectRuleConfigDTO(
            null,
            "SUBJECT_VLXD_DEMO",
            "VLXD demo",
            "K_lt = 1.0 + (SV - 40) * 0.01",
            "K_th = max(0.6 + (SV - 25) * 0.015, 0.5)",
            "Gio chuan = 42 * K_lt + 9 * K_th",
            ""
        ));

        RuleResultDTO result = engine.calculate(record, customSettings);

        assertThat(result.ruleCode()).isEqualTo("SUBJECT_VLXD_DEMO");
        assertThat(result.coefficientTheory()).isEqualTo(1.0);
        assertThat(result.coefficientPractice()).isEqualTo(0.83);
        assertThat(result.standardHours()).isEqualTo(49.43);
    }

    private ClassRecord baseRecord(String subjectName, double credits, int students) {
        ClassRecord record = new ClassRecord();
        record.setSubjectName(subjectName);
        record.setCredits(credits);
        record.setStudentCount(students);
        record.setTeacherOriginalName("Nguyễn Văn A");
        record.setDepartmentHn("Bộ môn Xây dựng");
        record.setDepartmentPh("KTCT");
        return record;
    }

    private CalculationSettingsDTO settingsWithRules(SubjectRuleConfigDTO... rules) {
        return new CalculationSettingsDTO(
            settings.detectGuestByPosition(),
            settings.graduationInternshipWeeks(),
            settings.defaultInternshipDays(),
            settings.surveyingInternshipDays(),
            settings.materialTheoryHours(),
            settings.materialLabHours(),
            settings.soilTheoryHours(),
            settings.soilLabHours(),
            settings.geotechnicalTheoryHours(),
            settings.geotechnicalLabHours(),
            settings.labGroupSize(),
            settings.labBaseK(),
            settings.labBaseStudents(),
            settings.labIncrementPerStudent(),
            settings.labMinK(),
            settings.labMaxK(),
            settings.theoryBaseK(),
            settings.theoryBaseStudents(),
            settings.theoryIncrementPerStudent(),
            settings.theoryMinK(),
            settings.theoryMaxK(),
            List.of(rules)
        );
    }
}
