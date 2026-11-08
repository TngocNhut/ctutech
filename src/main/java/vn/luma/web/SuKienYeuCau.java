package vn.luma.web;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
public record SuKienYeuCau(@Positive Long chuongTrinh,@NotBlank @Size(max=160) String tieuDe,@NotBlank @Size(max=160) @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") String duongDan,@NotBlank @Size(max=12000) String noiDung,@NotBlank @Pattern(regexp="NHAP|CONG_BO|AN") String trangThai,@NotNull @DateTimeFormat(pattern="yyyy-MM-dd'T'HH:mm") LocalDateTime batDau,@DateTimeFormat(pattern="yyyy-MM-dd'T'HH:mm") LocalDateTime ketThuc) { }
