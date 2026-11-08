package vn.luma.model;

import java.math.BigDecimal;

public record BienThe(long maBienThe,String tenSanPham,String duongDan,String maHang,String tenBienThe,
    String mauSac,String boXuLy,String boXuLyDoHoa,int dungLuongRam,int dungLuongLuuTru,
    BigDecimal kichThuocManHinh,String heDieuHanh,BigDecimal giaBan,BigDecimal giaNiemYet,
    BigDecimal giaVon,int soLuongTon,int soLuongGiu,boolean dangKinhDoanh,BigDecimal giaGoc,String tenKhuyenMai) {
    public int tonKhaDung(){return soLuongTon-soLuongGiu;}
}
