package edu.tlu.klgd.infracstructure.persistence.jpa;

import static edu.tlu.klgd.domain.common.TeachingRuleConstant.*;

import edu.tlu.klgd.application.dto.CalculationSettingsDTO;
import edu.tlu.klgd.application.dto.RuleResultDTO;
import edu.tlu.klgd.application.dto.SubjectRuleConfigDTO;
import edu.tlu.klgd.domain.common.util.TextNormalizer;
import edu.tlu.klgd.domain.entity.ClassRecord;
import edu.tlu.klgd.domain.service.TeachingRuleService;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class TeachingRuleServiceImpl implements TeachingRuleService {
    private static final DecimalFormat EXPLANATION_NUMBER_FORMAT = new DecimalFormat(EXPLANATION_NUMBER_FORMAT_PATTERN);
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:[\\.,]\\d+)?");

    @Override
    public RuleResultDTO calculate(ClassRecord record, CalculationSettingsDTO settings) {
        String subject = TextNormalizer.normalize(record.getSubjectName());
        double students = value(record.getStudentCount());
        double credits = value(record.getCredits());
        if (isGuest(record, settings.detectGuestByPosition())) {
            return new RuleResultDTO(RULE_GUEST_CODE, RULE_GUEST_NAME, null, null, null,
                    round(credits * CREDIT_TO_STANDARD_HOURS), GUEST_EXPLANATION);
        }
        SubjectRuleConfigDTO configuredRule = findConfiguredRule(record.getSubjectName(), settings.subjectRules());
        if ((configuredRule != null && RULE_GRADUATION_PROJECT_CODE.equals(configuredRule.code()))
                || isGraduationProject(subject)) {
            return calculateGraduationProject(record, students);
        }
        if (configuredRule != null && RULE_INDUSTRY_INTERNSHIP_CODE.equals(configuredRule.code())) {
            return calculateIndustryInternship(record, students);
        }
        if (configuredRule != null && !configuredFormula(configuredRule).isBlank()) {
            return calculateConfiguredRule(record, configuredRule, settings);
        }
        if (subject.contains(SUBJECT_GRADUATION_INTERNSHIP) || subject.contains(SUBJECT_PROFESSIONAL_INTERNSHIP)) {
            double coefficientK = GRADUATION_INTERNSHIP_HOURS_PER_STUDENT_PER_WEEK;
            double weeks = credits;
            double hours = coefficientK * students * weeks;
            return new RuleResultDTO(RULE_GRADUATION_INTERNSHIP_CODE, RULE_GRADUATION_INTERNSHIP_NAME,
                    round(coefficientK), null, null, round(hours),
                    String.format(GRADUATION_INTERNSHIP_EXPLANATION_FORMAT, formatNumber(weeks)));
        }
        if (subject.contains(SUBJECT_INDUSTRY_INTERNSHIP)) {
            return calculateIndustryInternship(record, students);
        }
        if (subject.contains(SUBJECT_SURVEYING_INTERNSHIP)) {
            double k = students < SURVEYING_INTERNSHIP_SMALL_CLASS_LIMIT
                    ? SURVEYING_INTERNSHIP_SMALL_CLASS_K
                    : SURVEYING_INTERNSHIP_BASE_K + (students - SURVEYING_INTERNSHIP_BASE_STUDENTS)
                            * SURVEYING_INTERNSHIP_INCREMENT_PER_STUDENT;
            return new RuleResultDTO(RULE_SURVEYING_INTERNSHIP_CODE, RULE_SURVEYING_INTERNSHIP_NAME,
                    round(k), null, null, round(k * settings.surveyingInternshipDays()),
                    String.format(INTERNSHIP_EXPLANATION_FORMAT, formatNumber(round(k)),
                            formatNumber(settings.surveyingInternshipDays())));
        }
        if (subject.contains(SUBJECT_INTERNSHIP)) {
            double k = students < DEFAULT_INTERNSHIP_SMALL_CLASS_LIMIT
                    ? DEFAULT_INTERNSHIP_SMALL_CLASS_K
                    : DEFAULT_INTERNSHIP_BASE_K
                            + (students - DEFAULT_INTERNSHIP_BASE_STUDENTS) * DEFAULT_INTERNSHIP_INCREMENT_PER_STUDENT;
            return new RuleResultDTO(RULE_DEFAULT_INTERNSHIP_CODE, RULE_DEFAULT_INTERNSHIP_NAME,
                    round(k), null, null, round(k * settings.defaultInternshipDays()),
                    String.format(INTERNSHIP_EXPLANATION_FORMAT, formatNumber(round(k)),
                            formatNumber(settings.defaultInternshipDays())));
        }
        if (subject.contains(SUBJECT_LAB)) {
            double k = calculateCumulativeLabK(students, settings);
            return new RuleResultDTO(RULE_LAB_CODE, RULE_LAB_NAME, k, k, null,
                    round(credits * CREDIT_TO_STANDARD_HOURS * k), LAB_EXPLANATION);
        }
        if (subject.contains(SUBJECT_PROJECT)) {
            double k = clamp(PROJECT_BASE_K + (students - PROJECT_BASE_STUDENTS) * PROJECT_INCREMENT_PER_STUDENT,
                    PROJECT_MIN_K, PROJECT_MAX_K);
            return new RuleResultDTO(RULE_PROJECT_CODE, RULE_PROJECT_NAME, k, k, null,
                    round(credits * CREDIT_TO_STANDARD_HOURS * k), PROJECT_EXPLANATION);
        }
        if (isForeignLanguage(record)) {
            double k = calculateDefaultK(students);
            return new RuleResultDTO(RULE_FOREIGN_LANGUAGE_CODE, RULE_FOREIGN_LANGUAGE_NAME, k, k, null,
                    round(credits * CREDIT_TO_STANDARD_HOURS * k), DEFAULT_EXPLANATION);
        }
        if (isPhysicalEducation(record)) {
            double kTheory = clamp(
                    PHYSICAL_EDUCATION_THEORY_BASE_K
                            + (students - PHYSICAL_EDUCATION_BASE_STUDENTS) * DEFAULT_INCREMENT_PER_STUDENT,
                    PHYSICAL_EDUCATION_THEORY_MIN_K,
                    PHYSICAL_EDUCATION_THEORY_MAX_K);
            double kPractice = clamp(
                    PHYSICAL_EDUCATION_PRACTICE_BASE_K
                            + (students - PHYSICAL_EDUCATION_BASE_STUDENTS) * DEFAULT_INCREMENT_PER_STUDENT,
                    PHYSICAL_EDUCATION_PRACTICE_MIN_K,
                    PHYSICAL_EDUCATION_PRACTICE_MAX_K);
            return new RuleResultDTO(RULE_PHYSICAL_EDUCATION_CODE, RULE_PHYSICAL_EDUCATION_NAME, null, kTheory,
                    kPractice,
                    round(PHYSICAL_EDUCATION_PRACTICE_HOURS * kTheory + PHYSICAL_EDUCATION_THEORY_HOURS * kPractice),
                    PHYSICAL_EDUCATION_EXPLANATION);
        }
        if (isMaterial(subject)) {
            return calculateSubjectWithLabs(
                    RULE_MATERIAL_CODE,
                    RULE_MATERIAL_NAME,
                    students,
                    settings.materialTheoryHours(),
                    settings.materialLabHours(),
                    settings);
        }
        if (subject.contains(SUBJECT_SOIL)) {
            return calculateSubjectWithLabs(
                    RULE_SOIL_CODE,
                    RULE_SOIL_NAME,
                    students,
                    settings.soilTheoryHours(),
                    settings.soilLabHours(),
                    settings);
        }
        if (subject.contains(SUBJECT_GEOTECHNICAL)) {
            return calculateSubjectWithLabs(
                    RULE_GEOTECHNICAL_CODE,
                    RULE_GEOTECHNICAL_NAME,
                    students,
                    settings.geotechnicalTheoryHours(),
                    settings.geotechnicalLabHours(),
                    settings);
        }
        double k = calculateDefaultK(students);
        return new RuleResultDTO(RULE_DEFAULT_CODE, RULE_DEFAULT_NAME, k, k, null,
                round(credits * CREDIT_TO_STANDARD_HOURS * k), DEFAULT_EXPLANATION);
    }

    private RuleResultDTO calculateConfiguredRule(
            ClassRecord record,
            SubjectRuleConfigDTO rule,
            CalculationSettingsDTO settings) {
        String formula = configuredFormula(rule);
        FormulaRuleEvaluator.Evaluation evaluated = FormulaRuleEvaluator.evaluate(
                record,
                rule.code(),
                rule.name(),
                formula,
                settings);
        if (!Double.isNaN(evaluated.standardHours()) && !Double.isInfinite(evaluated.standardHours())) {
            return new RuleResultDTO(
                    evaluated.ruleCode(),
                    evaluated.ruleName(),
                    round(evaluated.coefficientK()),
                    round(evaluated.coefficientTheory()),
                    round(evaluated.coefficientPractice()),
                    round(evaluated.standardHours()),
                    formula);
        }

        double students = value(record.getStudentCount());
        double credits = value(record.getCredits());
        String normalizedFormula = TextNormalizer.normalize(formula);
        List<Double> standardNumbers = numbers(lineContaining(formula, "giờ chuẩn", "gio chuan"));

        if (normalizedFormula.contains("klt") || normalizedFormula.contains("ktn") || normalizedFormula.contains("k_lt")
                || normalizedFormula.contains("k_th")) {
            double theoryHours = standardNumbers.size() > 0 ? standardNumbers.get(0)
                    : credits * CREDIT_TO_STANDARD_HOURS;
            double labHours = standardNumbers.size() > 1 ? standardNumbers.get(1) : 0;
            return calculateSubjectWithLabs(rule.code(), rule.name(), students, theoryHours, labHours, settings);
        }

        double k = calculateConfiguredK(students, formula);
        double standardHours;
        if (normalizedFormula.contains("tin chi") || normalizedFormula.contains("tc")) {
            double hoursPerCredit = standardNumbers.isEmpty() ? CREDIT_TO_STANDARD_HOURS : standardNumbers.get(0);
            standardHours = credits * hoursPerCredit * k;
        } else if (normalizedFormula.contains("sv")) {
            double multiplier = standardNumbers.isEmpty() ? 1 : standardNumbers.get(0);
            standardHours = students * multiplier * k;
        } else {
            double baseHours = standardNumbers.isEmpty() ? credits * CREDIT_TO_STANDARD_HOURS : standardNumbers.get(0);
            standardHours = baseHours * k;
        }
        return new RuleResultDTO(rule.code(), rule.name(), round(k), round(k), null, round(standardHours), formula);
    }

    private String configuredFormula(SubjectRuleConfigDTO rule) {
        List<String> parts = new ArrayList<>();
        addFormulaPart(parts, rule.coefficientTheoryFormula());
        addFormulaPart(parts, rule.coefficientPracticeFormula());
        addFormulaPart(parts, rule.formula());
        return String.join("; ", parts);
    }

    private void addFormulaPart(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value.trim());
        }
    }

    private double calculateConfiguredK(double students, String formula) {
        String kLine = lineContaining(formula, "K =", "K=");
        List<Double> values = numbers(kLine == null ? formula : kLine);
        if (values.size() >= 5) {
            return clamp(values.get(0) + (students - values.get(1)) * values.get(2), values.get(3), values.get(4));
        }
        return calculateDefaultK(students);
    }

    private SubjectRuleConfigDTO findConfiguredRule(String subjectName, List<SubjectRuleConfigDTO> rules) {
        if (subjectName == null || rules == null || rules.isEmpty()) {
            return null;
        }
        String subject = TextNormalizer.normalize(subjectName);
        SubjectRuleConfigDTO exactSubjectRule = rules.stream()
                .filter(rule -> rule.name() != null && subject.equals(TextNormalizer.normalize(rule.name())))
                .findFirst()
                .orElse(null);
        if (exactSubjectRule != null) {
            return exactSubjectRule;
        }
        SubjectRuleConfigDTO builtInRule = firstMatchingBuiltInRule(subject, rules);
        if (builtInRule != null) {
            return builtInRule;
        }
        return ruleByCode(rules, RULE_DEFAULT_CODE);
    }

    private SubjectRuleConfigDTO firstMatchingBuiltInRule(String normalizedSubject, List<SubjectRuleConfigDTO> rules) {
        for (String code : List.of(
                RULE_GRADUATION_INTERNSHIP_CODE,
                RULE_GRADUATION_PROJECT_CODE,
                RULE_INDUSTRY_INTERNSHIP_CODE,
                RULE_SURVEYING_INTERNSHIP_CODE,
                RULE_DEFAULT_INTERNSHIP_CODE,
                RULE_MATERIAL_CODE,
                RULE_SOIL_CODE,
                RULE_GEOTECHNICAL_CODE,
                RULE_PHYSICAL_EDUCATION_CODE,
                RULE_FOREIGN_LANGUAGE_CODE,
                RULE_LAB_CODE,
                RULE_PROJECT_CODE,
                RULE_GUEST_CODE)) {
            SubjectRuleConfigDTO rule = ruleByCode(rules, code);
            if (rule != null && matchesBuiltInRule(normalizedSubject, code)) {
                return rule;
            }
        }
        return null;
    }

    private SubjectRuleConfigDTO ruleByCode(List<SubjectRuleConfigDTO> rules, String code) {
        return rules.stream()
                .filter(rule -> code.equals(rule.code()))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesBuiltInRule(String normalizedSubject, String ruleCode) {
        return switch (ruleCode) {
            case RULE_GRADUATION_INTERNSHIP_CODE -> normalizedSubject.contains(SUBJECT_GRADUATION_INTERNSHIP)
                    || normalizedSubject.contains(SUBJECT_PROFESSIONAL_INTERNSHIP);
            case RULE_GRADUATION_PROJECT_CODE -> isGraduationProject(normalizedSubject);
            case RULE_INDUSTRY_INTERNSHIP_CODE -> normalizedSubject.contains(SUBJECT_INDUSTRY_INTERNSHIP);
            case RULE_SURVEYING_INTERNSHIP_CODE -> normalizedSubject.contains(SUBJECT_SURVEYING_INTERNSHIP);
            case RULE_DEFAULT_INTERNSHIP_CODE -> normalizedSubject.contains(SUBJECT_INTERNSHIP);
            case RULE_LAB_CODE -> normalizedSubject.contains(SUBJECT_LAB);
            case RULE_PROJECT_CODE -> normalizedSubject.contains(SUBJECT_PROJECT);
            case RULE_FOREIGN_LANGUAGE_CODE -> normalizedSubject.contains(SUBJECT_FOREIGN_LANGUAGE);
            case RULE_PHYSICAL_EDUCATION_CODE -> normalizedSubject.contains("giao duc the chat");
            case RULE_MATERIAL_CODE ->
                normalizedSubject.contains(SUBJECT_MATERIAL) || normalizedSubject.contains(SUBJECT_MATERIAL_SHORT);
            case RULE_SOIL_CODE -> normalizedSubject.contains(SUBJECT_SOIL);
            case RULE_GEOTECHNICAL_CODE -> normalizedSubject.contains(SUBJECT_GEOTECHNICAL);
            case RULE_GUEST_CODE -> normalizedSubject.contains(GUEST_TEXT_MARKER);
            case RULE_DEFAULT_CODE -> true;
            default -> false;
        };
    }

    private String lineContaining(String formula, String... needles) {
        if (formula == null) {
            return null;
        }
        for (String line : formula.split(";")) {
            String normalizedLine = TextNormalizer.normalize(line);
            for (String needle : needles) {
                if (normalizedLine.contains(TextNormalizer.normalize(needle))) {
                    return line;
                }
            }
        }
        return null;
    }

    private List<Double> numbers(String value) {
        List<Double> result = new ArrayList<>();
        if (value == null) {
            return result;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(value);
        while (matcher.find()) {
            result.add(Double.parseDouble(matcher.group().replace(',', '.')));
        }
        return result;
    }

    private RuleResultDTO calculateSubjectWithLabs(
            String ruleCode,
            String ruleName,
            double students,
            double theoryHours,
            double labHours,
            CalculationSettingsDTO settings) {
        double kTheory = calculateSpecialTheoryK(students, settings);
        double kLab = calculateSpecialLabK(students, settings);
        double totalHours = theoryHours * kTheory + labHours * kLab;
        return new RuleResultDTO(ruleCode, ruleName, null, kTheory, round(kLab), round(totalHours),
                String.format(
                        SPECIAL_LAB_EXPLANATION_FORMAT,
                        formatNumber(theoryHours),
                        formatNumber(labHours)));
    }

    private RuleResultDTO calculateIndustryInternship(ClassRecord record, double students) {
        double weeks = value(record.getCredits());
        double hoursPerStudent = INDUSTRY_INTERNSHIP_HOURS_PER_STUDENT_PER_WEEK * weeks;
        return new RuleResultDTO(
                RULE_INDUSTRY_INTERNSHIP_CODE,
                RULE_INDUSTRY_INTERNSHIP_NAME,
                round(hoursPerStudent),
                null,
                null,
                round(hoursPerStudent * students),
                String.format(
                        INDUSTRY_INTERNSHIP_EXPLANATION_FORMAT,
                        formatNumber(weeks),
                        formatNumber(hoursPerStudent)));
    }

    private RuleResultDTO calculateGraduationProject(ClassRecord record, double students) {
        double majorFactor = graduationProjectMajorFactor(record);
        double hours = students * GRADUATION_PROJECT_TECHNICAL_HOURS_PER_STUDENT * majorFactor;
        return new RuleResultDTO(
                RULE_GRADUATION_PROJECT_CODE,
                RULE_GRADUATION_PROJECT_NAME,
                round(majorFactor),
                null,
                null,
                round(hours),
                String.format(
                        "%s SV x 14 x hệ số Khối ngành %s.",
                        formatNumber(students),
                        formatNumber(majorFactor)));
    }

    private double graduationProjectMajorFactor(ClassRecord record) {
        String text = TextNormalizer.normalize(String.join(" ",
                text(record.getSubjectName()),
                text(record.getClassName()),
                text(record.getDepartmentHn()),
                text(record.getDepartmentPh()),
                text(record.getUnitName())));
        if (text.contains(MAJOR_TECHNICAL)
                || text.contains(MAJOR_TECHNOLOGY)
                || text.contains(MAJOR_INFORMATION_TECHNOLOGY)
                || text.contains(MAJOR_INFORMATION_TECHNOLOGY_SHORT)) {
            return 1.0;
        }
        return GRADUATION_PROJECT_OTHER_FACTOR;
    }

    private double calculateSpecialLabK(double students, CalculationSettingsDTO settings) {
        return calculateCumulativeLabK(students, settings);
    }

    private double calculateCumulativeLabK(double students, CalculationSettingsDTO settings) {
        if (students <= 0)
            return 0.0;
        double groupSize = Math.max(1, settings.labGroupSize());
        int numGroups = (int) Math.ceil(students / groupSize);
        double studentsPerGroup = students / numGroups;
        double kLabPerGroup = settings.labBaseK()
                + (studentsPerGroup - settings.labBaseStudents()) * settings.labIncrementPerStudent();
        return round(kLabPerGroup * numGroups);
    }

    private boolean isGuest(ClassRecord record, boolean detectGuestByPosition) {
        String name = TextNormalizer.normalize(record.getTeacherOriginalName());
        String position = TextNormalizer.normalize(record.getPosition());
        boolean byName = name.contains(GUEST_NAME_MARKER) || name.contains(GUEST_TEXT_MARKER);
        boolean byPosition = detectGuestByPosition
                && (position.contains(GUEST_TEXT_MARKER) || position.contains(INVITED_TEACHER_POSITION));
        return byName || byPosition;
    }

    private boolean isForeignLanguage(ClassRecord record) {
        String department = TextNormalizer.normalize(record.getDepartmentHn());
        String subject = TextNormalizer.normalize(record.getSubjectName());
        return subject.contains(SUBJECT_FOREIGN_LANGUAGE)
                || department.contains(DEPARTMENT_ENGLISH)
                || department.contains(DEPARTMENT_ENGLISH_SHORT)
                || department.contains(DEPARTMENT_CHINESE);
    }

    private boolean isPhysicalEducation(ClassRecord record) {
        String departmentHn = TextNormalizer.normalize(record.getDepartmentHn());
        String departmentPh = TextNormalizer.normalize(record.getDepartmentPh());
        return departmentHn.contains(DEPARTMENT_PHYSICAL_EDUCATION)
                && (departmentPh.equals(DEPARTMENT_GENERAL_SCIENCE)
                        || departmentPh.contains(DEPARTMENT_PHYSICAL_EDUCATION));
    }

    private boolean isMaterial(String subject) {
        return subject.contains(SUBJECT_MATERIAL) || subject.contains(SUBJECT_MATERIAL_SHORT);
    }

    private boolean isGraduationProject(String subject) {
        return subject.contains(SUBJECT_GRADUATION_PROJECT) || subject.contains(SUBJECT_GRADUATION_PROJECT_SHORT);
    }

    private double calculateTheoryK(double students, CalculationSettingsDTO settings) {
        return clamp(
                settings.theoryBaseK()
                        + (students - settings.theoryBaseStudents()) * settings.theoryIncrementPerStudent(),
                settings.theoryMinK(),
                settings.theoryMaxK());
    }

    private double calculateSpecialTheoryK(double students, CalculationSettingsDTO settings) {
        return round(settings.theoryBaseK()
                + (students - settings.theoryBaseStudents()) * settings.theoryIncrementPerStudent());
    }

    private double calculateDefaultK(double students) {
        return clamp(
                DEFAULT_BASE_K + (students - DEFAULT_BASE_STUDENTS) * DEFAULT_INCREMENT_PER_STUDENT,
                DEFAULT_MIN_K,
                DEFAULT_MAX_K);
    }

    private static double value(Number value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static double clamp(double value, double min, double max) {
        return round(Math.max(min, Math.min(max, value)));
    }

    private static double round(double value) {
        return Math.round(value * ROUND_SCALE) / ROUND_SCALE;
    }

    private static Double round(Double value) {
        return value == null ? null : round(value.doubleValue());
    }

    private static String formatNumber(double value) {
        return EXPLANATION_NUMBER_FORMAT.format(value);
    }
}
