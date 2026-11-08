package vn.luma.data;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TheoDoiDAO {
    private final JdbcClient jdbc;
    public TheoDoiDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    public Map<String,Object> tinhTrang(long product){return jdbc.sql("""
        SELECT s."MaSanPham",s."TenSanPham",s."DuongDan",a.gia,a.sku,a.sku_con,coalesce(a.co_hang,false) AS co_hang
        FROM "SanPham" s LEFT JOIN LATERAL (
          SELECT min(g."GiaHieuLuc") AS gia,bool_or(coalesce(t."SoLuongTon"-t."SoLuongGiu",0)>0) AS co_hang,
            (array_agg(b."MaBienThe" ORDER BY g."GiaHieuLuc",b."MaBienThe"))[1] AS sku,
            (array_agg(b."MaBienThe" ORDER BY g."GiaHieuLuc",b."MaBienThe") FILTER (WHERE coalesce(t."SoLuongTon"-t."SoLuongGiu",0)>0))[1] AS sku_con
          FROM "BienTheSanPham" b JOIN "GiaKhuyenMaiBienThe" g USING ("MaBienThe")
          JOIN "ThuongHieu" h ON h."MaThuongHieu"=s."MaThuongHieu" JOIN "PhanLoai" p ON p."MaPhanLoai"=s."MaPhanLoai"
          JOIN "KhoHang" k ON k."MaKhoHang"=1 AND k."DangHoatDong"
          LEFT JOIN "TonKho" t ON t."MaBienThe"=b."MaBienThe" AND t."MaKhoHang"=k."MaKhoHang"
          WHERE b."MaSanPham"=s."MaSanPham" AND b."DangKinhDoanh" AND s."DangHienThi" AND h."DangHienThi" AND p."DangHienThi"
        ) a ON true WHERE s."MaSanPham"=:p
        """).param("p",product).query().listOfRows().stream().findFirst().orElseThrow(()->new vn.luma.business.KhongTimThayException("Không tìm thấy sản phẩm theo dõi."));}
    public boolean phienBanHopLe(long product,long sku){return jdbc.sql("SELECT EXISTS(SELECT 1 FROM \"BienTheSanPham\" WHERE \"MaSanPham\"=:p AND \"MaBienThe\"=:sku AND \"DangKinhDoanh\")").param("p",product).param("sku",sku).query(Boolean.class).single();}
    public Optional<Map<String,Object>> cuaToi(long user,long product){return jdbc.sql("SELECT * FROM \"TheoDoiSanPham\" WHERE \"MaNguoiDung\"=:u AND \"MaSanPham\"=:p").param("u",user).param("p",product).query().listOfRows().stream().findFirst();}
    public long dem(long user){return jdbc.sql("SELECT count(*) FROM \"TheoDoiSanPham\" WHERE \"MaNguoiDung\"=:u").param("u",user).query(Long.class).single();}
    public void luu(long user,long product,boolean price,boolean stock,Map<String,Object> current,Long selected){jdbc.sql("""
        INSERT INTO "TheoDoiSanPham" ("MaNguoiDung","MaSanPham","NhanBaoGiamGia","NhanBaoCoHang","GiaThamChieu","CoHangThamChieu","NgayCapNhat","MaBienTheDaChon")
        VALUES (:u,:p,:price,:stock,:reference,:available,clock_timestamp(),:selected)
        ON CONFLICT ("MaNguoiDung","MaSanPham") DO UPDATE SET "NhanBaoGiamGia"=excluded."NhanBaoGiamGia","NhanBaoCoHang"=excluded."NhanBaoCoHang",
          "MaBienTheDaChon"=coalesce(excluded."MaBienTheDaChon","TheoDoiSanPham"."MaBienTheDaChon"),"GiaThamChieu"=coalesce(excluded."GiaThamChieu","TheoDoiSanPham"."GiaThamChieu"),"CoHangThamChieu"=excluded."CoHangThamChieu",
          "PhienThongBao"="TheoDoiSanPham"."PhienThongBao"+1,"NgayCapNhat"=clock_timestamp()
        """).param("u",user).param("p",product).param("price",price).param("stock",stock).param("reference",current.get("gia"),java.sql.Types.NUMERIC).param("available",current.get("co_hang")).param("selected",selected,java.sql.Types.BIGINT).update();}
    public void xoa(long user,long product){jdbc.sql("DELETE FROM \"TheoDoiSanPham\" WHERE \"MaNguoiDung\"=:u AND \"MaSanPham\"=:p").param("u",user).param("p",product).update();}
    public List<Map<String,Object>> danhSach(long user,int page){return jdbc.sql("""
        SELECT f.*,b."TenBienThe" AS ten_bien_the,b."MauSac" AS mau_da_chon,b."DangKinhDoanh" AS phien_ban_kinh_doanh,
          (SELECT a."DuongDanTep" FROM "TepSanPham" a WHERE a."MaSanPham"=f."MaSanPham" AND a."MaBienThe"=f."MaBienTheDaChon" AND a."LoaiTep"='ANH' AND a."DangHienThi" ORDER BY a."ThuTu",a."MaTep" LIMIT 1) AS anh_da_chon
        FROM "TheoDoiSanPham" f LEFT JOIN "BienTheSanPham" b ON b."MaBienThe"=f."MaBienTheDaChon" AND b."MaSanPham"=f."MaSanPham"
        WHERE f."MaNguoiDung"=:u ORDER BY f."MaTheoDoi" DESC LIMIT 30 OFFSET :offset
        """).param("u",user).param("offset",(page-1)*30).query().listOfRows();}
    public List<Long> cuaNguoiDung(long user){return jdbc.sql("SELECT \"MaTheoDoi\" FROM \"TheoDoiSanPham\" WHERE \"MaNguoiDung\"=:u ORDER BY \"MaTheoDoi\"").param("u",user).query(Long.class).list();}
    public Optional<Map<String,Object>> khoa(long id){return jdbc.sql("""
        SELECT t.*,n."TrangThai" AS tai_khoan FROM "TheoDoiSanPham" t JOIN "NguoiDung" n USING ("MaNguoiDung")
        WHERE t."MaTheoDoi"=:id FOR UPDATE OF t
        """).param("id",id).query().listOfRows().stream().findFirst();}
    public void capNhat(long id,BigDecimal price,boolean available,long phase){jdbc.sql("UPDATE \"TheoDoiSanPham\" SET \"GiaThamChieu\"=:price,\"CoHangThamChieu\"=:stock,\"PhienThongBao\"=:phase,\"NgayCapNhat\"=clock_timestamp() WHERE \"MaTheoDoi\"=:id").param("id",id).param("price",price,java.sql.Types.NUMERIC).param("stock",available).param("phase",phase).update();}
    public long mocCuoi(){return jdbc.sql("SELECT coalesce(max(\"MaTheoDoi\"),0) FROM \"TheoDoiSanPham\"").query(Long.class).single();}
    public List<Long> tiepTheo(long after,long until){return jdbc.sql("SELECT \"MaTheoDoi\" FROM \"TheoDoiSanPham\" WHERE \"MaTheoDoi\">:after AND \"MaTheoDoi\"<=:until ORDER BY \"MaTheoDoi\" LIMIT 200").param("after",after).param("until",until).query(Long.class).list();}
}
