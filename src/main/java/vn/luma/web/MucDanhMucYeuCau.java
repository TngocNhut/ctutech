package vn.luma.web;

import jakarta.validation.constraints.*;

public record MucDanhMucYeuCau(@NotBlank @Size(max=100) String ten,
    @NotBlank @Size(max=120) @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") String duongDan,
    @Size(max=2000) String moTa,@Min(0) @Max(10000) Integer thuTu,Boolean hienThi){public MucDanhMucYeuCau{hienThi=hienThi==null?false:hienThi;thuTu=thuTu==null?0:thuTu;}}
