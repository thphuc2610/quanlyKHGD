package edu.tlu.klgd.infracstructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.tlu.klgd.domain.common.ReportConstant;
import edu.tlu.klgd.domain.common.TeachingRuleConstant;
import edu.tlu.klgd.domain.entity.CalculationResult;
import edu.tlu.klgd.domain.entity.ClassRecord;
import edu.tlu.klgd.domain.repository.CalculationResultRepository;
import edu.tlu.klgd.domain.repository.SubjectRuleConfigRepository;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ReportServiceImplTest {
    @Test
    void exportMergesTeacherNameVariantsWithNoiseAndAccentErrors() throws Exception {
        CalculationResultRepository repository = mock(CalculationResultRepository.class);
        when(repository.findAll()).thenReturn(List.of(
            result(record("Lé Trung Phong", "Ket cau 1", "Kỳ 1"), "LY_THUYET_BAI_TAP", 10.0),
            result(record(". Lê Trung Phong", "Ket cau 2", "Kỳ 1"), "LY_THUYET_BAI_TAP", 20.0),
            result(record("ThS. Lé Trung Phong", "HPTN", "Kỳ 2"), TeachingRuleConstant.RULE_GRADUATION_PROJECT_CODE, 14.0)
        ));
        SubjectRuleConfigRepository configRepository = mock(SubjectRuleConfigRepository.class);
        ReportServiceImpl service = new ReportServiceImpl(repository, configRepository);

        byte[] workbookBytes = service.exportWorkloadWorkbook("2025-2026", "");

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(workbookBytes))) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            Sheet summary = workbook.getSheet(ReportConstant.SUMMARY_SHEET_NAME);
            Sheet graduation = workbook.getSheet(ReportConstant.GRADUATION_PROJECT_SHEET_NAME);

            assertThat(namesInColumn(summary, 1, formatter, evaluator))
                .containsExactly("Lê Trung Phong");
            assertThat(namesInColumn(graduation, 1, formatter, evaluator))
                .containsExactly("Lê Trung Phong");

            Row summaryTeacherRow = findRowByCellValue(summary, 1, "Lê Trung Phong", formatter, evaluator);
            assertThat(formatter.formatCellValue(summaryTeacherRow.getCell(5), evaluator)).isEqualTo("30.00");
            assertThat(formatter.formatCellValue(summaryTeacherRow.getCell(6), evaluator)).isEqualTo("14.00");
        }
    }

    @Test
    void exportPrefersVietnameseAccentVariantWhenTeacherNamesShareSameKey() throws Exception {
        CalculationResultRepository repository = mock(CalculationResultRepository.class);
        when(repository.findAll()).thenReturn(List.of(
            result(record("ThS. Phan Th\u1ECB Tai", "Ket cau 1", "Kỳ 1"), "LY_THUYET_BAI_TAP", 10.0),
            result(record("ThS. Phan Th\u1ECB T\u00e0i", "Ket cau 2", "Kỳ 1"), "LY_THUYET_BAI_TAP", 20.0)
        ));
        SubjectRuleConfigRepository configRepository = mock(SubjectRuleConfigRepository.class);
        ReportServiceImpl service = new ReportServiceImpl(repository, configRepository);

        byte[] workbookBytes = service.exportWorkloadWorkbook("2025-2026", "");

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(workbookBytes))) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            Sheet summary = workbook.getSheet(ReportConstant.SUMMARY_SHEET_NAME);

            assertThat(namesInColumn(summary, 1, formatter, evaluator))
                .containsExactly("Phan Th\u1ECB T\u00e0i");
        }
    }

    @Test
    void exportUsesNormalizedTeacherNamesForGraduationProjectSheetAndSummary() throws Exception {
        CalculationResultRepository repository = mock(CalculationResultRepository.class);
        when(repository.findAll()).thenReturn(List.of(
            result(record("Nguyen Vinh Sang", "Ket cau", "Kỳ 1"), "LY_THUYET_BAI_TAP", 30.0),
            result(record("ThS. Nguyen Vinh Sang", "HPTN", "Kỳ 1"), TeachingRuleConstant.RULE_GRADUATION_PROJECT_CODE, 14.0),
            result(record("TS. Nguyen Vinh Sang", "HPTN", "Kỳ 2"), TeachingRuleConstant.RULE_GRADUATION_PROJECT_CODE, 28.0)
        ));
        SubjectRuleConfigRepository configRepository = mock(SubjectRuleConfigRepository.class);
        ReportServiceImpl service = new ReportServiceImpl(repository, configRepository);

        byte[] workbookBytes = service.exportWorkloadWorkbook("2025-2026", "");

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(workbookBytes))) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            Sheet summary = workbook.getSheet(ReportConstant.SUMMARY_SHEET_NAME);
            Sheet graduation = workbook.getSheet(ReportConstant.GRADUATION_PROJECT_SHEET_NAME);

            assertThat(namesInColumn(summary, 1, formatter, evaluator))
                .containsExactly("Nguyen Vinh Sang");
            assertThat(namesInColumn(graduation, 1, formatter, evaluator))
                .contains("Nguyen Vinh Sang")
                .doesNotContain("ThS. Nguyen Vinh Sang", "TS. Nguyen Vinh Sang");

            Row graduationHeader = graduation.getRow(ReportConstant.REPORT_HEADER_ROW_INDEX);
            assertThat(formatter.formatCellValue(graduationHeader.getCell(0), evaluator)).isEqualTo("STT");
            assertThat(formatter.formatCellValue(graduationHeader.getCell(1), evaluator)).isEqualTo("Tên giảng viên");
            assertThat(formatter.formatCellValue(graduationHeader.getCell(2), evaluator)).isEqualTo("Tên bộ môn");
            assertThat(formatter.formatCellValue(graduationHeader.getCell(3), evaluator)).isEqualTo("SV quy đổi");
            assertThat(formatter.formatCellValue(graduationHeader.getCell(4), evaluator)).isEqualTo("Giờ chuẩn");
            assertThat(formatter.formatCellValue(graduationHeader.getCell(5), evaluator)).isEqualTo("Ghi chú");

            Row summaryTeacherRow = findRowByCellValue(summary, 1, "Nguyen Vinh Sang", formatter, evaluator);
            assertThat(formatter.formatCellValue(summaryTeacherRow.getCell(6), evaluator)).isEqualTo("42.00");

            Row graduationTeacherRow = findRowByCellValue(graduation, 1, "Nguyen Vinh Sang", formatter, evaluator);
            assertThat(formatter.formatCellValue(graduationTeacherRow.getCell(0), evaluator)).isEqualTo("1");
            assertThat(formatter.formatCellValue(graduationTeacherRow.getCell(2), evaluator)).isEqualTo("Bộ môn Xây dựng");
            assertThat(formatter.formatCellValue(graduationTeacherRow.getCell(3), evaluator)).isEqualTo("3.00");
            assertThat(formatter.formatCellValue(graduationTeacherRow.getCell(4), evaluator)).isEqualTo("42.00");
        }
    }

    private static List<String> namesInColumn(Sheet sheet, int column, DataFormatter formatter, FormulaEvaluator evaluator) {
        return java.util.stream.StreamSupport.stream(sheet.spliterator(), false)
            .filter(row -> row.getRowNum() >= ReportConstant.REPORT_FIRST_DATA_ROW_INDEX)
            .map(row -> formatter.formatCellValue(row.getCell(column), evaluator))
            .filter(value -> !value.isBlank())
            .filter(value -> !value.startsWith("Bộ môn"))
            .toList();
    }

    private static Row findRowByCellValue(Sheet sheet, int column, String expected, DataFormatter formatter, FormulaEvaluator evaluator) {
        return java.util.stream.StreamSupport.stream(sheet.spliterator(), false)
            .filter(row -> expected.equals(formatter.formatCellValue(row.getCell(column), evaluator)))
            .findFirst()
            .orElseThrow();
    }

    private static CalculationResult result(ClassRecord record, String ruleCode, double standardHours) {
        CalculationResult result = new CalculationResult();
        result.setClassRecord(record);
        result.setRuleCode(ruleCode);
        result.setStandardHours(standardHours);
        return result;
    }

    private static ClassRecord record(String teacherName, String subjectName, String semester) {
        ClassRecord record = new ClassRecord();
        record.setTeacherName(teacherName);
        record.setTeacherOriginalName(teacherName);
        record.setSubjectName(subjectName);
        record.setClassName(subjectName);
        record.setAcademicYear("2025-2026");
        record.setSemester(semester);
        record.setCredits(3.0);
        record.setStudentCount(40);
        record.setDepartmentPh("Xay dung");
        return record;
    }
}
