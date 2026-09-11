package edu.tlu.klgd.domain.common;

public final class ApiMessage {
    private ApiMessage() {
    }

    public static final String UNAUTHORIZED = "Chưa xác thực";
    public static final String FORBIDDEN = "Không có quyền truy cập";
    public static final String INVALID_DATA = "Dữ liệu không hợp lệ";
    public static final String FILE_TOO_LARGE = "Tệp vượt quá dung lượng cho phép";
    public static final String FILE_PROCESSING_FAILED = "Không thể xử lý tệp hoặc báo cáo";
    public static final String EXCEL_FILE_REQUIRED = "Vui lòng tải lên tệp Excel hoặc PDF HPTN";
    public static final String VALID_EXCEL_SHEET_NOT_FOUND = "Không tìm thấy trang tính hợp lệ trong tệp Excel";
    public static final String INVALID_CLASS_RECORD_WARNING = "Thiếu/sai tín chỉ, số sinh viên phải từ 10 trở lên, tên môn hoặc giảng viên.";
    public static final String INVALID_USERNAME_OR_PASSWORD = "Tên đăng nhập hoặc mật khẩu không đúng";
    public static final String USER_NOT_FOUND = "Người dùng không tồn tại";
    public static final String JWT_GENERATION_FAILED = "Không thể tạo JWT";
    public static final String PASSWORD_RESET_TOKEN_CREATED = "Đã tạo mã đặt lại mật khẩu";
    public static final String PASSWORD_RESET_TOKEN_INVALID = "Mã đặt lại mật khẩu không hợp lệ";
    public static final String PASSWORD_RESET_TOKEN_EXPIRED = "Mã đặt lại mật khẩu đã hết hạn";
    public static final String PASSWORD_CONFIRM_NOT_MATCHED = "Mật khẩu xác nhận không khớp";
    public static final String PASSWORD_RESET_SUCCESS = "Đặt lại mật khẩu thành công";
    public static final String PASSWORD_CHANGE_SUCCESS = "Đổi mật khẩu thành công";
    public static final String CURRENT_PASSWORD_INVALID = "Mật khẩu hiện tại không đúng";
    public static final String USERNAME_ALREADY_EXISTS = "Tên đăng nhập đã tồn tại";
    public static final String TEACHER_ACCOUNT_REQUIRES_LINK = "Tài khoản giảng viên phải gắn với giảng viên trong danh sách";
    public static final String RESOURCE_NOT_FOUND_FORMAT = "Không tìm thấy %s: %s";
    public static final String RESOURCE_IMPORT_BATCH = "lần nhập dữ liệu";
    public static final String RESOURCE_USER = "người dùng";
    public static final String VALIDATION_FIELD_SEPARATOR = ": ";
}
