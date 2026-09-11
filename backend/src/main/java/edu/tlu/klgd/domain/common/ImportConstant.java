package edu.tlu.klgd.domain.common;

import java.util.List;
import java.util.Map;

public final class ImportConstant {
    private ImportConstant() {
    }

    public static final String DEFAULT_UPLOAD_FILE_NAME = "upload.xlsx";
    public static final String EXCEL_XLSX_EXTENSION = ".xlsx";
    public static final String EXCEL_XLSX_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String PDF_EXTENSION = ".pdf";
    public static final String PDF_MEDIA_TYPE = "application/pdf";
    public static final String CLASS_NAME_FIELD = "className";
    public static final String SUBJECT_NAME_FIELD = "subjectName";
    public static final String CREDITS_FIELD = "credits";
    public static final String DEPARTMENT_HN_FIELD = "departmentHn";
    public static final String DEPARTMENT_PH_FIELD = "departmentPh";
    public static final String STUDENT_COUNT_FIELD = "studentCount";
    public static final String STATUS_FIELD = "status";
    public static final String TEACHER_ORIGINAL_NAME_FIELD = "teacherOriginalName";
    public static final String POSITION_FIELD = "position";
    public static final String DEGREE_FIELD = "degree";
    public static final String ACADEMIC_TITLE_FIELD = "academicTitle";
    public static final String UNIT_NAME_FIELD = "unitName";
    public static final String DECIMAL_COMMA = ",";
    public static final String DECIMAL_DOT = ".";
    public static final String EMPTY_TEXT = "";
    public static final String SINGLE_SPACE = " ";
    public static final String MULTIPLE_WHITESPACE_REGEX = "\\s+";
    public static final String GUEST_TEACHER_NAME_REGEX = "(?i)\\((thg|a|moi|thinh giang).*?\\)";
    public static final String TEACHER_ACADEMIC_PREFIX_REGEX = "(?iu)^\\s*(?:(?:GS|PGS|TS|ThS|CN|KS)\\.?\\s*)+";
    public static final int HEADER_ROW_MIN_PHYSICAL_ROWS = 1;
    public static final int MAX_HEADER_SCAN_ROWS = 10;
    public static final int NEXT_ROW_OFFSET = 1;
    public static final int INVALID_COLUMN_INDEX = -1;
    public static final int MIN_VALID_COLUMN_INDEX = 0;
    public static final int MIN_VALID_CREDITS = 0;
    public static final int MIN_VALID_STUDENTS = 10;
    public static final List<String> EXCLUDED_STATUS_VALUES = List.of("huy", "canceled", "cancelled");

    public static final Map<String, List<String>> REQUIRED_COLUMN_ALIASES = Map.of(
        CLASS_NAME_FIELD, List.of("ten lop", "lop hoc phan", "hoc phan"),
        SUBJECT_NAME_FIELD, List.of("ten mon", "mon hoc", "ten hoc phan"),
        CREDITS_FIELD, List.of("so tin chi", "tin chi", "tc"),
        DEPARTMENT_HN_FIELD, List.of("ten bo mon hn", "bo mon hn", "bo mon dhtl", "ten bo mon", "bo mon"),
        DEPARTMENT_PH_FIELD, List.of("ten bo mon ph", "bo mon ph", "bo mon phmn", "ten bo mon", "bo mon"),
        STUDENT_COUNT_FIELD, List.of("so sv dang ky", "sv dang ky", "so sinh vien", "si so"),
        TEACHER_ORIGINAL_NAME_FIELD, List.of("giang vien", "ten giang vien", "gv")
    );

    public static final Map<String, List<String>> OPTIONAL_COLUMN_ALIASES = Map.of(
        STATUS_FIELD, List.of("trang thai", "status"),
        POSITION_FIELD, List.of("chuc vu", "loai giang vien"),
        DEGREE_FIELD, List.of("trinh do dao tao", "trinh do"),
        ACADEMIC_TITLE_FIELD, List.of("chuc danh khoa hoc", "chuc danh"),
        UNIT_NAME_FIELD, List.of("don vi")
    );
}
