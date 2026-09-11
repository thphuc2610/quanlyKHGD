package edu.tlu.klgd.infracstructure.persistence.jpa;

import edu.tlu.klgd.application.dto.ClassRecordDTO;
import edu.tlu.klgd.application.dto.ImportBatchDTO;
import edu.tlu.klgd.domain.common.ApiMessage;
import edu.tlu.klgd.domain.common.ImportConstant;
import edu.tlu.klgd.domain.common.TeachingRuleConstant;
import edu.tlu.klgd.domain.common.util.ExcelHelper;
import edu.tlu.klgd.domain.common.util.TextNormalizer;
import edu.tlu.klgd.domain.entity.*;
import edu.tlu.klgd.domain.repository.ClassRecordRepository;
import edu.tlu.klgd.domain.repository.ImportBatchRepository;
import edu.tlu.klgd.domain.service.ImportBatchService;
import edu.tlu.klgd.infracstructure.exception.BadRequestException;
import edu.tlu.klgd.infracstructure.exception.ResourceNotFoundException;
import edu.tlu.klgd.infracstructure.persistence.mapper.ClassRecordMapper;
import edu.tlu.klgd.infracstructure.persistence.mapper.ImportBatchMapper;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportBatchServiceImpl implements ImportBatchService {
    private static final Pattern STUDENT_CODE_PATTERN = Pattern.compile("\\b\\d{10}\\b");
    private static final Pattern TEACHER_TITLE_PATTERN = Pattern.compile("(?iu)(PGS\\.?\\s*TS\\.?|GS\\.?\\s*TS\\.?|TS\\.?|ThS\\.?)\\s+.+");
    private static final Pattern CLASS_CODE_PATTERN = Pattern.compile("(?i)^[S$]\\d{2}[-–]\\d{2}[A-Z0-9]+.*");
    private static final int PDF_RENDER_DPI = 220;
    private static final int OCR_START_PAGE_INDEX = 1;

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

        List<ClassRecord> records = ExcelHelper.hasPdfFormat(file)
            ? parseGraduationProjectPdf(file.getBytes(), batch)
            : parseWorkbook(file.getInputStream(), batch);
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

    private List<ClassRecord> parseGraduationProjectPdf(byte[] pdfBytes, ImportBatch batch) throws IOException {
        Path workDir = Files.createTempDirectory("hptn-ocr-");
        try {
            List<ClassRecord> records = new ArrayList<>();
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                PDFRenderer renderer = new PDFRenderer(document);
                for (int pageIndex = OCR_START_PAGE_INDEX; pageIndex < document.getNumberOfPages(); pageIndex++) {
                    BufferedImage pageImage = renderer.renderImageWithDPI(pageIndex, PDF_RENDER_DPI);
                    BufferedImage rotated = rotateClockwise(pageImage);
                    Path imagePath = workDir.resolve("page-" + (pageIndex + 1) + ".png");
                    ImageIO.write(rotated, "png", imagePath.toFile());
                    String ocrText = runTesseract(imagePath);
                    records.addAll(parseGraduationProjectOcrText(ocrText, batch, pageIndex + 1));
                }
            }
            if (records.isEmpty()) {
                throw new BadRequestException("Không đọc được danh sách HPTN từ PDF OCR. Vui lòng dùng file Excel HPTN hoặc PDF scan rõ hơn.");
            }
            return records;
        } finally {
            deleteDirectory(workDir);
        }
    }

    private BufferedImage rotateClockwise(BufferedImage source) {
        int imageType = source.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_RGB : source.getType();
        BufferedImage rotated = new BufferedImage(source.getHeight(), source.getWidth(), imageType);
        Graphics2D graphics = rotated.createGraphics();
        graphics.translate(source.getHeight(), 0);
        graphics.rotate(Math.PI / 2);
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return rotated;
    }

    private String runTesseract(Path imagePath) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(tesseractExecutable());
        command.add(imagePath.toString());
        command.add("stdout");
        Optional<Path> tessdata = tessdataPath();
        if (tessdata.isPresent()) {
            command.add("--tessdata-dir");
            command.add(tessdata.get().toString());
        }
        command.add("-l");
        command.add("vie+eng");
        command.add("--psm");
        command.add("11");
        Process process = new ProcessBuilder(command)
            .redirectErrorStream(true)
            .start();
        try {
            byte[] output = process.getInputStream().readAllBytes();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new BadRequestException("Không chạy được OCR Tesseract: " + new String(output, StandardCharsets.UTF_8));
            }
            return new String(output, StandardCharsets.UTF_8);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("OCR Tesseract bị gián đoạn.");
        }
    }

    private String tesseractExecutable() {
        Path windowsDefault = Path.of("C:", "Program Files", "Tesseract-OCR", "tesseract.exe");
        return Files.exists(windowsDefault) ? windowsDefault.toString() : "tesseract";
    }

    private Optional<Path> tessdataPath() {
        Path cwd = Path.of(System.getProperty("user.dir"));
        List<Path> candidates = new ArrayList<>();
        candidates.add(cwd.resolve(".ocr").resolve("tessdata"));
        if (cwd.getParent() != null) {
            candidates.add(cwd.getParent().resolve(".ocr").resolve("tessdata"));
        }
        return candidates.stream()
            .filter(path -> Files.exists(path.resolve("vie.traineddata")))
            .findFirst();
    }

    private List<ClassRecord> parseGraduationProjectOcrText(String ocrText, ImportBatch batch, int pageNumber) {
        List<String> lines = Arrays.stream(ocrText.split("\\R"))
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .toList();
        List<Integer> studentLineIndexes = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (STUDENT_CODE_PATTERN.matcher(lines.get(i)).find()) {
                studentLineIndexes.add(i);
            }
        }
        List<ClassRecord> records = new ArrayList<>();
        for (int i = 0; i < studentLineIndexes.size(); i++) {
            int start = studentLineIndexes.get(i);
            int end = i + 1 < studentLineIndexes.size() ? studentLineIndexes.get(i + 1) : lines.size();
            List<String> block = lines.subList(start, end);
            records.addAll(toGraduationProjectRecordsFromOcrBlock(block, batch, pageNumber));
        }
        return records;
    }

    private List<ClassRecord> toGraduationProjectRecordsFromOcrBlock(List<String> block, ImportBatch batch, int pageNumber) {
        if (block.isEmpty()) {
            return List.of();
        }
        Matcher codeMatcher = STUDENT_CODE_PATTERN.matcher(block.getFirst());
        if (!codeMatcher.find()) {
            return List.of();
        }
        String studentCode = codeMatcher.group();
        String studentName = ocrStudentName(block, codeMatcher.end());
        if (studentName.isBlank()) {
            studentName = studentCode;
        }
        String className = block.stream()
            .map(this::stripOcrPipes)
            .filter(line -> CLASS_CODE_PATTERN.matcher(line).matches())
            .findFirst()
            .orElse("");
        String department = ocrDepartment(block);
        List<String> teachers = ocrTeachers(block);
        if (teachers.isEmpty()) {
            return List.of();
        }
        double advisorShare = 1.0 / teachers.size();
        List<ClassRecord> records = new ArrayList<>();
        for (String teacher : teachers) {
            ClassRecord record = new ClassRecord();
            record.setImportBatch(batch);
            record.setAcademicYear(batch.getAcademicYear());
            record.setSemester(batch.getSemester());
            record.setClassName("HPTN PDF - " + studentName + (className.isBlank() ? "" : " - " + className));
            record.setSubjectName(TeachingRuleConstant.RULE_GRADUATION_PROJECT_NAME);
            record.setCredits(advisorShare);
            record.setDepartmentHn(department.isBlank() ? TeachingRuleConstant.RULE_GRADUATION_PROJECT_NAME : department);
            record.setDepartmentPh(department.isBlank() ? TeachingRuleConstant.RULE_GRADUATION_PROJECT_NAME : department);
            record.setStudentCount(1);
            record.setTeacherOriginalName(teacher);
            record.setTeacherName(normalizeTeacherName(teacher));
            record.setUnitName(department);
            record.setSourceRowNumber(pageNumber);
            record.setValid(true);
            records.add(record);
        }
        return records;
    }

    private String ocrStudentName(List<String> block, int codeEndIndex) {
        String firstLine = block.getFirst();
        if (codeEndIndex < firstLine.length()) {
            String candidate = stripOcrPipes(firstLine.substring(codeEndIndex));
            if (!candidate.isBlank() && !CLASS_CODE_PATTERN.matcher(candidate).matches()) {
                return candidate;
            }
        }
        for (int i = 1; i < Math.min(block.size(), 5); i++) {
            String candidate = stripOcrPipes(block.get(i));
            if (!candidate.isBlank()
                && !CLASS_CODE_PATTERN.matcher(candidate).matches()
                && !TextNormalizer.normalize(candidate).contains("bo mon")
                && !looksLikeTeacher(candidate)) {
                return candidate;
            }
        }
        return "";
    }

    private List<String> ocrTeachers(List<String> block) {
        LinkedHashMap<String, String> teachers = new LinkedHashMap<>();
        for (String line : block) {
            String candidate = stripOcrPipes(line);
            if (!looksLikeTeacher(candidate) || TextNormalizer.normalize(candidate).contains("bo mon")) {
                continue;
            }
            String cleaned = cleanupOcrTeacher(candidate);
            if (!cleaned.isBlank()) {
                teachers.putIfAbsent(TextNormalizer.normalize(cleaned), cleaned);
            }
        }
        return new ArrayList<>(teachers.values());
    }

    private boolean looksLikeTeacher(String value) {
        return TEACHER_TITLE_PATTERN.matcher(value).find();
    }

    private String cleanupOcrTeacher(String value) {
        String cleaned = value
            .replaceAll("(?iu)\\btrình\\b.*$", "")
            .replaceAll("(?iu)\\bthiết\\s+kế\\b.*$", "")
            .replaceAll("(?iu)\\bbộ\\s+môn\\b.*$", "")
            .replaceAll("\\s+", " ")
            .trim();
        Matcher matcher = TEACHER_TITLE_PATTERN.matcher(cleaned);
        return matcher.find() ? matcher.group().trim() : "";
    }

    private String ocrDepartment(List<String> block) {
        for (int i = 0; i < block.size(); i++) {
            String normalized = TextNormalizer.normalize(block.get(i));
            if (normalized.contains("bo mon")) {
                StringBuilder department = new StringBuilder(stripOcrPipes(block.get(i)));
                for (int j = i + 1; j < Math.min(block.size(), i + 3); j++) {
                    String next = stripOcrPipes(block.get(j));
                    if (looksLikeTeacher(next) || STUDENT_CODE_PATTERN.matcher(next).find()) {
                        break;
                    }
                    if (!next.isBlank() && !CLASS_CODE_PATTERN.matcher(next).matches()) {
                        department.append(' ').append(next);
                    }
                }
                return department.toString().replaceAll("\\s+", " ").trim();
            }
        }
        return "";
    }

    private String stripOcrPipes(String value) {
        return value
            .replace('|', ' ')
            .replace('[', ' ')
            .replace(']', ' ')
            .replaceAll("\\s+", " ")
            .trim();
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
        }
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
        record.setDepartmentHn(TextNormalizer.cleanDepartmentName(readString(row, mapped.get(ImportConstant.DEPARTMENT_HN_FIELD), evaluator)));
        record.setDepartmentPh(TextNormalizer.cleanDepartmentName(readString(row, mapped.get(ImportConstant.DEPARTMENT_PH_FIELD), evaluator)));
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
        return TextNormalizer.cleanTeacherName(name);
    }

    private record HeaderMapping(int rowIndex, Map<String, Integer> mapped) {
    }
}
