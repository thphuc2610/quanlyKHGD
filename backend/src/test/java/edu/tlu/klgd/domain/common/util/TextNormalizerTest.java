package edu.tlu.klgd.domain.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextNormalizerTest {
    @Test
    void cleanTeacherNameRemovesAcademicTitleAndOcrDepartmentTail() {
        assertThat(TextNormalizer.cleanTeacherName("TS. Phan Khanh Khánh 2° môn Ky thuật công"))
            .isEqualTo("Phan Khanh Khánh");
        assertThat(TextNormalizer.cleanTeacherName("TS. Phan Khanh Khánh PÔ môn Ky thuật công"))
            .isEqualTo("Phan Khanh Khánh");
        assertThat(TextNormalizer.cleanTeacherName("PGS.TS. Lê Trung Phong"))
            .isEqualTo("Lê Trung Phong");
        assertThat(TextNormalizer.cleanTeacherName("TS. Đặng Đồng Nguyên nguyên nướ..."))
            .isEqualTo("Đặng Đồng Nguyên");
        assertThat(TextNormalizer.cleanTeacherName("Đặng Đồng Nguyên (nguyên nước)"))
            .isEqualTo("Đặng Đồng Nguyên");
        assertThat(TextNormalizer.cleanTeacherName("Nguyễn Bùi Viết Hưng S24-63C"))
            .isEqualTo("Nguyễn Bùi Viết Hưng");
        assertThat(TextNormalizer.cleanTeacherName("Lê Văn Tiên Hưng nguyén nước v..."))
            .isEqualTo("Lê Văn Tiên Hưng");
    }

    @Test
    void cleanDepartmentNameNormalizesMessyDepartmentStrings() {
        assertThat(TextNormalizer.cleanDepartmentName("Bộ môn Kỹ thuật công trình 17"))
            .isEqualTo("Bộ môn Kỹ thuật công trình");
        assertThat(TextNormalizer.cleanDepartmentName("Bộ môn Kỹ thuật công trình 1S"))
            .isEqualTo("Bộ môn Kỹ thuật công trình");
        assertThat(TextNormalizer.cleanDepartmentName("Bộ môn Kỹ thuật công trình INguyễn Bùi Viết Hưng S24-63C"))
            .isEqualTo("Bộ môn Kỹ thuật công trình");
        assertThat(TextNormalizer.cleanDepartmentName("Bộ môn Kỹ thuật công trình là - Đồng Tháp"))
            .isEqualTo("Bộ môn Kỹ thuật công trình");
        assertThat(TextNormalizer.cleanDepartmentName("Bộ môn Kỹ thuật công trình ------"))
            .isEqualTo("Bộ môn Kỹ thuật công trình");
        assertThat(TextNormalizer.cleanDepartmentName("Bộ môn Kỹ thuật tài nguyên nước"))
            .isEqualTo("Bộ môn Kỹ thuật tài nguyên nước");
    }
}
