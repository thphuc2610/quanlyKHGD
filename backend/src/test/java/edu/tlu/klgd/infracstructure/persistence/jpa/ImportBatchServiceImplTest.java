package edu.tlu.klgd.infracstructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.tlu.klgd.domain.entity.ClassRecord;
import edu.tlu.klgd.domain.entity.ImportBatch;
import edu.tlu.klgd.domain.repository.ClassRecordRepository;
import edu.tlu.klgd.domain.repository.ImportBatchRepository;
import edu.tlu.klgd.infracstructure.persistence.mapper.ClassRecordMapper;
import edu.tlu.klgd.infracstructure.persistence.mapper.ImportBatchMapper;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

class ImportBatchServiceImplTest {

    @Test
    void importsStandardWorkbook() throws Exception {
        ImportBatchRepository importBatchRepository = mock(ImportBatchRepository.class);
        ClassRecordRepository classRecordRepository = mock(ClassRecordRepository.class);
        when(importBatchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ImportBatchServiceImpl service = new ImportBatchServiceImpl(
            importBatchRepository,
            classRecordRepository,
            new ImportBatchMapper(),
            new ClassRecordMapper()
        );

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "klgd.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            standardWorkbookBytes()
        );

        service.importWorkbook(file, "2025-2026", "Kỳ 2");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ClassRecord>> recordsCaptor = ArgumentCaptor.forClass(List.class);
        verify(classRecordRepository).saveAll(recordsCaptor.capture());
        List<ClassRecord> records = recordsCaptor.getValue();

        assertThat(records).hasSize(2);
        assertThat(records).extracting(ClassRecord::getTeacherName)
            .containsExactly("nguyen van a", "tran thi b");
    }

    private byte[] standardWorkbookBytes() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Data");
            Row header = sheet.createRow(0);
            List<String> headers = List.of(
                "Lớp học phần",
                "Tên học phần",
                "Tín chỉ",
                "Bộ môn HN",
                "Bộ môn PH",
                "Số sinh viên",
                "Giảng viên",
                "Chức danh",
                "Trình độ"
            );
            for (int i = 0; i < headers.size(); i++) {
                header.createCell(i).setCellValue(headers.get(i));
            }
            Row row1 = sheet.createRow(2);
            row1.createCell(0).setCellValue("Lop A");
            row1.createCell(1).setCellValue("Toan");
            row1.createCell(2).setCellValue(3);
            row1.createCell(3).setCellValue("Toan");
            row1.createCell(4).setCellValue("Toan");
            row1.createCell(5).setCellValue(60);
            row1.createCell(6).setCellValue("Nguyen Van A");

            Row row2 = sheet.createRow(3);
            row2.createCell(0).setCellValue("Lop B");
            row2.createCell(1).setCellValue("Ly");
            row2.createCell(2).setCellValue(2);
            row2.createCell(3).setCellValue("Ly");
            row2.createCell(4).setCellValue("Ly");
            row2.createCell(5).setCellValue(40);
            row2.createCell(6).setCellValue("Tran Thi B");

            workbook.write(output);
            return output.toByteArray();
        }
    }
}
