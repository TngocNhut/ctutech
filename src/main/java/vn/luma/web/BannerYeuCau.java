package vn.luma.web;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
public record BannerYeuCau(@Positive long viTri,@Positive Long suKien,@NotBlank @Size(max=160) String tieuDe,@NotBlank @Size(max=200) String moTaAnh,@Size(max=500) String dich,@Min(0) @Max(10000) int thuTu,boolean hienThi,@NotNull @DateTimeFormat(pattern="yyyy-MM-dd'T'HH:mm") LocalDateTime batDau,@DateTimeFormat(pattern="yyyy-MM-dd'T'HH:mm") LocalDateTime ketThuc) { }
