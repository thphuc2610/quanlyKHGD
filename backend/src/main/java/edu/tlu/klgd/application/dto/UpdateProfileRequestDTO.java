package edu.tlu.klgd.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequestDTO(
    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 255, message = "Họ và tên tối đa 255 ký tự")
    String fullName,

    @Size(max = 255, message = "Tên giảng viên tối đa 255 ký tự")
    String teacherName,

    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email tối đa 255 ký tự")
    String email,

    @Size(max = 40, message = "Số điện thoại tối đa 40 ký tự")
    String phone,

    @Size(max = 2000000, message = "Ảnh đại diện quá lớn")
    String avatarUrl
) {
}
