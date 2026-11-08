package vn.luma.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record DatHangYeuCau(@NotBlank @Pattern(regexp="[0-9a-fA-F-]{36}") String khoaChongLap,
    @NotBlank @Size(max=2000) String noiDungGio,
    @Size(max=40) String maGiamGia,
    @NotNull @Valid DiaChiYeuCau diaChi,
    @Pattern(regexp="COD|PAYOS|PAYOS_MO_PHONG") String phuongThuc) {
    public DatHangYeuCau {if(phuongThuc==null||phuongThuc.isBlank())phuongThuc="COD";}
    public DatHangYeuCau(String key,String cart,String coupon,DiaChiYeuCau address){this(key,cart,coupon,address,"COD");}
}
