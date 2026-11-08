package vn.luma.web;

import jakarta.validation.constraints.*;

public record NguoiDungYeuCau(@NotNull @Pattern(regexp="KhachHang|QuanTri|NhanVien") String vaiTro,
    @NotNull @Pattern(regexp="HOAT_DONG|BI_KHOA") String trangThai,
    @NotBlank @Size(max=255) String lyDo) { }
