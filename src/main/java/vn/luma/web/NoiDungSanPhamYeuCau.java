package vn.luma.web;

import jakarta.validation.constraints.*;

public record NoiDungSanPhamYeuCau(@Min(1) long maPhanLoai,@Min(1) long maThuongHieu,
    @NotBlank @Size(max=200) String tenSanPham,@NotBlank @Size(max=220) @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") String duongDan,
    @NotBlank @Size(max=500) String moTaNgan,@Size(max=12000) String moTaChiTiet,
    @Min(0) @Max(120) int baoHanh,@Size(max=2000) String nguon,Boolean hienThi){
    public NoiDungSanPhamYeuCau {hienThi=hienThi==null?false:hienThi;}
}
