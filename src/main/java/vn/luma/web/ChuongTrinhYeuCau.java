package vn.luma.web;

import java.time.*;
import jakarta.validation.constraints.*;

public record ChuongTrinhYeuCau(
    @NotBlank @Pattern(regexp="[A-Z0-9_-]{2,50}",message="Ký hiệu gồm 2–50 chữ hoa, số, _ hoặc -.") String kyHieu,
    @NotBlank @Size(max=160) String ten,@Size(max=10000) String moTa,
    @NotNull @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime batDau,@NotNull @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime ketThuc,
    @Min(-10000) @Max(10000) int uuTien,
    @Pattern(regexp="NHAP|CONG_BO|TAM_DUNG") @NotNull String trangThai,boolean kemMa) {
    public OffsetDateTime batDauTaiVN(){return batDau.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();}
    public OffsetDateTime ketThucTaiVN(){return ketThuc.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();}
}
