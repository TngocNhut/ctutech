package vn.luma.data;

import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DoiSoatDAO {
    private final JdbcClient jdbc;
    public DoiSoatDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    private static final String LOC="""
        FROM "PhienThanhToan" p JOIN "DonHang" d USING ("MaDonHang")
        WHERE (:state='TAT_CA' OR (:state='CAN_KIEM_TRA' AND (p."MaLoi" IS NOT NULL OR p."TrangThaiXuLy" IN ('THU_THIEU','THU_DU','CAN_DOI_SOAT')))
            OR p."TrangThaiXuLy"=:state)
          AND (:q='' OR d."MaDonCongKhai" ILIKE :pattern OR CAST(p."MaDonPayOS" AS text)=:q)
        """;
    private JdbcClient.StatementSpec loc(String sql,String state,String q){return jdbc.sql(sql).param("state",state).param("q",q).param("pattern","%"+q.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%");}
    public long dem(String state,String q){return loc("SELECT count(*) "+LOC,state,q).query(Long.class).single();}
    public List<Map<String,Object>> danhSach(String state,String q,int page){return loc("""
        SELECT p."MaPhienThanhToan",p."MaDonPayOS",p."NhaCungCap",p."CheDoXuLy",p."TrangThaiXuLy",p."MaLoi",p."SoTienYeuCau",d."MaDonCongKhai",
          (SELECT coalesce(sum(g."SoTien"),0) FROM "GiaoDichThanhToan" g WHERE g."MaPhienThanhToan"=p."MaPhienThanhToan") AS da_thu
        """+LOC+" ORDER BY p.\"MaPhienThanhToan\" DESC LIMIT 30 OFFSET :offset",state,q).param("offset",(page-1)*30).query().listOfRows();}
    public Map<String,Object> phien(long id){return jdbc.sql("""
        SELECT p.*,d."MaDonCongKhai",d."TongThanhToan",d."TrangThaiDon",d."TrangThaiThanhToan",d."MaPhienThanhToanChapNhan"
        FROM "PhienThanhToan" p JOIN "DonHang" d USING ("MaDonHang") WHERE p."MaPhienThanhToan"=:id
        """).param("id",id).query().listOfRows().stream().findFirst().orElseThrow(()->new vn.luma.business.KhongTimThayException("Không tìm thấy phiên đối soát."));}
    public List<Map<String,Object>> thu(long id){return jdbc.sql("""
        SELECT g.*,coalesce(sum(h."SoTienHoan"),0) AS han_muc_hoan,
          coalesce(sum(h."SoTienHoan") FILTER (WHERE h."TrangThai"='THANH_CONG'),0) AS da_hoan
        FROM "GiaoDichThanhToan" g LEFT JOIN "HoanTien" h USING ("MaGiaoDich") WHERE g."MaPhienThanhToan"=:id
        GROUP BY g."MaGiaoDich" ORDER BY g."MaGiaoDich"
        """).param("id",id).query().listOfRows();}
    public List<Map<String,Object>> suKien(long id,int page){return jdbc.sql("""
        SELECT "MaSuKien","MaGiaoDich","KhoaSuKienNgoai","LoaiSuKien","TrangThaiXuLy","NoiDungRutGon"::text AS noi_dung,
          to_char("NgayTao" AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY HH24:MI:SS') AS thoi_diem
        FROM "SuKienThanhToan" WHERE "MaPhienThanhToan"=:id ORDER BY "MaSuKien" DESC LIMIT 50 OFFSET :offset
        """).param("id",id).param("offset",(page-1)*50).query().listOfRows();}
    public List<Map<String,Object>> lichSu(long id,int page){return jdbc.sql("""
        SELECT *,to_char("NgayKiemTra" AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY HH24:MI:SS') AS thoi_diem
        FROM "LanDoiSoatThanhToan" WHERE "MaPhienThanhToan"=:id ORDER BY "MaLanDoiSoat" DESC LIMIT 50 OFFSET :offset
        """).param("id",id).param("offset",(page-1)*50).query().listOfRows();}
    public List<Map<String,Object>> chuaGan(int page){return jdbc.sql("""
        SELECT "MaSuKien","MaKenhThanhToan","KhoaSuKienNgoai","TrangThaiXuLy","NoiDungRutGon"::text AS noi_dung,
          to_char("NgayTao" AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY HH24:MI:SS') AS thoi_diem
        FROM "SuKienThanhToan" WHERE "MaPhienThanhToan" IS NULL ORDER BY "MaSuKien" DESC LIMIT 50 OFFSET :offset
        """).param("offset",(page-1)*50).query().listOfRows();}
    public void ghiChu(long id,long actor,String note){
        jdbc.sql("SELECT \"MaPhienThanhToan\" FROM \"PhienThanhToan\" WHERE \"MaPhienThanhToan\"=:id FOR UPDATE").param("id",id).query(Long.class).single();
        jdbc.sql("""
            INSERT INTO "LanDoiSoatThanhToan" ("MaPhienThanhToan","MaNguoiThucHien","NguonKiemTra","TongThuTrongSo","KetQua","MaLyDo","GhiChuQuanTri")
            SELECT :id,:actor,'QUAN_TRI',coalesce(sum("SoTien"),0),'CHUA_RO','GHI_CHU_QUAN_TRI',:note
            FROM "GiaoDichThanhToan" WHERE "MaPhienThanhToan"=:id
            """).param("id",id).param("actor",actor).param("note",note).update();
    }
}
