package vn.luma.web;

import jakarta.validation.constraints.*;

public record DongSanPhamYeuCau(@Positive long maThuongHieu,@NotBlank @Size(max=120) String ten,
    @NotBlank @Size(max=120) @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") String duongDan) { }
