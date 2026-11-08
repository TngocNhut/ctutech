package vn.luma.web;
import jakarta.validation.constraints.*;
public record TaiLieuHoTroYeuCau(@Positive long chuDe,@NotBlank @Size(max=150) String tieuDe,@NotBlank @Size(max=2000) String noiDung) { }
