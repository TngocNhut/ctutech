package vn.luma.web;

import jakarta.validation.constraints.*;

public record ThuocTinhYeuCau(@Positive long maPhanLoai,
    @NotBlank @Pattern(regexp="[A-Z0-9_]{2,50}") String kyHieu,@NotBlank @Size(max=120) String ten,
    @NotNull @Pattern(regexp="CHU|SO|CO_KHONG") String kieu,@Size(max=30) String donVi,
    boolean choLoc,@Min(0) @Max(10000) int thuTu) { }
