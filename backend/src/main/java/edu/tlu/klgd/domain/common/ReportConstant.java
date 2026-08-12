package edu.tlu.klgd.domain.common;

public final class ReportConstant {
    private ReportConstant() {
    }

    public static final String WORKLOAD_EXCEL_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String WORKLOAD_EXCEL_FILE_NAME = "tong-hop-klgd-hptn.xlsx";
    public static final String SUMMARY_SHEET_NAME = "TỔNG HỢP_KLGD";
    public static final String DETAIL_SHEET_NAME = "CHI TIẾT_KLGD";
    public static final String PHYSICAL_EDUCATION_SHEET_NAME = "CHI TIẾT_KLGD CÁC MÔN GDTC";
    public static final String GRADUATION_PROJECT_SHEET_NAME = "KHỐI LƯỢNG HPTN";
    public static final String NUMBER_FORMAT = "#,##0.00";
    public static final String EMPTY_CELL_VALUE = "";
    public static final int REPORT_HEADER_ROW_INDEX = 3;
    public static final int REPORT_FIRST_DATA_ROW_INDEX = 4;

    public static final String[] SUMMARY_HEADERS = {
        "STT", "Giảng viên", "Số_lớp", "Tổng_TC", "Tổng_SV", "Tổng_tiết_quy_đổi", "Khối lượng HPTN", "Tổng cộng", "Ghi chú"
    };
    public static final int SUMMARY_INDEX_COLUMN = 0;
    public static final int SUMMARY_TEACHER_NAME_COLUMN = 1;
    public static final int SUMMARY_CLASS_COUNT_COLUMN = 2;
    public static final int SUMMARY_TOTAL_CREDITS_COLUMN = 3;
    public static final int SUMMARY_TOTAL_STUDENTS_COLUMN = 4;
    public static final int SUMMARY_TOTAL_STANDARD_HOURS_COLUMN = 5;
    public static final int SUMMARY_GRADUATION_HOURS_COLUMN = 6;
    public static final int SUMMARY_TOTAL_HOURS_COLUMN = 7;
    public static final int SUMMARY_NOTE_COLUMN = 8;

    public static final String[] DETAIL_HEADERS = {
        "STT", "Giảng viên", "Tên lớp", "Số tín chỉ", "Số SV đăng ký", "Tên bộ môn PH", "Đơn vị", "Hệ số K_lt", "Hệ số K_th", "Tiết quy đổi"
    };
    public static final int DETAIL_INDEX_COLUMN = 0;
    public static final int DETAIL_TEACHER_NAME_COLUMN = 1;
    public static final int DETAIL_CLASS_NAME_COLUMN = 2;
    public static final int DETAIL_CREDITS_COLUMN = 3;
    public static final int DETAIL_STUDENT_COUNT_COLUMN = 4;
    public static final int DETAIL_DEPARTMENT_PH_COLUMN = 5;
    public static final int DETAIL_UNIT_COLUMN = 6;
    public static final int DETAIL_COEFFICIENT_THEORY_COLUMN = 7;
    public static final int DETAIL_COEFFICIENT_PRACTICE_COLUMN = 8;
    public static final int DETAIL_STANDARD_HOURS_COLUMN = 9;

    public static final String[] GRADUATION_PROJECT_HEADERS = {
        "STT", "Tên giảng viên", "Số lượng sinh viên quy đổi", "Số tiết quy đổi", "Ghi chú", "", "HK1", "", "HK2"
    };
    public static final int GRADUATION_INDEX_COLUMN = 0;
    public static final int GRADUATION_TEACHER_NAME_COLUMN = 1;
    public static final int GRADUATION_STUDENT_TOTAL_COLUMN = 2;
    public static final int GRADUATION_HOURS_COLUMN = 3;
    public static final int GRADUATION_NOTE_COLUMN = 4;
    public static final int GRADUATION_HK1_COLUMN = 6;
    public static final int GRADUATION_HK2_COLUMN = 8;
}
