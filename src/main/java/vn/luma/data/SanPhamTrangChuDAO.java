package vn.luma.data;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SanPhamTrangChuDAO {
    private final JdbcClient jdbc;
    public SanPhamTrangChuDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    public List<Map<String,Object>> lay(boolean banChay){
        String order=banChay?"sold DESC, s.\"MaSanPham\" DESC":"s.\"NgayTao\" DESC, s.\"MaSanPham\" DESC";
        return jdbc.sql("""
            SELECT s."TenSanPham" AS ten, s."DuongDan" AS slug, h."TenThuongHieu" AS hang,
                   min(g."GiaHieuLuc") AS gia,
                   (SELECT t."DuongDanTep" FROM "TepSanPham" t WHERE t."MaSanPham"=s."MaSanPham" AND t."LoaiTep"='ANH' AND t."DangHienThi" ORDER BY t."ThuTu",t."MaTep" LIMIT 1) AS anh,
                   coalesce((SELECT sum(c."SoLuong") FROM "ChiTietDonHang" c JOIN "DonHang" d USING ("MaDonHang") JOIN "BienTheSanPham" sold_sku ON sold_sku."MaBienThe"=c."MaBienThe" WHERE sold_sku."MaSanPham"=s."MaSanPham" AND d."TrangThaiDon"='HOAN_THANH'),0) AS sold
            FROM "SanPham" s JOIN "PhanLoai" p USING ("MaPhanLoai") JOIN "ThuongHieu" h USING ("MaThuongHieu")
            JOIN "BienTheSanPham" b USING ("MaSanPham") JOIN "GiaKhuyenMaiBienThe" g USING ("MaBienThe")
            WHERE s."DangHienThi" AND p."DangHienThi" AND h."DangHienThi" AND b."DangKinhDoanh"
            GROUP BY s."MaSanPham",h."TenThuongHieu"
            """+(banChay?" HAVING coalesce((SELECT sum(c.\"SoLuong\") FROM \"ChiTietDonHang\" c JOIN \"DonHang\" d USING (\"MaDonHang\") JOIN \"BienTheSanPham\" sold_sku ON sold_sku.\"MaBienThe\"=c.\"MaBienThe\" WHERE sold_sku.\"MaSanPham\"=s.\"MaSanPham\" AND d.\"TrangThaiDon\"='HOAN_THANH'),0)>0":"")+" ORDER BY "+order+" LIMIT 8").query().listOfRows();
    }
}
