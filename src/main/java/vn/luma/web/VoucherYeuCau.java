package vn.luma.web;

import java.math.BigDecimal;
import java.time.*;
import jakarta.validation.constraints.*;

public record VoucherYeuCau(
    @NotBlank @Pattern(regexp="[A-Z0-9_-]{2,40}",message="Mã gồm 2–40 chữ hoa, số, _ hoặc -.") String kyHieu,
    @NotBlank @Size(max=255) String moTa,
    @NotNull @Pattern(regexp="PHAN_TRAM|SO_TIEN") String kieu,
    @NotNull @DecimalMin("0.01") @Digits(integer=13,fraction=2) BigDecimal giaTri,
    @DecimalMin("1") @Digits(integer=15,fraction=0) BigDecimal toiDa,
    @NotNull @DecimalMin("0") @Digits(integer=15,fraction=0) BigDecimal donToiThieu,
    @NotNull @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime batDau,@NotNull @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime ketThuc,
    @Min(1) int tongLuot,@Min(1) int moiKhach,boolean congKhai,boolean hoatDong) {
    public OffsetDateTime batDauTaiVN(){return batDau.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();}
    public OffsetDateTime ketThucTaiVN(){return ketThuc.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();}
}
