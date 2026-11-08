package vn.luma.web;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record QuyTacKhuyenMaiYeuCau(@Positive long sku,
    @NotNull @Pattern(regexp="PHAN_TRAM|SO_TIEN") String kieu,
    @NotNull @DecimalMin("0.01") @Digits(integer=13,fraction=2) BigDecimal giaTri,boolean hoatDong) { }
