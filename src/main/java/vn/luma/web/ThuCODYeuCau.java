package vn.luma.web;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record ThuCODYeuCau(@NotNull @DecimalMin("1") @DecimalMax("999999999999999") @Digits(integer=15,fraction=0) BigDecimal soTien,
    @NotBlank @Size(max=150) String maChungTu,@AssertTrue(message="Cần xác nhận đã kiểm tra khoản thu") boolean daKiemTra) { }
