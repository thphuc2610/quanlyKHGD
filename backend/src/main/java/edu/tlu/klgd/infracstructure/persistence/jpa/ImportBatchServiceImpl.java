package edu.tlu.klgd.infracstructure.persistence.jpa;

import edu.tlu.klgd.application.dto.ClassRecordDTO;
import edu.tlu.klgd.application.dto.ImportBatchDTO;
import edu.tlu.klgd.domain.common.ApiMessage;
import edu.tlu.klgd.domain.common.ImportConstant;
import edu.tlu.klgd.domain.entity.*;
import edu.tlu.klgd.domain.repository.ClassRecordRepository;
import edu.tlu.klgd.domain.repository.ImportBatchRepository;
import edu.tlu.klgd.domain.service.ImportBatchService;
import edu.tlu.klgd.infracstructure.exception.BadRequestException;
import edu.tlu.klgd.infracstructure.exception.ResourceNotFoundException;
import edu.tlu.klgd.infracstructure.persistence.mapper.ClassRecordMapper;
import edu.tlu.klgd.infracstructure.persistence.mapper.ImportBatchMapper;
import edu.tlu.klgd.domain.common.util.TextNormalizer;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportBatchServiceImpl implements ImportBatchService {
    private final ImportBatchRepository importBatchRepository;
    private final ClassRecordRepository classRecordRepository;
    private final ImportBatchMapper importBatchMapper;
    private final ClassRecordMapper classRecordMapper;
    private final ExcelColumnMapper columnMapper = new ExcelColumnMapper();

    public ImportBatchServiceImpl(
        ImportBatchRepository importBatchRepository,
        ClassRecordRepository classRecordRepository,
        ImportBatchMapper importBatchMapper,
        ClassRecordMapper classRecordMapper
    ) {
        this.importBatchRepository = importBatchRepository;
        this.classRecordRepository = classRecordRepository;
        this.importBatchMapper = importBatchMapper;
        this.classRecordMapper = classRecordMapper;
    }

    @Override
    @Transactional
    public ImportBatchDTO importWorkbook(MultipartFile file, String academicYear, String semester) throws IOException {
        ImportBatch batch = new ImportBatch();
        batch.setFileName(file.getOriginalFilename() == null ? ImportConstant.DEFAULT_UPLOAD_FILE_NAME : file.getOriginalFilename());
        batch.setAcademicYear(academicYear);
        batch.setSemester(semester);
        importBatchRepository.save(batch);

        List<ClassRecord> records = parseWorkbook(file.getInputStream(), batch);
        classRecordRepository.saveAll(records);
        batch.setTotalRows(records.size());
        batch.setValidRows((int) records.stream().filter(ClassRecord::isValid).count());
        batch.setWarningRows((int) records.stream().filter(record -> !record.isValid()).count());
        importBatchRepository.save(batch);
        return importBatchMapper.toDto(batch);
    }

    @Override
    public List<ImportBatchDTO> findAll() {
        return importBatchRepository.findAll().stream().map(importBatchMapper::toDto).toList();
    }

    @Override
    public List<ClassRecordDTO> findRecords(Long importBatchId) {
        if (!importBatchRepository.existsById(importBatchId)) {
            throw new ResourceNotFoundException(ApiMessage.RESOURCE_IMPORT_BATCH, importBatchId);
        }
        return classRecordRepository.findByImportBatchId(importBatchId).stream()
            .map(classRecordMapper::toDto)
            .toList();
    }

    @Override
    @Transactional
    public void delete(Long importBatchId) {
        ImportBatch batch = importBatchRepository.findById(importBatchId)
            .orElseThrow(() -> new ResourceNotFoundException(ApiMessage.RESOURCE_IMPORT_BATCH, importBatchId));
        importBatchRepository.delete(batch);
    }

    private List<ClassRecord> parseWorkbook(InputStream inputStream, ImportBatch batch) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            List<ClassRecord> records = new ArrayList<>();
            for (Sheet sheet : workbook) {
                if (sheet.getPhysicalNumberOfRows() <= ImportConstant.HEADER_ROW_MIN_PHYSICAL_ROWS) {
                    continue;
                }
                FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                HeaderMapping headerMapping = findHeaderMapping(sheet, evaluator);
                if (headerMapping == null) {
                    continue;
                }
                for (int rowIndex = headerMapping.rowIndex() + ImportConstant.NEXT_ROW_OFFSET; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row != null && !isEmpty(row) && !isExcludedStatus(row, headerMapping.mapped(), evaluator)) {
                        records.add(toRecord(row, headerMapping.mapped(), batch, evaluator));
                    }
                }
            }
            if (records.isEmpty()) {
                throw new BadRequestException(ApiMessage.VALID_EXCEL_SHEET_NOT_FOUND);
            }
            return records;
        }
    }

    private HeaderMapping findHeaderMapping(Sheet sheet, FormulaEvaluator evaluator) {
        int firstRow = sheet.getFirstRowNum();
        int lastHeaderRow = Math.min(sheet.getLastRowNum(), firstRow + ImportConstant.MAX_HEADER_SCAN_ROWS - 1);
        for (int rowIndex = firstRow; rowIndex <= lastHeaderRow; rowIndex++) {
            Row headerRow = sheet.getRow(rowIndex);
            List<String> headers = readHeaders(headerRow);
            Map<String, Integer> mapped = columnMapper.mapColumns(headers, readSampleRows(sheet, rowIndex, headers.size(), evaluator));
            if (columnMapper.missingRequired(mapped).isEmpty()) {
                return new HeaderMapping(rowIndex, mapped);
            }
        }
        return null;
    }

    private List<List<String>> readSampleRows(Sheet sheet, int headerRowIndex, int columnCount, FormulaEvaluator evaluator) {
        List<List<String>> rows = new ArrayList<>();
        int lastSampleRow = Math.min(sheet.getLastRowNum(), headerRowIndex + 12);
        for (int rowIndex = headerRowIndex + ImportConstant.NEXT_ROW_OFFSET; rowIndex <= lastSampleRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isEmpty(row)) {
                continue;
            }
            List<String> values = new ArrayList<>();
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                values.add(readString(row, columnIndex, evaluator));
            }
            rows.add(values);
        }
        return rows;
    }

    private ClassRecord toRecord(Row row, Map<String, Integer> mapped, ImportBatch batch, FormulaEvaluator evaluator) {
        ClassRecord record = new ClassRecord();
        record.setImportBatch(batch);
        record.setAcademicYear(batch.getAcademicYear());
        record.setSemester(batch.getSemester());
        record.setClassName(readString(row, mapped.get(ImportConstant.CLASS_NAME_FIELD), evaluator));
        record.setSubjectName(readString(row, mapped.get(ImportConstant.SUBJECT_NAME_FIELD), evaluator));
        record.setCredits(readDouble(row, mapped.get(ImportConstant.CREDITS_FIELD), evaluator));
        record.setDepartmentHn(readString(row, mapped.get(ImportConstant.DEPARTMENT_HN_FIELD), evaluator));
        record.setDepartmentPh(readString(row, mapped.get(ImportConstant.DEPARTMENT_PH_FIELD), evaluator));
        Double students = readDouble(row, mapped.get(ImportConstant.STUDENT_COUNT_FIELD), evaluator);
        record.setStudentCount(students == null ? null : students.intValue());
        record.setTeacherOriginalName(readString(row, mapped.get(ImportConstant.TEACHER_ORIGINAL_NAME_FIELD), evaluator));
        record.setTeacherName(normalizeTeacherName(record.getTeacherOriginalName()));
        record.setPosition(readString(row, mapped.get(ImportConstant.POSITION_FIELD), evaluator));
        record.setDegree(readString(row, mapped.get(ImportConstant.DEGREE_FIELD), evaluator));
        record.setAcademicTitle(readString(row, mapped.get(ImportConstant.ACADEMIC_TITLE_FIELD), evaluator));
        record.setUnitName(readString(row, mapped.get(ImportConstant.UNIT_NAME_FIELD), evaluator));
        record.setSourceRowNumber(row.getRowNum() + ImportConstant.NEXT_ROW_OFFSET);
        boolean valid = record.getCredits() != null
            && record.getCredits() > ImportConstant.MIN_VALID_CREDITS
            && record.getStudentCount() != null
            && record.getStudentCount() >= ImportConstant.MIN_VALID_STUDENTS
            && !record.getSubjectName().isBlank()
            && !record.getTeacherOriginalName().isBlank();
        record.setValid(valid);
        if (!valid) {
            record.setWarningMessage(ApiMessage.INVALID_CLASS_RECORD_WARNING);
        }
        return record;
    }

    private List<String> readHeaders(Row headerRow) {
        List<String> headers = new ArrayList<>();
        if (headerRow == null) {
            return headers;
        }
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            headers.add(readString(headerRow, i));
        }
        return headers;
    }

    private boolean isEmpty(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            if (!readString(row, i).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean isExcludedStatus(Row row, Map<String, Integer> mapped, FormulaEvaluator evaluator) {
        String status = TextNormalizer.normalize(readString(row, mapped.get(ImportConstant.STATUS_FIELD), evaluator));
        return ImportConstant.EXCLUDED_STATUS_VALUES.contains(status);
    }

    private String readString(Row row, Integer index) {
        return readString(row, index, null);
    }

    private String readString(Row row, Integer index, FormulaEvaluator evaluator) {
        if (index == null || index < ImportConstant.MIN_VALID_COLUMN_INDEX) {
            return ImportConstant.EMPTY_TEXT;
        }
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return ImportConstant.EMPTY_TEXT;
        }
        DataFormatter formatter = new DataFormatter();
        String value = evaluator == null ? formatter.formatCellValue(cell) : formatter.formatCellValue(cell, evaluator);
        return value.trim()
            .replaceAll(ImportConstant.MULTIPLE_WHITESPACE_REGEX, ImportConstant.SINGLE_SPACE);
    }

    private Double readDouble(Row row, Integer index) {
        return readDouble(row, index, null);
    }

    private Double readDouble(Row row, Integer index, FormulaEvaluator evaluator) {
        String value = readString(row, index, evaluator);
        if (value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.replace(ImportConstant.DECIMAL_COMMA, ImportConstant.DECIMAL_DOT));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeTeacherName(String name) {
        return name == null ? ImportConstant.EMPTY_TEXT : name
            .replaceAll(ImportConstant.GUEST_TEACHER_NAME_REGEX, ImportConstant.EMPTY_TEXT)
            .trim()
            .replaceAll(ImportConstant.MULTIPLE_WHITESPACE_REGEX, ImportConstant.SINGLE_SPACE);
    }

    private record HeaderMapping(int rowIndex, Map<String, Integer> mapped) {
    }
}
