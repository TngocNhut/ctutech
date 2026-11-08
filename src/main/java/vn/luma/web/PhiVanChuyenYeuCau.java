package vn.luma.web;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record PhiVanChuyenYeuCau(@NotBlank @Pattern(regexp="[0-9a-fA-F-]{36}") String khoaChongLap,
    @Pattern(regexp="CUOC_GOC|PHU_PHI|GIAM_CUOC") @NotBlank String loaiPhi,
    @NotNull @DecimalMin("0") @Digits(integer=15,fraction=0) BigDecimal soTien,
    @NotBlank @Size(max=500) String ghiChu){}
