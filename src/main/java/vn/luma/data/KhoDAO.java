package vn.luma.data;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.luma.web.CapNhatBienThe;

@Repository
public class KhoDAO {
    private final JdbcClient jdbc;
    public KhoDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    public Map<String,Object> khoa(long id){
        return jdbc.sql("""
            SELECT b."GiaBan",b."GiaNiemYet",b."GiaVon",t."SoLuongTon",t."SoLuongGiu"
            FROM "BienTheSanPham" b JOIN "TonKho" t USING ("MaBienThe")
            WHERE b."MaBienThe"=:id AND t."MaKhoHang"=1 FOR UPDATE OF b,t
            """).param("id",id).query().listOfRows().stream().findFirst().orElseThrow(()->new vn.luma.business.KhongTimThayException("SKU không tồn tại."));
    }
    public void ghi(long id,CapNhatBienThe form,long actor,int delta,boolean priceChanged,String requestId){
        jdbc.sql("UPDATE \"BienTheSanPham\" SET \"GiaBan\"=:gia,\"GiaNiemYet\"=:niem,\"GiaVon\"=:von,\"NgayCapNhat\"=now() WHERE \"MaBienThe\"=:id")
            .param("gia",form.giaBan()).param("niem",form.giaNiemYet()).param("von",form.giaVon()).param("id",id).update();
        if(priceChanged) jdbc.sql("""
            INSERT INTO "LichSuGiaBienThe" ("MaBienThe","MaNguoiCapNhat","GiaBan","GiaNiemYet","LyDo")
            VALUES (:id,:actor,:gia,:niem,:lydo)
            """).param("id",id).param("actor",actor).param("gia",form.giaBan()).param("niem",form.giaNiemYet()).param("lydo",form.lyDo()).update();
        if(delta!=0){
            jdbc.sql("UPDATE \"TonKho\" SET \"SoLuongTon\"=:ton,\"NgayCapNhat\"=now() WHERE \"MaKhoHang\"=1 AND \"MaBienThe\"=:id")
                .param("ton",form.soLuongTon()).param("id",id).update();
            jdbc.sql("""
                INSERT INTO "BienDongKho" ("MaKhoHang","MaBienThe","MaNguoiThucHien","LoaiBienDong","ThayDoiTon","ThayDoiGiu","KhoaChongLap","GhiChu")
                VALUES (1,:id,:actor,'DIEU_CHINH',:delta,0,:key,:reason)
                """).param("id",id).param("actor",actor).param("delta",delta).param("key",requestId).param("reason",form.lyDo()).update();
        }
        jdbc.sql("""
            INSERT INTO "NhatKyQuanTri" ("MaNguoiThucHien","HanhDong","LoaiDoiTuong","MaDoiTuong","KhoaYeuCau")
            VALUES (:actor,'CAP_NHAT_GIA_TON','BIEN_THE',:id,:key)
            """).param("actor",actor).param("id",Long.toString(id)).param("key",requestId).update();
    }
}
