package vn.luma.web;

import jakarta.validation.constraints.*;

public record DangKyYeuCau(
    @NotBlank(message="Nhập họ tên của bạn") @Size(max=120) String hoTen,
    @NotBlank(message="Nhập email") @Email(message="Email chưa đúng định dạng") @Size(max=254) String email,
    @NotBlank(message="Nhập mật khẩu") @Size(min=12,max=64,message="Mật khẩu cần từ 12 đến 64 ký tự") String matKhau,
    @NotBlank(message="Nhập lại mật khẩu") String xacNhanMatKhau) { }
