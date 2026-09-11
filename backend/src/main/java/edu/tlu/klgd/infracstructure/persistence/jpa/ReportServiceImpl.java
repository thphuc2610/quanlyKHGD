package edu.tlu.klgd.infracstructure.persistence.jpa;

import edu.tlu.klgd.domain.common.ReportConstant;
import edu.tlu.klgd.domain.common.TeachingRuleConstant;
import edu.tlu.klgd.domain.common.util.TextNormalizer;
import edu.tlu.klgd.domain.entity.CalculationResult;
import edu.tlu.klgd.domain.entity.ClassRecord;
import edu.tlu.klgd.domain.repository.CalculationResultRepository;
import edu.tlu.klgd.domain.service.ReportService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import edu.tlu.klgd.domain.entity.SubjectRuleConfig;
import edu.tlu.klgd.domain.repository.SubjectRuleConfigRepository;
import edu.tlu.klgd.domain.common.TeachingRuleConstant;
import java.util.stream.Collectors;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportServiceImpl implements ReportService {
    private static final int TITLE_ROW_INDEX = 0;
    private static final int YEAR_ROW_INDEX = 1;
    private static final int TITLE_HEIGHT_IN_POINTS = 24;
    private static final int TEXT_COLUMN_WIDTH = 34;

    private final CalculationResultRepository calculationResultRepository;
    private final SubjectRuleConfigRepository subjectRuleConfigRepository;

    public ReportServiceImpl(CalculationResultRepository calculationResultRepository,
                             SubjectRuleConfigRepository subjectRuleConfigRepository) {
        this.calculationResultRepository = calculationResultRepository;
        this.subjectRuleConfigRepository = subjectRuleConfigRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportWorkloadWorkbook(String academicYear, String semester) throws IOException {
        List<CalculationResult> results = calculationResultRepository.findAll().stream()
            .filter(result -> matchesTerm(result.getClassRecord(), academicYear, semester))
            .sorted(
                Comparator.comparing((CalculationResult result) -> text(result.getClassRecord().getTeacherName()))
                    .thenComparing(result -> text(result.getClassRecord().getClassName()))
            )
            .toList();
        List<CalculationResult> graduationRows = results.stream().filter(this::isGraduationProject).toList();
        List<CalculationResult> regularRows = results.stream().filter(result -> !isGraduationProject(result)).toList();
        List<CalculationResult> physicalEducationRows = regularRows.stream().filter(this::isPhysicalEducation).toList();
        Map<String, String> teacherDisplayNames = teacherDisplayNames(results);
        Map<String, SubjectRuleConfig> ruleConfigs = subjectRuleConfigRepository.findAll().stream()
            .collect(Collectors.toMap(SubjectRuleConfig::getCode, config -> config));

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ReportStyles styles = createStyles(workbook);
            String reportAcademicYear = resolveAcademicYear(results);
            createSummarySheet(workbook, styles, reportAcademicYear, regularRows, graduationRows, teacherDisplayNames);
            createDetailSheet(workbook, styles, ReportConstant.DETAIL_SHEET_NAME, regularRows, teacherDisplayNames, ruleConfigs);
            createDetailSheet(workbook, styles, ReportConstant.PHYSICAL_EDUCATION_SHEET_NAME, physicalEducationRows, teacherDisplayNames, ruleConfigs);
            createGraduationProjectSheet(workbook, styles, reportAcademicYear, graduationRows, teacherDisplayNames, ruleConfigs);
            workbook.setForceFormulaRecalculation(true);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void createSummarySheet(Workbook workbook, ReportStyles styles, String academicYear, List<CalculationResult> regularRows, List<CalculationResult> graduationRows, Map<String, String> teacherDisplayNames) {
        Sheet sheet = workbook.createSheet(ReportConstant.SUMMARY_SHEET_NAME);
        createTitle(sheet, styles, "BẢNG TỔNG HỢP KHỐI LƯỢNG GIẢNG DẠY", "NĂM HỌC " + academicYear, ReportConstant.SUMMARY_HEADERS.length);
        createHeader(sheet, styles.header(), ReportConstant.REPORT_HEADER_ROW_INDEX, ReportConstant.SUMMARY_HEADERS);

        int rowIndex = ReportConstant.REPORT_FIRST_DATA_ROW_INDEX;
        for (String teacherName : teacherNames(regularRows, graduationRows, teacherDisplayNames)) {
            Row row = sheet.createRow(rowIndex);
            int excelRow = rowIndex + 1;
            String teacherCell = "B" + excelRow;
            writeNumber(row, ReportConstant.SUMMARY_INDEX_COLUMN, rowIndex - ReportConstant.REPORT_FIRST_DATA_ROW_INDEX + 1, styles.integer());
            writeText(row, ReportConstant.SUMMARY_TEACHER_NAME_COLUMN, teacherName, styles.text());
            writeFormula(row, ReportConstant.SUMMARY_CLASS_COUNT_COLUMN,
                "COUNTIF('" + ReportConstant.DETAIL_SHEET_NAME + "'!B:B," + teacherCell + ")",
                styles.integer());
            writeFormula(row, ReportConstant.SUMMARY_TOTAL_CREDITS_COLUMN,
                "SUMIF('" + ReportConstant.DETAIL_SHEET_NAME + "'!B:B," + teacherCell + ",'" + ReportConstant.DETAIL_SHEET_NAME + "'!D:D)",
                styles.number());
            writeFormula(row, ReportConstant.SUMMARY_TOTAL_STUDENTS_COLUMN,
                "SUMIF('" + ReportConstant.DETAIL_SHEET_NAME + "'!B:B," + teacherCell + ",'" + ReportConstant.DETAIL_SHEET_NAME + "'!E:E)",
                styles.integer());
            writeFormula(row, ReportConstant.SUMMARY_TOTAL_STANDARD_HOURS_COLUMN,
                "SUMIF('" + ReportConstant.DETAIL_SHEET_NAME + "'!B:B," + teacherCell + ",'" + ReportConstant.DETAIL_SHEET_NAME + "'!K:K)",
                styles.number());
            writeFormula(row, ReportConstant.SUMMARY_GRADUATION_HOURS_COLUMN,
                "SUMIF('" + ReportConstant.GRADUATION_PROJECT_SHEET_NAME + "'!B:B," + teacherCell + ",'" + ReportConstant.GRADUATION_PROJECT_SHEET_NAME + "'!E:E)",
                styles.number());
            writeFormula(row, ReportConstant.SUMMARY_TOTAL_HOURS_COLUMN,
                "F" + excelRow + "+G" + excelRow,
                styles.number());
            writeText(row, ReportConstant.SUMMARY_NOTE_COLUMN, ReportConstant.EMPTY_CELL_VALUE, styles.text());
            rowIndex++;
        }
        finishSheet(sheet, ReportConstant.SUMMARY_HEADERS.length);
    }

    private void createDetailSheet(Workbook workbook, ReportStyles styles, String sheetName, List<CalculationResult> rows, Map<String, String> teacherDisplayNames, Map<String, SubjectRuleConfig> ruleConfigs) {
        Sheet sheet = workbook.createSheet(sheetName);
        createHeader(sheet, styles.header(), 0, ReportConstant.DETAIL_HEADERS);

        int rowIndex = 1;
        for (CalculationResult result : rows) {
            ClassRecord record = result.getClassRecord();
            Row row = sheet.createRow(rowIndex);
            writeNumber(row, ReportConstant.DETAIL_INDEX_COLUMN, rowIndex, styles.integer());
            writeText(row, ReportConstant.DETAIL_TEACHER_NAME_COLUMN, reportTeacherName(record, teacherDisplayNames), styles.text());
            writeText(row, ReportConstant.DETAIL_CLASS_NAME_COLUMN, record.getClassName(), styles.text());
            writeNumber(row, ReportConstant.DETAIL_CREDITS_COLUMN, value(record.getCredits()), styles.number());
            writeNumber(row, ReportConstant.DETAIL_STUDENT_COUNT_COLUMN, value(record.getStudentCount()), styles.integer());
            if (ReportConstant.PHYSICAL_EDUCATION_SHEET_NAME.equals(sheetName)) {
                writeText(row, ReportConstant.DETAIL_DEPARTMENT_PH_COLUMN, TextNormalizer.cleanDepartmentName(record.getDepartmentPh()), styles.text());
                writeText(row, ReportConstant.DETAIL_UNIT_COLUMN, record.getUnitName(), styles.text());
                writeNullableNumber(row, ReportConstant.DETAIL_COEFFICIENT_K_COLUMN, null, styles.number());
                writeNumber(row, ReportConstant.DETAIL_COEFFICIENT_THEORY_COLUMN, physicalEducationTheoryCoefficient(result), styles.number());
                writeNumber(row, ReportConstant.DETAIL_COEFFICIENT_PRACTICE_COLUMN, physicalEducationPracticeCoefficient(result), styles.number());
                int excelRow = rowIndex + 1;
                SubjectRuleConfig rule = ruleConfigs.get(result.getRuleCode());
                String formula = (rule != null && rule.getFormula() != null && !rule.getFormula().isBlank()) 
                    ? convertToDetailExcelFormula(rule.getFormula(), excelRow) 
                    : "I" + excelRow + "*10+J" + excelRow + "*20";
                writeFormula(row, ReportConstant.DETAIL_STANDARD_HOURS_COLUMN, formula, styles.number());
            } else {
                writeText(row, ReportConstant.DETAIL_DEPARTMENT_PH_COLUMN, TextNormalizer.cleanDepartmentName(record.getDepartmentPh()), styles.text());
                writeText(row, ReportConstant.DETAIL_UNIT_COLUMN, record.getUnitName(), styles.text());
                writeNullableNumber(row, ReportConstant.DETAIL_COEFFICIENT_K_COLUMN, result.getCoefficientK(), styles.number());
                writeNullableNumber(row, ReportConstant.DETAIL_COEFFICIENT_THEORY_COLUMN, result.getCoefficientTheory(), styles.number());
                writeNullableNumber(row, ReportConstant.DETAIL_COEFFICIENT_PRACTICE_COLUMN, result.getCoefficientPractice(), styles.number());
                writeNumber(row, ReportConstant.DETAIL_STANDARD_HOURS_COLUMN, value(result.getStandardHours()), styles.number());
            }
            rowIndex++;
        }
        finishSheet(sheet, ReportConstant.DETAIL_HEADERS.length);
    }

    private void createGraduationProjectSheet(Workbook workbook, ReportStyles styles, String academicYear, List<CalculationResult> rows, Map<String, String> teacherDisplayNames, Map<String, SubjectRuleConfig> ruleConfigs) {
        Sheet sheet = workbook.createSheet(ReportConstant.GRADUATION_PROJECT_SHEET_NAME);
        createTitle(sheet, styles, "BẢNG TỔNG HỢP KHỐI LƯỢNG HỌC PHẦN TỐT NGHIỆP", "NĂM HỌC " + academicYear, ReportConstant.GRADUATION_PROJECT_HEADERS.length);
        createHeader(sheet, styles.header(), ReportConstant.REPORT_HEADER_ROW_INDEX, ReportConstant.GRADUATION_PROJECT_HEADERS);

        int rowIndex = ReportConstant.REPORT_FIRST_DATA_ROW_INDEX;
        int index = 1;
        for (GraduationDepartmentGroup group : graduationSummaries(rows, teacherDisplayNames)) {
            for (GraduationSummary item : group.items()) {
                Row row = sheet.createRow(rowIndex);
                int excelRow = rowIndex + 1;
                writeNumber(row, ReportConstant.GRADUATION_INDEX_COLUMN, index++, styles.integer());
                writeText(row, ReportConstant.GRADUATION_TEACHER_NAME_COLUMN, item.teacherName(), styles.text());
                writeText(row, ReportConstant.GRADUATION_DEPARTMENT_COLUMN, group.departmentName(), styles.text());
                writeNumber(row, ReportConstant.GRADUATION_STUDENT_TOTAL_COLUMN, item.totalStudents(), styles.number());
                SubjectRuleConfig rule = ruleConfigs.get(TeachingRuleConstant.RULE_GRADUATION_PROJECT_CODE);
                String formula = (rule != null && rule.getFormula() != null && !rule.getFormula().isBlank())
                    ? convertToGraduationExcelFormula(rule.getFormula(), excelRow)
                    : "D" + excelRow + "*14";
                writeFormula(row, ReportConstant.GRADUATION_HOURS_COLUMN, formula, styles.number());
                writeText(row, ReportConstant.GRADUATION_NOTE_COLUMN, ReportConstant.EMPTY_CELL_VALUE, styles.text());
                rowIndex++;
            }
        }
        finishSheet(sheet, ReportConstant.GRADUATION_PROJECT_HEADERS.length);
    }

    private List<String> teacherNames(List<CalculationResult> regularRows, List<CalculationResult> graduationRows, Map<String, String> teacherDisplayNames) {
        return java.util.stream.Stream.concat(regularRows.stream(), graduationRows.stream())
            .map(result -> reportTeacherName(result.getClassRecord(), teacherDisplayNames))
            .filter(Objects::nonNull)
            .filter(value -> !value.isBlank())
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    private List<GraduationDepartmentGroup> graduationSummaries(List<CalculationResult> rows, Map<String, String> teacherDisplayNames) {
        Map<String, Map<String, GraduationSummaryBuilder>> grouped = new LinkedHashMap<>();
        rows.stream()
            .sorted(
                Comparator.comparing((CalculationResult result) -> graduationDepartmentName(result.getClassRecord()))
                    .thenComparing(result -> text(result.getClassRecord().getTeacherName()))
            )
            .forEach(result -> {
                ClassRecord record = result.getClassRecord();
                Map<String, GraduationSummaryBuilder> departmentGroup = grouped.computeIfAbsent(
                    graduationDepartmentName(record),
                    key -> new LinkedHashMap<>()
                );
                String teacherName = reportTeacherName(record, teacherDisplayNames);
                GraduationSummaryBuilder builder = departmentGroup.computeIfAbsent(teacherName, GraduationSummaryBuilder::new);
                double convertedStudents = graduationConvertedStudents(result);
                if (isSemesterTwo(record.getSemester())) {
                    builder.hk2Students += convertedStudents;
                } else {
                    builder.hk1Students += convertedStudents;
                }
            });
        return grouped.entrySet().stream()
            .map(entry -> new GraduationDepartmentGroup(
                entry.getKey(),
                entry.getValue().values().stream().map(GraduationSummaryBuilder::build).toList()
            ))
            .toList();
    }

    private void createTitle(Sheet sheet, ReportStyles styles, String title, String yearTitle, int columns) {
        Row titleRow = sheet.createRow(TITLE_ROW_INDEX);
        titleRow.setHeightInPoints(TITLE_HEIGHT_IN_POINTS);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(styles.title());
        sheet.addMergedRegion(new CellRangeAddress(TITLE_ROW_INDEX, TITLE_ROW_INDEX, 0, columns - 1));

        Row yearRow = sheet.createRow(YEAR_ROW_INDEX);
        Cell yearCell = yearRow.createCell(0);
        yearCell.setCellValue(yearTitle);
        yearCell.setCellStyle(styles.subtitle());
        sheet.addMergedRegion(new CellRangeAddress(YEAR_ROW_INDEX, YEAR_ROW_INDEX, 0, columns - 1));
    }

    private static ReportStyles createStyles(Workbook workbook) {
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 15);

        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        Font subtitleFont = workbook.createFont();
        subtitleFont.setBold(true);
        subtitleFont.setFontHeightInPoints((short) 12);

        CellStyle subtitleStyle = workbook.createCellStyle();
        subtitleStyle.setFont(subtitleFont);
        subtitleStyle.setAlignment(HorizontalAlignment.CENTER);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(true);

        CellStyle textStyle = workbook.createCellStyle();
        textStyle.setBorderBottom(BorderStyle.THIN);
        textStyle.setBorderLeft(BorderStyle.THIN);
        textStyle.setBorderRight(BorderStyle.THIN);
        textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        textStyle.setWrapText(true);

        CellStyle integerStyle = workbook.createCellStyle();
        integerStyle.cloneStyleFrom(textStyle);
        integerStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        integerStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.cloneStyleFrom(textStyle);
        numberStyle.setDataFormat(workbook.createDataFormat().getFormat(ReportConstant.NUMBER_FORMAT));
        numberStyle.setAlignment(HorizontalAlignment.CENTER);

        return new ReportStyles(titleStyle, subtitleStyle, headerStyle, textStyle, integerStyle, numberStyle);
    }

    private static void createHeader(Sheet sheet, CellStyle style, int rowIndex, String[] headers) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(28);
        for (int index = 0; index < headers.length; index++) {
            Cell cell = row.createCell(index);
            cell.setCellValue(headers[index]);
            cell.setCellStyle(style);
        }
    }

    private static void writeText(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? ReportConstant.EMPTY_CELL_VALUE : value);
        cell.setCellStyle(style);
    }

    private static void writeNumber(Row row, int column, double value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void writeNullableNumber(Row row, int column, Double value, CellStyle style) {
        if (value == null) {
            writeText(row, column, ReportConstant.EMPTY_CELL_VALUE, style);
            return;
        }
        writeNumber(row, column, value, style);
    }

    private static void writeFormula(Row row, int column, String formula, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellFormula(formula);
        cell.setCellStyle(style);
    }

    private static void finishSheet(Sheet sheet, int columns) {
        sheet.createFreezePane(0, sheet.getLastRowNum() > ReportConstant.REPORT_HEADER_ROW_INDEX ? ReportConstant.REPORT_FIRST_DATA_ROW_INDEX : 1);
        for (int index = 0; index < columns; index++) {
            sheet.autoSizeColumn(index);
            int width = sheet.getColumnWidth(index);
            sheet.setColumnWidth(index, Math.min(Math.max(width, 12 * 256), TEXT_COLUMN_WIDTH * 256));
        }
    }

    private boolean isPhysicalEducation(CalculationResult result) {
        if (TeachingRuleConstant.RULE_PHYSICAL_EDUCATION_CODE.equals(result.getRuleCode())) {
            return true;
        }
        ClassRecord record = result.getClassRecord();
        String departmentHn = TextNormalizer.normalize(record.getDepartmentHn());
        String departmentPh = TextNormalizer.normalize(record.getDepartmentPh());
        return departmentHn.contains(TeachingRuleConstant.DEPARTMENT_PHYSICAL_EDUCATION)
            && (departmentPh.equals(TeachingRuleConstant.DEPARTMENT_GENERAL_SCIENCE)
                || departmentPh.contains(TeachingRuleConstant.DEPARTMENT_PHYSICAL_EDUCATION));
    }

    private boolean isGraduationProject(CalculationResult result) {
        return TeachingRuleConstant.RULE_GRADUATION_PROJECT_CODE.equals(result.getRuleCode());
    }

    private static double graduationConvertedStudents(CalculationResult result) {
        return round(value(result.getStandardHours()) / TeachingRuleConstant.GRADUATION_PROJECT_TECHNICAL_HOURS_PER_STUDENT);
    }

    private static boolean matchesTerm(ClassRecord record, String academicYear, String semester) {
        return matchesValue(record.getAcademicYear(), academicYear) && matchesValue(record.getSemester(), semester);
    }

    private static boolean matchesValue(String actual, String expected) {
        String expectedText = text(expected);
        return expectedText.isBlank() || isAllYear(expectedText) || text(actual).equalsIgnoreCase(expectedText);
    }

    private static boolean isAllYear(String value) {
        String normalizedValue = TextNormalizer.normalize(value);
        return normalizedValue.equals("ca nam") || normalizedValue.equals("all year");
    }

    private static double physicalEducationTheoryCoefficient(CalculationResult result) {
        Double coefficient = result.getCoefficientTheory();
        if (coefficient != null) {
            return coefficient;
        }
        double students = value(result.getClassRecord().getStudentCount());
        return clamp(
            TeachingRuleConstant.PHYSICAL_EDUCATION_THEORY_BASE_K
                + (students - TeachingRuleConstant.PHYSICAL_EDUCATION_BASE_STUDENTS) * TeachingRuleConstant.DEFAULT_INCREMENT_PER_STUDENT,
            TeachingRuleConstant.PHYSICAL_EDUCATION_THEORY_MIN_K,
            TeachingRuleConstant.PHYSICAL_EDUCATION_THEORY_MAX_K
        );
    }

    private static double physicalEducationPracticeCoefficient(CalculationResult result) {
        if (result.getCoefficientPractice() != null) {
            return result.getCoefficientPractice();
        }
        double students = value(result.getClassRecord().getStudentCount());
        return clamp(
            TeachingRuleConstant.PHYSICAL_EDUCATION_PRACTICE_BASE_K
                + (students - TeachingRuleConstant.PHYSICAL_EDUCATION_BASE_STUDENTS) * TeachingRuleConstant.DEFAULT_INCREMENT_PER_STUDENT,
            TeachingRuleConstant.PHYSICAL_EDUCATION_PRACTICE_MIN_K,
            TeachingRuleConstant.PHYSICAL_EDUCATION_PRACTICE_MAX_K
        );
    }

    private static String resolveAcademicYear(List<CalculationResult> results) {
        return results.stream()
            .map(result -> result.getClassRecord().getAcademicYear())
            .filter(Objects::nonNull)
            .filter(value -> !value.isBlank())
            .findFirst()
            .orElse("2025-2026");
    }

    private static boolean isSemesterTwo(String value) {
        return text(value).contains("2");
    }

    private static String graduationDepartmentName(ClassRecord record) {
        String departmentName = text(record.getDepartmentPh());
        if (departmentName.isBlank()) {
            departmentName = text(record.getDepartmentHn());
        }
        departmentName = TextNormalizer.cleanDepartmentName(departmentName);
        if (departmentName.isBlank()) {
            return "Bộ môn";
        }
        String lowerName = departmentName.toLowerCase();
        return lowerName.startsWith("bộ môn") || lowerName.startsWith("bo mon") ? departmentName : "Bộ môn " + departmentName;
    }

    private static Map<String, String> teacherDisplayNames(List<CalculationResult> results) {
        Map<String, String> names = new LinkedHashMap<>();
        results.stream()
            .map(result -> cleanTeacherName(result.getClassRecord()))
            .filter(value -> !value.isBlank())
            .forEach(name -> names.merge(TextNormalizer.normalize(name), name, ReportServiceImpl::preferredTeacherName));
        return names;
    }

    private static String reportTeacherName(ClassRecord record, Map<String, String> teacherDisplayNames) {
        String teacherName = cleanTeacherName(record);
        return teacherDisplayNames.getOrDefault(TextNormalizer.normalize(teacherName), teacherName);
    }

    private static String cleanTeacherName(ClassRecord record) {
        String teacherName = text(record.getTeacherName());
        if (teacherName.isBlank()) {
            teacherName = text(record.getTeacherOriginalName());
        }
        return TextNormalizer.cleanTeacherName(teacherName);
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

    private static long value(Integer value) {
        return value == null ? 0 : value;
    }

    private static double value(Double value) {
        return value == null ? 0.0 : value;
    }

    private static double clamp(double value, double min, double max) {
        return round(Math.max(min, Math.min(max, value)));
    }

    private static double round(double value) {
        return Math.round(value * TeachingRuleConstant.ROUND_SCALE) / TeachingRuleConstant.ROUND_SCALE;
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static String convertToDetailExcelFormula(String formula, int excelRow) {
        return formula.replaceAll("(?i)\\bTC\\b", "D" + excelRow)
                      .replaceAll("(?i)\\bSV\\b", "E" + excelRow)
                      .replaceAll("(?i)\\bK_lt\\b", "I" + excelRow)
                      .replaceAll("(?i)\\bK_th\\b", "J" + excelRow)
                      .replaceAll("(?i)\\bK\\b", "H" + excelRow);
    }

    private static String convertToGraduationExcelFormula(String formula, int excelRow) {
        return formula.replaceAll("(?i)\\bTC\\b", "D" + excelRow)
                      .replaceAll("(?i)\\bSV\\b", "D" + excelRow);
    }

    private record ReportStyles(CellStyle title, CellStyle subtitle, CellStyle header, CellStyle text, CellStyle integer, CellStyle number) {
    }

    private record GraduationSummary(String teacherName, double hk1Students, double hk2Students) {
        private double totalStudents() {
            return round(hk1Students + hk2Students);
        }
    }

    private record GraduationDepartmentGroup(String departmentName, List<GraduationSummary> items) {
    }

    private static final class GraduationSummaryBuilder {
        private final String teacherName;
        private double hk1Students;
        private double hk2Students;

        private GraduationSummaryBuilder(String teacherName) {
            this.teacherName = teacherName;
        }

        private GraduationSummary build() {
            return new GraduationSummary(teacherName, hk1Students, hk2Students);
        }
    }
}
