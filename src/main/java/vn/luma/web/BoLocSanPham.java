package vn.luma.web;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record BoLocSanPham(@Size(max=100) String q,@Size(max=120) String loai,@Size(max=120) String hang,
    @DecimalMin("0") @Digits(integer=15,fraction=0) BigDecimal giaTu,@DecimalMin("0") @Digits(integer=15,fraction=0) BigDecimal giaDen,
    @Min(0) @Max(1024) Integer ram,@Min(0) @Max(65536) Integer luuTru,@Size(max=80) String cpu,@Size(max=80) String gpu,
    Boolean conHang,@Size(max=20) String sapXep,@Min(1) @Max(10000) Integer trang){
    public BoLocSanPham {q=chuan(q);loai=chuan(loai);hang=chuan(hang);cpu=chuan(cpu);gpu=chuan(gpu);conHang=conHang==null?false:conHang;sapXep=sapXep==null||sapXep.isBlank()?"mac-dinh":sapXep.strip();trang=trang==null?1:trang;}
    private static String chuan(String s){return s==null?"":s.strip();}
    public boolean coBoLoc(){return !q.isBlank()||!hang.isBlank()||giaTu!=null||giaDen!=null||ram!=null||luuTru!=null||!cpu.isBlank()||!gpu.isBlank()||conHang||!"mac-dinh".equals(sapXep);}
    public boolean coDieuKien(){return coBoLoc()||!loai.isBlank()||trang>1;}
}
