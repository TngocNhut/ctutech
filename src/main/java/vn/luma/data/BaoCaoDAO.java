package vn.luma.data;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class BaoCaoDAO {
    private final JdbcClient jdbc;
    public BaoCaoDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    private JdbcClient.StatementSpec ky(String sql,LocalDate from,LocalDate to){var zone=ZoneId.of("Asia/Ho_Chi_Minh");return jdbc.sql(sql).param("start",from.atStartOfDay(zone).toOffsetDateTime()).param("end",to.plusDays(1).atStartOfDay(zone).toOffsetDateTime());}
    public List<Map<String,Object>> doanhSo(LocalDate from,LocalDate to,String grain){return ky("""
        SELECT date_trunc(:grain,d."NgayHoanThanh" AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AS ky,
          count(DISTINCT d."MaDonHang") AS so_don,sum(c."SoLuong") AS so_may,
          sum(c."SoLuong"*c."DonGia"-c."GiamGiaPhanBo") AS doanh_thu,
          sum(c."SoLuong"*c."GiaVonLucDat") AS gia_von
        FROM "DonHang" d JOIN "ChiTietDonHang" c USING ("MaDonHang")
        WHERE d."TrangThaiDon"='HOAN_THANH' AND d."NgayHoanThanh">=:start AND d."NgayHoanThanh"<:end
        GROUP BY ky ORDER BY ky
        """,from,to).param("grain",grain).query().listOfRows();}
    public List<Map<String,Object>> tien(LocalDate from,LocalDate to){return ky("""
        WITH thu AS (SELECT p."CheDoXuLy" AS mode,sum(g."SoTien") AS value FROM "GiaoDichThanhToan" g JOIN "PhienThanhToan" p USING ("MaPhienThanhToan") WHERE g."NgayGiaoDich">=:start AND g."NgayGiaoDich"<:end GROUP BY p."CheDoXuLy"),
        hoan AS (SELECT p."CheDoXuLy" AS mode,sum(h."SoTienHoan") AS value FROM "HoanTien" h JOIN "GiaoDichThanhToan" g USING ("MaGiaoDich") JOIN "PhienThanhToan" p USING ("MaPhienThanhToan") WHERE h."TrangThai"='THANH_CONG' AND h."NgayHoanThanh">=:start AND h."NgayHoanThanh"<:end GROUP BY p."CheDoXuLy")
        SELECT m.mode,coalesce(t.value,0) AS da_thu,coalesce(h.value,0) AS da_hoan,coalesce(t.value,0)-coalesce(h.value,0) AS thu_rong FROM (VALUES ('MO_PHONG'),('TICH_HOP_THAT')) m(mode) LEFT JOIN thu t USING(mode) LEFT JOIN hoan h USING(mode)
        """,from,to).query().listOfRows();}
    public List<Map<String,Object>> banChay(LocalDate from,LocalDate to){return ky("""
        SELECT c."MaBienThe",(array_agg(c."TenSanPhamLucDat" ORDER BY d."NgayHoanThanh" DESC,d."MaDonHang" DESC))[1] AS "TenSanPhamLucDat",(array_agg(c."MaHangLucDat" ORDER BY d."NgayHoanThanh" DESC,d."MaDonHang" DESC))[1] AS "MaHangLucDat",(array_agg(c."TenPhanLoaiLucDat" ORDER BY d."NgayHoanThanh" DESC,d."MaDonHang" DESC))[1] AS "TenPhanLoaiLucDat",sum(c."SoLuong") AS so_may,sum(c."SoLuong"*c."DonGia"-c."GiamGiaPhanBo") AS doanh_thu
        FROM "DonHang" d JOIN "ChiTietDonHang" c USING ("MaDonHang") WHERE d."TrangThaiDon"='HOAN_THANH' AND d."NgayHoanThanh">=:start AND d."NgayHoanThanh"<:end GROUP BY c."MaBienThe" ORDER BY so_may DESC,c."MaBienThe" LIMIT 30
        """,from,to).query().listOfRows();}
    public Map<String,Object> quayLai(LocalDate from,LocalDate to){return ky("""
        WITH khach AS (SELECT DISTINCT "MaNguoiDung" FROM "DonHang" WHERE "TrangThaiDon"='HOAN_THANH' AND "NgayHoanThanh">=:start AND "NgayHoanThanh"<:end)
        SELECT count(*) AS tong_khach,count(*) FILTER(WHERE EXISTS(SELECT 1 FROM "DonHang" d WHERE d."MaNguoiDung"=k."MaNguoiDung" AND d."TrangThaiDon"='HOAN_THANH' AND d."NgayHoanThanh"<:start)) AS quay_lai FROM khach k
        """,from,to).query().singleRow();}
    public List<Map<String,Object>> danhGia(LocalDate from,LocalDate to){return ky("SELECT \"SoSao\",count(*) AS so_luong FROM \"DanhGia\" WHERE \"TrangThai\"='HIEN_THI' AND \"NgayTao\">=:start AND \"NgayTao\"<:end GROUP BY \"SoSao\" ORDER BY \"SoSao\"",from,to).query().listOfRows();}
    public List<Map<String,Object>> hangCho(){return jdbc.sql("SELECT \"TrangThaiDon\",count(*) AS so_don FROM \"DonHang\" GROUP BY \"TrangThaiDon\" ORDER BY \"TrangThaiDon\"").query().listOfRows();}
    public Map<String,Object> kho(){return jdbc.sql("SELECT coalesce(sum(\"SoLuongTon\"),0) AS ton,coalesce(sum(\"SoLuongGiu\"),0) AS giu,coalesce(sum(\"SoLuongTon\"-\"SoLuongGiu\"),0) AS kha_dung FROM \"TonKho\" WHERE \"MaKhoHang\"=1").query().singleRow();}
    public List<Map<String,Object>> danhMuc(LocalDate from,LocalDate to){return ky("SELECT c.\"TenPhanLoaiLucDat\",sum(c.\"SoLuong\") AS so_may,sum(c.\"SoLuong\"*c.\"DonGia\"-c.\"GiamGiaPhanBo\") AS doanh_thu FROM \"DonHang\" d JOIN \"ChiTietDonHang\" c USING (\"MaDonHang\") WHERE d.\"TrangThaiDon\"='HOAN_THANH' AND d.\"NgayHoanThanh\">=:start AND d.\"NgayHoanThanh\"<:end GROUP BY c.\"TenPhanLoaiLucDat\" ORDER BY doanh_thu DESC",from,to).query().listOfRows();}
    public List<Map<String,Object>> traHang(LocalDate from,LocalDate to){return ky("SELECT f.\"LoaiTraHang\",count(DISTINCT f.\"MaPhieuTraHang\") AS so_phieu,coalesce(sum(c.\"SoLuongDaNhan\"),0) AS so_may,coalesce(sum(c.\"SoTienDuocHoan\"),0) AS tien_duyet FROM \"PhieuTraHang\" f JOIN \"ChiTietTraHang\" c USING (\"MaPhieuTraHang\") WHERE f.\"NgayNhanHang\">=:start AND f.\"NgayNhanHang\"<:end GROUP BY f.\"LoaiTraHang\"",from,to).query().listOfRows();}
    public Map<String,Object> hanhVi(LocalDate from,LocalDate to){return ky("SELECT count(*) AS so_phien,coalesce(sum(\"SoGiayHoatDong\"),0) AS so_giay FROM \"PhienTruyCap\" WHERE \"NgayTao\">=:start AND \"NgayTao\"<:end",from,to).query().singleRow();}
    public List<Map<String,Object>> nhomTrang(LocalDate from,LocalDate to){return ky("SELECT \"NhomTrang\",\"LoaiSuKien\",count(*) AS so_luot FROM \"SuKienTruyCap\" WHERE \"NgayTao\">=:start AND \"NgayTao\"<:end GROUP BY \"NhomTrang\",\"LoaiSuKien\" ORDER BY so_luot DESC",from,to).query().listOfRows();}
    public List<Map<String,Object>> tuongTac(LocalDate month,int page){return ky("""
        WITH counts AS (SELECT "MaSanPham",count(*) FILTER(WHERE "LoaiTuongTac"='XEM') AS xem,count(*) FILTER(WHERE "LoaiTuongTac"='THEM_GIO') AS them_gio,count(*) FILTER(WHERE "LoaiTuongTac"='THEO_DOI') AS theo_doi FROM "TuongTacSanPham" WHERE "NgayTao">=:start AND "NgayTao"<:end GROUP BY "MaSanPham")
        SELECT s."MaSanPham",s."TenSanPham",s."DuongDan",s."NgayTao">=:start AS moi,coalesce(c.xem,0) AS xem,coalesce(c.them_gio,0) AS them_gio,coalesce(c.theo_doi,0) AS theo_doi,
          EXISTS(SELECT 1 FROM "TuongTacSanPham" WHERE "NgayTao">=:start AND "NgayTao"<:end) AS co_du_lieu
        FROM "SanPham" s JOIN "PhanLoai" p USING ("MaPhanLoai") JOIN "ThuongHieu" h USING ("MaThuongHieu") LEFT JOIN counts c USING ("MaSanPham")
        WHERE s."DangHienThi" AND p."DangHienThi" AND h."DangHienThi" AND EXISTS(SELECT 1 FROM "BienTheSanPham" b WHERE b."MaSanPham"=s."MaSanPham" AND b."DangKinhDoanh")
        ORDER BY coalesce(c.xem,0),s."MaSanPham" LIMIT 30 OFFSET :offset
        """,month,YearMonth.from(month).atEndOfMonth()).param("offset",(page-1)*30).query().listOfRows();}
}
