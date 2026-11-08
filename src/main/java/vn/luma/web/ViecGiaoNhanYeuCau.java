package vn.luma.web;

import java.time.LocalDateTime;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

public record ViecGiaoNhanYeuCau(@NotBlank @Size(max=255) String ghiChu,
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime hanXuLy,boolean dongViec) { }
