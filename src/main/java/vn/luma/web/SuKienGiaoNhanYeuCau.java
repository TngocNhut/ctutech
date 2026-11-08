package vn.luma.web;
import jakarta.validation.constraints.*;

public record SuKienGiaoNhanYeuCau(@NotBlank @Pattern(regexp="BAN_GIAO|CHO_GIAO_HANG|DA_GIAO|GIAO_THAT_BAI|GIAO_LAI") String hanhDong,
    @NotBlank @Pattern(regexp="[0-9a-fA-F-]{36}") String khoaSuKien,@NotBlank @Size(max=500) String ghiChu) { }
