package vn.luma.web;
import jakarta.validation.constraints.*;
public record ChuDeHoTroYeuCau(@NotBlank @Pattern(regexp="[A-Z0-9_]{2,50}") String kyHieu,@NotBlank @Size(max=120) String ten,@Size(max=2000) String viDu,boolean chuyenNhanVien,boolean hoatDong) { }
