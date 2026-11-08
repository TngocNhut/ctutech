package vn.luma.web;

import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record NoiDungBienTheYeuCau(@NotBlank @Size(max=80) @Pattern(regexp="[A-Za-z0-9._-]+") String maHang,
    @Size(max=100) String maMayHang,@NotBlank @Size(max=220) String tenBienThe,@NotBlank @Size(max=60) String mauSac,
    @NotBlank @Size(max=120) String cpu,@Size(max=120) String gpu,@Min(0) @Max(1024) int ram,
    @Min(0) @Max(65536) int luuTru,@NotNull @DecimalMin("0") @DecimalMax("99.9") @Digits(integer=2,fraction=1) BigDecimal manHinh,
    @NotBlank @Size(max=100) String heDieuHanh,Boolean kinhDoanh,@NotNull @Valid CapNhatBienThe giaTon){
    public NoiDungBienTheYeuCau {kinhDoanh=kinhDoanh==null?false:kinhDoanh;if(giaTon==null)giaTon=new CapNhatBienThe(null,null,null,0,"");}
}
