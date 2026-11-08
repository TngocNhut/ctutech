package vn.luma.web;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record XacNhanHoanYeuCau(@NotNull @Positive @Digits(integer=15,fraction=0) BigDecimal soTien,
    @NotBlank @Size(max=50) @Pattern(regexp="[A-Za-z0-9 -]{4,50}") String taiKhoan,
    @NotBlank @Size(max=100) String nganHang,@NotBlank @Size(max=120) String nguoiNhan,
    @NotBlank @Size(max=150) String thamChieu,@AssertTrue(message="Cần kiểm tra chứng từ hoàn tiền") boolean daKiemTra){}
