package vn.luma.web;

import jakarta.validation.constraints.*;

public record AnhSanPhamYeuCau(@Min(1) Long maSku,@NotBlank @Size(max=255) String moTa,
    @Min(0) @Max(10000) int thuTu,@Size(max=2000) String nguon,@Size(max=500) String thongTinSuDung,
    Boolean hienThi){public AnhSanPhamYeuCau{hienThi=hienThi==null?false:hienThi;}}
