package vn.luma.web;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

public record TaoVanDonYeuCau(@Min(1) long maDonViVanChuyen,@NotBlank @Size(max=80) @Pattern(regexp="[A-Za-z0-9._/-]+") String maTheoDoi,
    @DecimalMin("0") @DecimalMax("999999999999999") @Digits(integer=15,fraction=0) BigDecimal cuocGoc,
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime ngayDuKien) { }
