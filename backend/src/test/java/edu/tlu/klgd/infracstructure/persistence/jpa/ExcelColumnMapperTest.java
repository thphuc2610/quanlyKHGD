package edu.tlu.klgd.infracstructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import edu.tlu.klgd.domain.common.ImportConstant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExcelColumnMapperTest {
    private final ExcelColumnMapper mapper = new ExcelColumnMapper();

    @Test
    void mapsStudentCountAndStatusFromClassReportHeaders() {
        List<String> headers = List.of(
            "STT",
            "Ma lop",
            "Ten lop",
            "Ten mon",
            "So tin chi",
            "Ten bo mon",
            "So SV dang ky",
            "Khoa",
            "Giang vien",
            "Trang thai"
        );
        List<List<String>> sampleRows = List.of(
            List.of("1", "251002_BC111_7", "Bong chuyen", "Bong chuyen 1", "1", "Bo mon GDTC", "60", "K66", "", "Chot"),
            List.of("2", "251002_BR111_1", "Bong ro", "Bong ro", "1", "Bo mon GDTC", "53", "K66", "Duong Thanh Tan", "Chot")
        );

        Map<String, Integer> mapped = mapper.mapColumns(headers, sampleRows);

        assertThat(mapped.get(ImportConstant.CLASS_NAME_FIELD)).isEqualTo(2);
        assertThat(mapped.get(ImportConstant.CREDITS_FIELD)).isEqualTo(4);
        assertThat(mapped.get(ImportConstant.STUDENT_COUNT_FIELD)).isEqualTo(6);
        assertThat(mapped.get(ImportConstant.TEACHER_ORIGINAL_NAME_FIELD)).isEqualTo(8);
        assertThat(mapped.get(ImportConstant.STATUS_FIELD)).isEqualTo(9);
    }

    @Test
    void doesNotMatchShortAliasesInsideUnrelatedHeaders() {
        List<String> headers = List.of(
            "STT",
            "Ten lop",
            "Ten mon",
            "So tin chi",
            "Ten bo mon",
            "Ma SV mau",
            "So SV dang ky",
            "Ten giang vien"
        );
        List<List<String>> sampleRows = List.of(
            List.of("1", "Lop A", "Mon A", "3", "Bo mon A", "SV-001", "45", "Nguyen Van A")
        );

        Map<String, Integer> mapped = mapper.mapColumns(headers, sampleRows);

        assertThat(mapped.get(ImportConstant.STUDENT_COUNT_FIELD)).isEqualTo(6);
        assertThat(mapped.get(ImportConstant.TEACHER_ORIGINAL_NAME_FIELD)).isEqualTo(7);
    }
}
