package edu.tlu.klgd.infracstructure.persistence.jpa;

import edu.tlu.klgd.application.dto.*;
import edu.tlu.klgd.domain.common.ApiMessage;
import edu.tlu.klgd.domain.common.TeachingRuleConstant;
import edu.tlu.klgd.domain.common.WorkloadConstant;
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
        calculationResultRepository.deleteByImportBatchId(importBatchId);
        List<CalculationResult> results = classRecordRepository.findByImportBatchId(importBatchId).stream()
            .filter(ClassRecord::isValid)
            .map(record -> toResult(record, batch, teachingRuleService.calculate(record, settings)))
            .toList();
        calculationResultRepository.saveAll(results);
        batch.setStatus(ImportStatus.CALCULATED);
        importBatchRepository.save(batch);
        return results.size();
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
        return results.stream()
            .collect(Collectors.groupingBy(result ->
                result.getClassRecord().getTeacherName()
                    + WorkloadConstant.GROUP_KEY_SEPARATOR
                    + result.getClassRecord().getDepartmentPh()
            ))
            .values()
            .stream()
            .map(group -> {
                ClassRecord first = group.getFirst().getClassRecord();
                return new TeacherWorkloadDTO(
                    first.getTeacherName(),
                    first.getDepartmentPh(),
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
        return scopedResults(username).stream()
            .map(result -> {
                ClassRecord record = result.getClassRecord();
                return new TeacherWorkloadDetailDTO(
                    record.getTeacherName(),
                    record.getClassName(),
                    record.getSubjectName(),
                    value(record.getCredits()),
                    value(record.getStudentCount()),
                    record.getDepartmentPh(),
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
            .collect(Collectors.groupingBy(result -> result.getClassRecord().getDepartmentPh()))
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
        return classRecordRepository.findTeacherOptions();
    }

    private List<CalculationResult> scopedResults(String username) {
        List<CalculationResult> results = calculationResultRepository.findAll();
        AppUser user = appUserRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException(ApiMessage.RESOURCE_USER, username));
        if (user.getRoles().contains(UserRole.ADMIN)) {
            return results;
        }
        String teacherName = normalize(user.getTeacherName());
        if (teacherName == null) {
            return List.of();
        }
        return results.stream()
            .filter(result -> teacherName.equals(normalize(result.getClassRecord().getTeacherName())))
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
}
