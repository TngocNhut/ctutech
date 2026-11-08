package vn.luma.web;

import jakarta.validation.constraints.*;

public record DanhGiaYeuCau(@Min(1) @Max(5) int soSao,@Size(max=150) String tieuDe,
    @NotBlank @Size(min=10,max=2000) String noiDung) { }
