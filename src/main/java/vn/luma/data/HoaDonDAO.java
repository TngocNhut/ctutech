package vn.luma.data;

import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class HoaDonDAO {
    private final JdbcClient jdbc;
    public HoaDonDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    public Map<String,Object> khoaDon(long order){return jdbc.sql("SELECT * FROM \"DonHang\" WHERE \"MaDonHang\"=:d FOR UPDATE").param("d",order).query().singleRow();}
    public Optional<Map<String,Object>> doc(long order){return jdbc.sql("""
        SELECT h.*,d."MaDonCongKhai",p."CheDoXuLy",to_char(h."NgayPhatHanh" AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY HH24:MI') AS "NgayIn",
          h."NoiDungChot"->>'NguoiNhan' AS "NguoiNhan",h."NoiDungChot"->>'DienThoai' AS "DienThoai",h."NoiDungChot"->>'Email' AS "Email",
          h."NoiDungChot"->>'DiaChi' AS "DiaChi",(h."NoiDungChot"->>'PhiVanChuyen')::numeric AS "PhiGiao"
        FROM "HoaDon" h JOIN "DonHang" d USING ("MaDonHang")
        LEFT JOIN "PhienThanhToan" p ON p."MaPhienThanhToan"=d."MaPhienThanhToanChapNhan" WHERE h."MaDonHang"=:d
        """).param("d",order).query().listOfRows().stream().findFirst();}
    public List<Map<String,Object>> dong(long invoice){return jdbc.sql("SELECT * FROM \"ChiTietHoaDon\" WHERE \"MaHoaDon\"=:i ORDER BY \"MaChiTietDonHang\"").param("i",invoice).query().listOfRows();}
    public java.math.BigDecimal tongDongDon(long order){return jdbc.sql("SELECT coalesce(sum(\"SoLuong\"*\"DonGia\"-\"GiamGiaPhanBo\"),0) FROM \"ChiTietDonHang\" WHERE \"MaDonHang\"=:d").param("d",order).query(java.math.BigDecimal.class).single();}
    public java.math.BigDecimal tongThu(long session){return jdbc.sql("SELECT coalesce(sum(\"SoTien\"),0) FROM \"GiaoDichThanhToan\" WHERE \"MaPhienThanhToan\"=:p").param("p",session).query(java.math.BigDecimal.class).single();}
    public void tao(long order){
        jdbc.sql("""
            INSERT INTO "HoaDon" ("MaDonHang","SoHoaDon","NgayPhatHanh","NoiDungChot","TongTien","PhienBanNoiDung")
            SELECT "MaDonHang",'LUMA-'||"MaDonCongKhai",now(),jsonb_build_object('NguoiNhan',"TenNguoiNhan",'DienThoai',"SoDienThoaiNhan",'Email',"ThuDienTuNhan",'DiaChi',"DiaChiNhanCuThe"||', '||"PhuongXaNhan"||', '||"TinhThanhNhan",'PhiVanChuyen',"PhiVanChuyen",'TienGiamGia',"TienGiamGia",'TienTe','VND'),"TongThanhToan",2
            FROM "DonHang" WHERE "MaDonHang"=:d ON CONFLICT ("MaDonHang") DO NOTHING
            """).param("d",order).update();
        jdbc.sql("""
            INSERT INTO "ChiTietHoaDon" ("MaHoaDon","MaDonHang","MaChiTietDonHang","TenMatHang","MaHangLucDat","SoLuong","DonGia","GiamGia")
            SELECT h."MaHoaDon",d."MaDonHang",d."MaChiTietDonHang",d."TenSanPhamLucDat",d."MaHangLucDat",d."SoLuong",d."DonGia",d."GiamGiaPhanBo"
            FROM "ChiTietDonHang" d JOIN "HoaDon" h USING ("MaDonHang") WHERE d."MaDonHang"=:d ON CONFLICT DO NOTHING
            """).param("d",order).update();
    }
}
