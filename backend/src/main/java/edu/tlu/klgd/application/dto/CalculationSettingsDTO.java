package edu.tlu.klgd.application.dto;

import edu.tlu.klgd.domain.common.TeachingRuleConstant;
import java.util.List;

public record CalculationSettingsDTO(
    boolean detectGuestByPosition,
    double graduationInternshipWeeks,
    double defaultInternshipDays,
    double surveyingInternshipDays,
    double materialTheoryHours,
    double materialLabHours,
    double soilTheoryHours,
    double soilLabHours,
    double geotechnicalTheoryHours,
    double geotechnicalLabHours,
    double labGroupSize,
    double labBaseK,
    double labBaseStudents,
    double labIncrementPerStudent,
    double labMinK,
    double labMaxK,
    double theoryBaseK,
    double theoryBaseStudents,
    double theoryIncrementPerStudent,
    double theoryMinK,
    double theoryMaxK,
    List<SubjectRuleConfigDTO> subjectRules
) {
    public static CalculationSettingsDTO defaults() {
        return new CalculationSettingsDTO(
            TeachingRuleConstant.DEFAULT_DETECT_GUEST_BY_POSITION,
            TeachingRuleConstant.DEFAULT_GRADUATION_INTERNSHIP_WEEKS,
            TeachingRuleConstant.DEFAULT_INTERNSHIP_DAYS,
            TeachingRuleConstant.DEFAULT_SURVEYING_INTERNSHIP_DAYS,
            TeachingRuleConstant.MATERIAL_THEORY_HOURS,
            TeachingRuleConstant.MATERIAL_LAB_HOURS,
            TeachingRuleConstant.SOIL_THEORY_HOURS,
            TeachingRuleConstant.SOIL_LAB_HOURS,
            TeachingRuleConstant.GEOTECHNICAL_THEORY_HOURS,
            TeachingRuleConstant.GEOTECHNICAL_LAB_HOURS,
            TeachingRuleConstant.LAB_GROUP_SIZE,
            TeachingRuleConstant.LAB_BASE_K,
            TeachingRuleConstant.LAB_BASE_STUDENTS,
            TeachingRuleConstant.LAB_INCREMENT_PER_STUDENT,
            TeachingRuleConstant.LAB_MIN_K,
            TeachingRuleConstant.LAB_MAX_K,
            TeachingRuleConstant.DEFAULT_BASE_K,
            TeachingRuleConstant.DEFAULT_BASE_STUDENTS,
            TeachingRuleConstant.DEFAULT_INCREMENT_PER_STUDENT,
            TeachingRuleConstant.DEFAULT_MIN_K,
            TeachingRuleConstant.DEFAULT_MAX_K,
            List.of()
        );
    }
}
