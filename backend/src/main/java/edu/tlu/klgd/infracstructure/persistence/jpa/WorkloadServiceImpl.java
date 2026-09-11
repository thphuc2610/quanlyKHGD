package edu.tlu.klgd.infracstructure.persistence.jpa;

import edu.tlu.klgd.application.dto.*;
import edu.tlu.klgd.domain.common.ApiMessage;
import edu.tlu.klgd.domain.common.TeachingRuleConstant;
import edu.tlu.klgd.domain.common.WorkloadConstant;
import edu.tlu.klgd.domain.common.util.TextNormalizer;
import edu.tlu.klgd.domain.entity.*;
import edu.tlu.klgd.domain.repository.*;
import edu.tlu.klgd.domain.service.TeachingRuleService;
import edu.tlu.klgd.domain.service.WorkloadService;
import edu.tlu.klgd.infracstructure.exception.ResourceNotFoundException;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkloadServiceImpl implements WorkloadService {
    private final ImportBatchRepository importBatchRepository;
    private final ClassRecordRepository classRecordRepository;
    private final CalculationResultRepository calculationResultRepository;
    private final AppUserRepository appUserRepository;
    private final TeachingRuleService teachingRuleService;

    public WorkloadServiceImpl(
        ImportBatchRepository importBatchRepository,
        ClassRecordRepository classRecordRepository,
        CalculationResultRepository calculationResultRepository,
        AppUserRepository appUserRepository,
        TeachingRuleService teachingRuleService
    ) {
        this.importBatchRepository = importBatchRepository;
        this.classRecordRepository = classRecordRepository;
        this.calculationResultRepository = calculationResultRepository;
        this.appUserRepository = appUserRepository;
        this.teachingRuleService = teachingRuleService;
    }

    @Override
    @Transactional
    public int calculate(Long importBatchId, CalculationSettingsDTO settings) {
        ImportBatch batch = importBatchRepository.findById(importBatchId)
            .orElseThrow(() -> new ResourceNotFoundException(ApiMessage.RESOURCE_IMPORT_BATCH, importBatchId));
        
        Set<ImportBatch> batchesToCalculate = new HashSet<>();
        batchesToCalculate.addAll(importBatchRepository.findByAcademicYearAndSemester(
            batch.getAcademicYear(),
            batch.getSemester()
        ));
        batchesToCalculate.addAll(importBatchRepository.findByStatus(ImportStatus.UPLOADED));
        
        int calculatedRows = 0;
        for (ImportBatch b : batchesToCalculate) {
            calculationResultRepository.deleteByImportBatchId(b.getId());
            List<CalculationResult> results = classRecordRepository.findByImportBatchId(b.getId()).stream()
                .filter(ClassRecord::isValid)
                .map(record -> toResult(record, b, teachingRuleService.calculate(record, settings)))
                .toList();
            calculationResultRepository.saveAll(results);
            b.setStatus(ImportStatus.CALCULATED);
            importBatchRepository.save(b);
            calculatedRows += results.size();
        }
        return calculatedRows;
    }

    @Override
    @Transactional
    public int recalculate(CalculationSettingsDTO settings) {
        calculationResultRepository.deleteAll();
        int calculatedRows = 0;
        for (ImportBatch batch : importBatchRepository.findAll()) {
            List<CalculationResult> results = classRecordRepository.findByImportBatchId(batch.getId()).stream()
                .filter(ClassRecord::isValid)
                .map(record -> toResult(record, batch, teachingRuleService.calculate(record, settings)))
                .toList();
            calculationResultRepository.saveAll(results);
            batch.setStatus(ImportStatus.CALCULATED);
            importBatchRepository.save(batch);
            calculatedRows += results.size();
        }
        return calculatedRows;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewDTO overview(String username) {
        List<CalculationResult> results = scopedResults(username);
        long teacherCount = results.stream().map(result -> result.getClassRecord().getTeacherName()).distinct().count();
        long guestClasses = results.stream().filter(result -> TeachingRuleConstant.RULE_GUEST_CODE.equals(result.getRuleCode())).count();
        double totalHours = results.stream().mapToDouble(CalculationResult::getStandardHours).sum();
        return new DashboardOverviewDTO(teacherCount, results.size(), round(totalHours), guestClasses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherWorkloadDTO> teachers() {
        return buildTeacherWorkloads(calculationResultRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherWorkloadDTO> teachers(String username) {
        return buildTeacherWorkloads(scopedResults(username));
    }

    private List<TeacherWorkloadDTO> buildTeacherWorkloads(List<CalculationResult> results) {
        Map<String, String> teacherDisplayNames = teacherDisplayNames(results);
        return results.stream()
            .collect(Collectors.groupingBy(result ->
                reportTeacherName(result.getClassRecord(), teacherDisplayNames)
                    + WorkloadConstant.GROUP_KEY_SEPARATOR
                    + TextNormalizer.cleanDepartmentName(result.getClassRecord().getDepartmentPh())
            ))
            .values()
            .stream()
            .map(group -> {
                ClassRecord first = group.getFirst().getClassRecord();
                return new TeacherWorkloadDTO(
                    reportTeacherName(first, teacherDisplayNames),
                    TextNormalizer.cleanDepartmentName(first.getDepartmentPh()),
                    group.size(),
                    round(group.stream().mapToDouble(result -> value(result.getClassRecord().getCredits())).sum()),
                    group.stream().mapToLong(result -> value(result.getClassRecord().getStudentCount())).sum(),
                    round(group.stream().mapToDouble(CalculationResult::getStandardHours).sum())
                );
            })
            .sorted(Comparator.comparingDouble(TeacherWorkloadDTO::totalStandardHours).reversed())
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherWorkloadDetailDTO> teacherDetails(String username) {
        List<CalculationResult> results = scopedResults(username);
        Map<String, String> teacherDisplayNames = teacherDisplayNames(results);
        return results.stream()
            .map(result -> {
                ClassRecord record = result.getClassRecord();
                return new TeacherWorkloadDetailDTO(
                    reportTeacherName(record, teacherDisplayNames),
                    record.getClassName(),
                    record.getSubjectName(),
                    value(record.getCredits()),
                    value(record.getStudentCount()),
                    TextNormalizer.cleanDepartmentName(record.getDepartmentPh()),
                    record.getUnitName(),
                    result.getCoefficientK(),
                    result.getCoefficientTheory(),
                    result.getCoefficientPractice(),
                    result.getStandardHours(),
                    result.getRuleName(),
                    record.getAcademicYear(),
                    record.getSemester()
                );
            })
            .sorted(
                Comparator.comparing(TeacherWorkloadDetailDTO::teacherName, Comparator.nullsLast(String::compareToIgnoreCase))
                    .thenComparing(TeacherWorkloadDetailDTO::subjectName, Comparator.nullsLast(String::compareToIgnoreCase))
            )
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentWorkloadDTO> departments(String username) {
        return scopedResults(username).stream()
            .collect(Collectors.groupingBy(result -> TextNormalizer.cleanDepartmentName(result.getClassRecord().getDepartmentPh())))
            .entrySet()
            .stream()
            .map(entry -> new DepartmentWorkloadDTO(
                entry.getKey(),
                entry.getValue().stream().map(result -> result.getClassRecord().getTeacherName()).distinct().count(),
                entry.getValue().size(),
                round(entry.getValue().stream().mapToDouble(CalculationResult::getStandardHours).sum())
            ))
            .sorted(Comparator.comparingDouble(DepartmentWorkloadDTO::totalStandardHours).reversed())
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleSummaryDTO> rules(String username) {
        return scopedResults(username).stream()
            .collect(Collectors.groupingBy(result ->
                result.getRuleCode()
                    + WorkloadConstant.GROUP_KEY_SEPARATOR
                    + result.getRuleName()
            ))
            .values()
            .stream()
            .map(group -> new RuleSummaryDTO(
                group.getFirst().getRuleCode(),
                group.getFirst().getRuleName(),
                group.size(),
                round(group.stream().mapToDouble(CalculationResult::getStandardHours).sum())
            ))
            .sorted(Comparator.comparingDouble(RuleSummaryDTO::totalStandardHours).reversed())
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherOptionDTO> teacherOptions() {
        Map<String, TeacherOptionDTO> options = new LinkedHashMap<>();
        classRecordRepository.findTeacherOptions().forEach(option -> {
            String teacherName = TextNormalizer.cleanTeacherName(option.teacherName());
            if (teacherName.isBlank()) {
                return;
            }
            String cleanDept = TextNormalizer.cleanDepartmentName(option.departmentPh());
            String key = TextNormalizer.normalize(teacherName)
                + WorkloadConstant.GROUP_KEY_SEPARATOR
                + normalizeText(cleanDept);
            options.merge(
                key,
                new TeacherOptionDTO(teacherName, cleanDept),
                (current, candidate) -> new TeacherOptionDTO(preferredTeacherName(current.teacherName(), candidate.teacherName()), current.departmentPh())
            );
        });
        return options.values().stream()
            .sorted(Comparator.comparing(TeacherOptionDTO::teacherName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private List<CalculationResult> scopedResults(String username) {
        List<CalculationResult> results = calculationResultRepository.findAll();
        AppUser user = appUserRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException(ApiMessage.RESOURCE_USER, username));
        if (user.getRoles().contains(UserRole.ADMIN)) {
            return results;
        }
        String teacherName = normalizeTeacherKey(user.getTeacherName());
        if (teacherName == null) {
            return List.of();
        }
        return results.stream()
            .filter(result -> teacherName.equals(normalizeTeacherKey(result.getClassRecord().getTeacherName())))
            .toList();
    }

    private CalculationResult toResult(ClassRecord record, ImportBatch batch, RuleResultDTO rule) {
        CalculationResult result = new CalculationResult();
        result.setClassRecord(record);
        result.setImportBatch(batch);
        result.setRuleCode(rule.ruleCode());
        result.setRuleName(rule.ruleName());
        result.setCoefficientK(rule.coefficientK());
        result.setCoefficientTheory(rule.coefficientTheory());
        result.setCoefficientPractice(rule.coefficientPractice());
        result.setStandardHours(rule.standardHours());
        result.setExplanation(rule.explanation());
        return result;
    }

    private static long value(Integer value) {
        return value == null ? 0 : value;
    }

    private static double value(Double value) {
        return value == null ? 0.0 : value;
    }

    private static double round(double value) {
        return Math.round(value * WorkloadConstant.ROUND_SCALE) / WorkloadConstant.ROUND_SCALE;
    }

    private static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> teacherDisplayNames(List<CalculationResult> results) {
        Map<String, String> names = new LinkedHashMap<>();
        results.stream()
            .map(result -> TextNormalizer.cleanTeacherName(result.getClassRecord().getTeacherName()))
            .filter(value -> !value.isBlank())
            .forEach(name -> names.merge(TextNormalizer.normalize(name), name, WorkloadServiceImpl::preferredTeacherName));
        return names;
    }

    private static String reportTeacherName(ClassRecord record, Map<String, String> teacherDisplayNames) {
        String teacherName = TextNormalizer.cleanTeacherName(record.getTeacherName());
        return teacherDisplayNames.getOrDefault(TextNormalizer.normalize(teacherName), teacherName);
    }

    private static String preferredTeacherName(String current, String candidate) {
        int comparison = Integer.compare(teacherNameScore(candidate), teacherNameScore(current));
        return comparison > 0 || (comparison == 0 && candidate.compareToIgnoreCase(current) < 0) ? candidate : current;
    }

    private static int teacherNameScore(String value) {
        int score = 0;
        if (!value.matches(".*[.,;:]+.*")) {
            score += 2;
        }
        return score + accentScore(value);
    }

    private static int accentScore(String value) {
        return (int) value.chars()
            .filter(character -> character > 127 && character != '\uFFFD')
            .count();
    }

    private static String normalizeTeacherKey(String value) {
        String teacherName = TextNormalizer.cleanTeacherName(value);
        return teacherName.isBlank() ? null : TextNormalizer.normalize(teacherName);
    }

    private static String normalizeText(String value) {
        return value == null ? "" : TextNormalizer.normalize(value);
    }
}
