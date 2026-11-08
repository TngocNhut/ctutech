package vn.luma.web;

import jakarta.validation.constraints.*;

public record KiemDuyetDanhGiaYeuCau(boolean hienThi,@Size(max=2000) String phanHoi,
    @NotBlank @Size(max=255) String lyDo) { }
