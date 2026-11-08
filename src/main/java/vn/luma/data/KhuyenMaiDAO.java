package vn.luma.data;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.luma.web.*;

@Repository
public class KhuyenMaiDAO {
    public record Gia(long sku, BigDecimal giaGoc, BigDecimal gia, Long chuongTrinh, String ten, boolean kemMa) { }
    private final JdbcClient jdbc;
    public KhuyenMaiDAO(JdbcClient jdbc) { this.jdbc=jdbc; }
    public Map<Long,Gia> gia(Collection<Long> skus) {
        Map<Long,Gia> result=new HashMap<>();
        if(skus.isEmpty())return result;
        jdbc.sql("SELECT * FROM \"GiaKhuyenMaiBienThe\" WHERE \"MaBienThe\" IN (:ids)").param("ids",skus)
            .query((r,n)->new Gia(r.getLong("MaBienThe"),r.getBigDecimal("GiaGoc"),r.getBigDecimal("GiaHieuLuc"),r.getObject("MaChuongTrinh",Long.class),r.getString("TenChuongTrinh"),r.getBoolean("ChoPhepKemMaGiamGia")))
            .list().forEach(g->result.put(g.sku(),g));return result;
    }
    public void chot(long order,Gia g) {
        if(g.chuongTrinh()==null)return;
        jdbc.sql("""
            INSERT INTO "ApDungKhuyenMai" ("MaChiTietDonHang","MaBienThe","MaChuongTrinh","TenChuongTrinhLucDat","GiaTruocKhuyenMai","GiaSauKhuyenMai","ChoPhepKemMaLucDat")
            SELECT "MaChiTietDonHang",:sku,:program,:name,:before,:after,:stack FROM "ChiTietDonHang" WHERE "MaDonHang"=:order AND "MaBienThe"=:sku
            """).param("order",order).param("sku",g.sku()).param("program",g.chuongTrinh()).param("name",g.ten()).param("before",g.giaGoc()).param("after",g.gia()).param("stack",g.kemMa()).update();
    }
    public List<Map<String,Object>> chuongTrinh() {return chuongTrinh(1);}
    public List<Map<String,Object>> chuongTrinh(int page) { return jdbc.sql("SELECT *,to_char(\"NgayBatDau\" AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY HH24:MI') AS bat_dau,to_char(\"NgayKetThuc\" AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY HH24:MI') AS ket_thuc FROM \"ChuongTrinhKhuyenMai\" ORDER BY \"MaChuongTrinh\" DESC LIMIT 30 OFFSET :offset").param("offset",(page-1)*30).query().listOfRows(); }
    public Map<String,Object> chuongTrinh(long id,boolean lock) { return mot("ChuongTrinhKhuyenMai","MaChuongTrinh",id,lock); }
    public Map<String,Object> voucher(long id,boolean lock) { return mot("MaGiamGia","MaGiamGia",id,lock); }
    public Optional<Map<String,Object>> voucherCongKhai(long id){return jdbc.sql("SELECT * FROM \"MaGiamGia\" WHERE \"MaGiamGia\"=:id AND \"CongKhai\" AND \"DangHoatDong\" AND \"NgayBatDau\"<=clock_timestamp() AND \"NgayKetThuc\">clock_timestamp()").param("id",id).query().listOfRows().stream().findFirst();}
    private Map<String,Object> mot(String table,String key,long id,boolean lock) {
        return jdbc.sql("SELECT * FROM \""+table+"\" WHERE \""+key+"\"=:id"+(lock?" FOR UPDATE":"")).param("id",id).query().listOfRows().stream().findFirst().orElseThrow(()->new vn.luma.business.KhongTimThayException("Không tìm thấy ưu đãi."));
    }
    public long luu(Long id,ChuongTrinhYeuCau f) {
        String sql=id==null?"INSERT INTO \"ChuongTrinhKhuyenMai\" (\"KyHieu\",\"TenChuongTrinh\",\"MoTa\",\"NgayBatDau\",\"NgayKetThuc\",\"DoUuTien\",\"TrangThai\",\"ChoPhepKemMaGiamGia\") VALUES (:code,:name,:description,:start,:end,:priority,:state,:stack) RETURNING \"MaChuongTrinh\"":
            "UPDATE \"ChuongTrinhKhuyenMai\" SET \"TenChuongTrinh\"=:name,\"MoTa\"=:description,\"NgayBatDau\"=:start,\"NgayKetThuc\"=:end,\"DoUuTien\"=:priority,\"TrangThai\"=:state,\"ChoPhepKemMaGiamGia\"=:stack WHERE \"MaChuongTrinh\"=:id RETURNING \"MaChuongTrinh\"";
        return jdbc.sql(sql).param("id",id).param("code",f.kyHieu()).param("name",f.ten()).param("description",f.moTa()).param("start",f.batDauTaiVN()).param("end",f.ketThucTaiVN()).param("priority",f.uuTien()).param("state",f.trangThai()).param("stack",f.kemMa()).query(Long.class).single();
    }
    public List<Map<String,Object>> quyTac(long id) { return jdbc.sql("SELECT a.*,b.\"MaHang\",b.\"TenBienThe\",b.\"GiaBan\",s.\"TenSanPham\" FROM \"SanPhamKhuyenMai\" a JOIN \"BienTheSanPham\" b USING (\"MaBienThe\") JOIN \"SanPham\" s USING (\"MaSanPham\") WHERE a.\"MaChuongTrinh\"=:id ORDER BY a.\"MaBienThe\"").param("id",id).query().listOfRows(); }
    public void quyTac(long id,QuyTacKhuyenMaiYeuCau f) {
        jdbc.sql("""
            INSERT INTO "SanPhamKhuyenMai" ("MaChuongTrinh","MaBienThe","KieuGiam","GiaTriGiam","DangHoatDong") VALUES (:id,:sku,:type,:value,:active)
            ON CONFLICT ("MaChuongTrinh","MaBienThe") DO UPDATE SET "KieuGiam"=excluded."KieuGiam","GiaTriGiam"=excluded."GiaTriGiam","DangHoatDong"=excluded."DangHoatDong"
            """).param("id",id).param("sku",f.sku()).param("type",f.kieu()).param("value",f.giaTri()).param("active",f.hoatDong()).update();
    }
    public BigDecimal giaGoc(long sku) { return jdbc.sql("SELECT \"GiaBan\" FROM \"BienTheSanPham\" WHERE \"MaBienThe\"=:id").param("id",sku).query(BigDecimal.class).optional().orElseThrow(()->new IllegalArgumentException("Không tìm thấy SKU.")); }
    public List<Map<String,Object>> vouchers(boolean publicOnly) {return vouchers(publicOnly,1);}
    public List<Map<String,Object>> vouchers(boolean publicOnly,int page) { return jdbc.sql("""
        SELECT v.*,to_char(v."NgayKetThuc" AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY HH24:MI') AS ket_thuc,
          v."TongLuotToiDa"-(SELECT count(*) FROM "DonHang" d WHERE d."MaGiamGia"=v."MaGiamGia" AND d."TrangThaiMaGiamGia" IN ('DANG_GIU','DA_DUNG')) AS luot_con
        FROM "MaGiamGia" v WHERE NOT :public OR (v."CongKhai" AND v."DangHoatDong" AND v."NgayBatDau"<=clock_timestamp() AND v."NgayKetThuc">clock_timestamp())
        ORDER BY v."MaGiamGia" DESC LIMIT 30 OFFSET :offset
        """).param("offset",(page-1)*30).param("public",publicOnly).query().listOfRows(); }
    public long luuVoucher(Long id,VoucherYeuCau f) {
        String sql=id==null?"INSERT INTO \"MaGiamGia\" (\"KyHieu\",\"MoTa\",\"KieuGiam\",\"GiaTriGiam\",\"GiamToiDa\",\"DonToiThieu\",\"NgayBatDau\",\"NgayKetThuc\",\"TongLuotToiDa\",\"LuotMoiKhach\",\"CongKhai\",\"DangHoatDong\",\"NgayCapNhat\") VALUES (:code,:description,:type,:value,:max,:min,:start,:end,:limit,:per,:public,:active,now()) RETURNING \"MaGiamGia\"":
            "UPDATE \"MaGiamGia\" SET \"MoTa\"=:description,\"KieuGiam\"=:type,\"GiaTriGiam\"=:value,\"GiamToiDa\"=:max,\"DonToiThieu\"=:min,\"NgayBatDau\"=:start,\"NgayKetThuc\"=:end,\"TongLuotToiDa\"=:limit,\"LuotMoiKhach\"=:per,\"CongKhai\"=:public,\"DangHoatDong\"=:active,\"NgayCapNhat\"=now() WHERE \"MaGiamGia\"=:id RETURNING \"MaGiamGia\"";
        return jdbc.sql(sql).param("id",id).param("code",f.kyHieu()).param("description",f.moTa()).param("type",f.kieu()).param("value",f.giaTri()).param("max",f.toiDa(),Types.NUMERIC).param("min",f.donToiThieu()).param("start",f.batDauTaiVN()).param("end",f.ketThucTaiVN()).param("limit",f.tongLuot()).param("per",f.moiKhach()).param("public",f.congKhai()).param("active",f.hoatDong()).query(Long.class).single();
    }
}
