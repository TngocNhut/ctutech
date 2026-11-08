package vn.luma.web;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record CapNhatBienThe(@NotNull @DecimalMin("1") @DecimalMax("999999999999999") @Digits(integer=15,fraction=0) BigDecimal giaBan,
    @NotNull @DecimalMin("1") @DecimalMax("999999999999999") @Digits(integer=15,fraction=0) BigDecimal giaNiemYet,
    @NotNull @DecimalMin("0") @DecimalMax("999999999999999") @Digits(integer=15,fraction=0) BigDecimal giaVon,
    @Min(0) @Max(1000000) int soLuongTon,
    @NotBlank @Size(max=255) String lyDo) { }
