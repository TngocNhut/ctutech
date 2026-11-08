package vn.luma.web;
import java.time.LocalDate;
import jakarta.validation.constraints.*;
public record DangKyMoiYeuCau(
 @NotBlank(message="Nhập họ của bạn.") @Size(max=60,message="Họ tối đa 60 ký tự.") String ho,
 @NotBlank(message="Nhập tên của bạn.") @Size(max=60,message="Tên tối đa 60 ký tự.") String ten,
 @NotNull(message="Chọn ngày sinh.") @Past(message="Ngày sinh phải trước hôm nay.") LocalDate ngaySinh,
 @NotBlank(message="Nhập email.") @Email(message="Email chưa đúng định dạng.") @Size(max=254) String taiKhoan,
 @NotBlank(message="Nhập mật khẩu.") @Size(min=12,max=64,message="Mật khẩu cần 12–64 ký tự.") String matKhau,
 @NotBlank(message="Nhập lại mật khẩu.") String xacNhanMatKhau) { }
