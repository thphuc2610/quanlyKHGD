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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportServiceImpl implements ReportService {
    private static final int TITLE_ROW_INDEX = 0;
    private static final int YEAR_ROW_INDEX = 1;
    private static final int TITLE_HEIGHT_IN_POINTS = 24;
    private static final int TEXT_COLUMN_WIDTH = 34;

    private final CalculationResultRepository calculationResultRepository;

    public ReportServiceImpl(CalculationResultRepository calculationResultRepository) {
        this.calculationResultRepository = calculationResultRepository;
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
        List<CalculationResult> physicalEducationRows = results.stream().filter(this::isPhysicalEducation).toList();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ReportStyles styles = createStyles(workbook);
            String reportAcademicYear = resolveAcademicYear(results);
            createSummarySheet(workbook, styles, reportAcademicYear, results, graduationRows);
            createDetailSheet(workbook, styles, ReportConstant.DETAIL_SHEET_NAME, results);
            createDetailSheet(workbook, styles, ReportConstant.PHYSICAL_EDUCATION_SHEET_NAME, physicalEducationRows);
            createGraduationProjectSheet(workbook, styles, reportAcademicYear, graduationRows);
            workbook.setForceFormulaRecalculation(true);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void createSummarySheet(Workbook workbook, ReportStyles styles, String academicYear, List<CalculationResult> regularRows, List<CalculationResult> graduationRows) {
        Sheet sheet = workbook.createSheet(ReportConstant.SUMMARY_SHEET_NAME);
        createTitle(sheet, styles, "BẢNG TỔNG HỢP KHỐI LƯỢNG GIẢNG DẠY", "NĂM HỌC " + academicYear, ReportConstant.SUMMARY_HEADERS.length);
        createHeader(sheet, styles.header(), ReportConstant.REPORT_HEADER_ROW_INDEX, ReportConstant.SUMMARY_HEADERS);

        int rowIndex = ReportConstant.REPORT_FIRST_DATA_ROW_INDEX;
        for (String teacherName : teacherNames(regularRows, graduationRows)) {
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
                "SUMIF('" + ReportConstant.DETAIL_SHEET_NAME + "'!B:B," + teacherCell + ",'" + ReportConstant.DETAIL_SHEET_NAME + "'!J:J)",
                styles.number());
            writeFormula(row, ReportConstant.SUMMARY_GRADUATION_HOURS_COLUMN,
                "SUMIF('" + ReportConstant.GRADUATION_PROJECT_SHEET_NAME + "'!B:B," + teacherCell + ",'" + ReportConstant.GRADUATION_PROJECT_SHEET_NAME + "'!D:D)",
                styles.number());
            writeFormula(row, ReportConstant.SUMMARY_TOTAL_HOURS_COLUMN,
                "F" + excelRow + "+G" + excelRow,
                styles.number());
            writeText(row, ReportConstant.SUMMARY_NOTE_COLUMN, ReportConstant.EMPTY_CELL_VALUE, styles.text());
            rowIndex++;
        }
        finishSheet(sheet, ReportConstant.SUMMARY_HEADERS.length);
    }

    private void createDetailSheet(Workbook workbook, ReportStyles styles, String sheetName, List<CalculationResult> rows) {
        Sheet sheet = workbook.createSheet(sheetName);
        createHeader(sheet, styles.header(), 0, ReportConstant.DETAIL_HEADERS);

        int rowIndex = 1;
        for (CalculationResult result : rows) {
            ClassRecord record = result.getClassRecord();
            Row row = sheet.createRow(rowIndex);
            writeNumber(row, ReportConstant.DETAIL_INDEX_COLUMN, rowIndex, styles.integer());
            writeText(row, ReportConstant.DETAIL_TEACHER_NAME_COLUMN, record.getTeacherName(), styles.text());
            writeText(row, ReportConstant.DETAIL_CLASS_NAME_COLUMN, record.getClassName(), styles.text());
            writeNumber(row, ReportConstant.DETAIL_CREDITS_COLUMN, value(record.getCredits()), styles.number());
            writeNumber(row, ReportConstant.DETAIL_STUDENT_COUNT_COLUMN, value(record.getStudentCount()), styles.integer());
            if (ReportConstant.PHYSICAL_EDUCATION_SHEET_NAME.equals(sheetName)) {
                writeText(row, ReportConstant.DETAIL_DEPARTMENT_PH_COLUMN, record.getDepartmentPh(), styles.text());
                writeText(row, ReportConstant.DETAIL_UNIT_COLUMN, record.getUnitName(), styles.text());
                writeNumber(row, ReportConstant.DETAIL_COEFFICIENT_THEORY_COLUMN, physicalEducationTheoryCoefficient(result), styles.number());
                writeNumber(row, ReportConstant.DETAIL_COEFFICIENT_PRACTICE_COLUMN, physicalEducationPracticeCoefficient(result), styles.number());
                int excelRow = rowIndex + 1;
                writeFormula(row, ReportConstant.DETAIL_STANDARD_HOURS_COLUMN, "H" + excelRow + "*10+I" + excelRow + "*20", styles.number());
            } else {
                writeText(row, ReportConstant.DETAIL_DEPARTMENT_PH_COLUMN, record.getDepartmentPh(), styles.text());
                writeText(row, ReportConstant.DETAIL_UNIT_COLUMN, record.getUnitName(), styles.text());
                writeNullableNumber(row, ReportConstant.DETAIL_COEFFICIENT_THEORY_COLUMN, coefficientTheory(result), styles.number());
                writeNullableNumber(row, ReportConstant.DETAIL_COEFFICIENT_PRACTICE_COLUMN, result.getCoefficientPractice(), styles.number());
                writeNumber(row, ReportConstant.DETAIL_STANDARD_HOURS_COLUMN, value(result.getStandardHours()), styles.number());
            }
            rowIndex++;
        }
        finishSheet(sheet, ReportConstant.DETAIL_HEADERS.length);
    }

    private void createGraduationProjectSheet(Workbook workbook, ReportStyles styles, String academicYear, List<CalculationResult> rows) {
        Sheet sheet = workbook.createSheet(ReportConstant.GRADUATION_PROJECT_SHEET_NAME);
        createTitle(sheet, styles, "BẢNG TỔNG HỢP KHỐI LƯỢNG HỌC PHẦN TỐT NGHIỆP", "NĂM HỌC " + academicYear, ReportConstant.GRADUATION_PROJECT_HEADERS.length);
        createHeader(sheet, styles.header(), ReportConstant.REPORT_HEADER_ROW_INDEX, ReportConstant.GRADUATION_PROJECT_HEADERS);
        Row headerRow = sheet.getRow(ReportConstant.REPORT_HEADER_ROW_INDEX);
        writeText(headerRow, ReportConstant.GRADUATION_HK1_COLUMN, ReportConstant.EMPTY_CELL_VALUE, styles.header());
        writeText(headerRow, ReportConstant.GRADUATION_HK2_COLUMN, ReportConstant.EMPTY_CELL_VALUE, styles.header());

        int rowIndex = ReportConstant.REPORT_FIRST_DATA_ROW_INDEX;
        int index = 1;
        for (GraduationDepartmentGroup group : graduationSummaries(rows)) {
            Row groupRow = sheet.createRow(rowIndex++);
            writeText(groupRow, ReportConstant.GRADUATION_INDEX_COLUMN, group.departmentName(), styles.header());
            writeText(groupRow, ReportConstant.GRADUATION_HK1_COLUMN, "HK1", styles.header());
            writeText(groupRow, ReportConstant.GRADUATION_HK2_COLUMN, "HK2", styles.header());

            for (GraduationSummary item : group.items()) {
                Row row = sheet.createRow(rowIndex);
                int excelRow = rowIndex + 1;
                writeNumber(row, ReportConstant.GRADUATION_INDEX_COLUMN, index++, styles.integer());
                writeText(row, ReportConstant.GRADUATION_TEACHER_NAME_COLUMN, item.teacherName(), styles.text());
                writeFormula(row, ReportConstant.GRADUATION_STUDENT_TOTAL_COLUMN, "G" + excelRow + "+I" + excelRow, styles.number());
                writeFormula(row, ReportConstant.GRADUATION_HOURS_COLUMN, "C" + excelRow + "*14", styles.number());
                writeText(row, ReportConstant.GRADUATION_NOTE_COLUMN, ReportConstant.EMPTY_CELL_VALUE, styles.text());
                writeNumber(row, ReportConstant.GRADUATION_HK1_COLUMN, item.hk1Students(), styles.number());
                writeNumber(row, ReportConstant.GRADUATION_HK2_COLUMN, item.hk2Students(), styles.number());
                rowIndex++;
            }
        }
        finishSheet(sheet, ReportConstant.GRADUATION_PROJECT_HEADERS.length);
    }

    private List<String> teacherNames(List<CalculationResult> regularRows, List<CalculationResult> graduationRows) {
        return java.util.stream.Stream.concat(regularRows.stream(), graduationRows.stream())
            .map(result -> result.getClassRecord().getTeacherName())
            .filter(Objects::nonNull)
            .filter(value -> !value.isBlank())
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    private List<GraduationDepartmentGroup> graduationSummaries(List<CalculationResult> rows) {
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
                GraduationSummaryBuilder builder = departmentGroup.computeIfAbsent(record.getTeacherName(), GraduationSummaryBuilder::new);
                if (isSemesterTwo(record.getSemester())) {
                    builder.hk2Students += value(record.getStudentCount());
                } else {
                    builder.hk1Students += value(record.getStudentCount());
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
        return TeachingRuleConstant.RULE_GRADUATION_INTERNSHIP_CODE.equals(result.getRuleCode());
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

    private static Double coefficientTheory(CalculationResult result) {
        return result.getCoefficientTheory() == null ? result.getCoefficientK() : result.getCoefficientTheory();
    }

    private static double physicalEducationTheoryCoefficient(CalculationResult result) {
        Double coefficient = coefficientTheory(result);
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
        if (departmentName.isBlank()) {
            return "Bộ môn";
        }
        String lowerName = departmentName.toLowerCase();
        return lowerName.startsWith("bộ môn") || lowerName.startsWith("bo mon") ? departmentName : "Bộ môn " + departmentName;
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

    private record ReportStyles(CellStyle title, CellStyle subtitle, CellStyle header, CellStyle text, CellStyle integer, CellStyle number) {
    }

    private record GraduationSummary(String teacherName, double hk1Students, double hk2Students) {
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
