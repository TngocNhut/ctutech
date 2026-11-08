package vn.luma.data;

import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.luma.web.*;

@Repository
public class DanhGiaDAO {
    private final JdbcClient jdbc;
    public DanhGiaDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    public Optional<Map<String,Object>> bangChung(long user,long product){return jdbc.sql("""
        SELECT c."MaChiTietDonHang",c."TenSanPhamLucDat" FROM "DonHang" d JOIN "ChiTietDonHang" c USING ("MaDonHang")
        JOIN "BienTheSanPham" b USING ("MaBienThe") WHERE d."MaNguoiDung"=:u AND b."MaSanPham"=:p
          AND d."TrangThaiDon"='HOAN_THANH' AND d."MaPhienThanhToanChapNhan" IS NOT NULL
        ORDER BY d."NgayHoanThanh" DESC,d."MaDonHang" DESC,c."MaChiTietDonHang" LIMIT 1
        """).param("u",user).param("p",product).query().listOfRows().stream().findFirst();}
    public List<Map<String,Object>> daMua(long user,int page){return jdbc.sql("""
        SELECT b."MaSanPham",(array_agg(c."TenSanPhamLucDat" ORDER BY d."NgayHoanThanh" DESC,d."MaDonHang" DESC))[1] AS ten,
          g."MaDanhGia",g."TrangThai",g."SoSao"
        FROM "DonHang" d JOIN "ChiTietDonHang" c USING ("MaDonHang") JOIN "BienTheSanPham" b USING ("MaBienThe")
        LEFT JOIN "DanhGia" g ON g."MaNguoiDung"=d."MaNguoiDung" AND g."MaSanPham"=b."MaSanPham"
        WHERE d."MaNguoiDung"=:u AND d."TrangThaiDon"='HOAN_THANH' AND d."MaPhienThanhToanChapNhan" IS NOT NULL
        GROUP BY b."MaSanPham",g."MaDanhGia" ORDER BY max(d."NgayHoanThanh") DESC,b."MaSanPham" DESC LIMIT 30 OFFSET :offset
        """).param("u",user).param("offset",(page-1)*30).query().listOfRows();}
    public Optional<Map<String,Object>> cuaToi(long user,long product){return jdbc.sql("SELECT * FROM \"DanhGia\" WHERE \"MaNguoiDung\"=:u AND \"MaSanPham\"=:p").param("u",user).param("p",product).query().listOfRows().stream().findFirst();}
    public void luu(long user,long product,long proof,DanhGiaYeuCau f){jdbc.sql("""
        INSERT INTO "DanhGia" ("MaNguoiDung","MaSanPham","MaChiTietDonHang","SoSao","TieuDe","NoiDung","TrangThai","NgayCapNhat")
        VALUES (:u,:p,:proof,:stars,:title,:body,'HIEN_THI',now())
        ON CONFLICT ("MaNguoiDung","MaSanPham") DO UPDATE SET "SoSao"=excluded."SoSao","TieuDe"=excluded."TieuDe","NoiDung"=excluded."NoiDung","NgayCapNhat"=now()
        """).param("u",user).param("p",product).param("proof",proof).param("stars",f.soSao()).param("title",f.tieuDe()==null?null:f.tieuDe().strip()).param("body",f.noiDung().strip()).update();}
    public Map<String,Object> tongHop(long product){return jdbc.sql("""
        SELECT count(*) AS so_luot,round(avg("SoSao"),1) AS trung_binh,
          count(*) FILTER(WHERE "SoSao"=5) AS sao_5,count(*) FILTER(WHERE "SoSao"=4) AS sao_4,
          count(*) FILTER(WHERE "SoSao"=3) AS sao_3,count(*) FILTER(WHERE "SoSao"=2) AS sao_2,count(*) FILTER(WHERE "SoSao"=1) AS sao_1
        FROM "DanhGia" WHERE "MaSanPham"=:p AND "TrangThai"='HIEN_THI'
        """).param("p",product).query().singleRow();}
    public List<Map<String,Object>> congKhai(long product,int page,int size){return jdbc.sql("""
        SELECT g."SoSao",g."TieuDe",g."NoiDung",g."PhanHoiQuanTri",regexp_replace(trim(n."HoTen"),'^.*[[:space:]]','') AS ten_hien_thi,
          to_char(g."NgayCapNhat" AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY') AS ngay
        FROM "DanhGia" g JOIN "NguoiDung" n USING ("MaNguoiDung") WHERE g."MaSanPham"=:p AND g."TrangThai"='HIEN_THI'
        ORDER BY g."NgayCapNhat" DESC,g."MaDanhGia" DESC LIMIT :size OFFSET :offset
        """).param("p",product).param("size",size).param("offset",(page-1)*size).query().listOfRows();}
    private static final String LOC="""
        FROM "DanhGia" g JOIN "SanPham" s USING ("MaSanPham") JOIN "NguoiDung" n USING ("MaNguoiDung")
        WHERE (:state='TAT_CA' OR g."TrangThai"=:state) AND (:q='' OR s."TenSanPham" ILIKE :pattern OR n."HoTen" ILIKE :pattern)
        """;
    private JdbcClient.StatementSpec loc(String sql,String state,String q){return jdbc.sql(sql).param("state",state).param("q",q).param("pattern","%"+q.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%");}
    public List<Map<String,Object>> quanTri(String state,String q,int page){return loc("SELECT g.*,s.\"TenSanPham\",n.\"HoTen\" "+LOC+" ORDER BY g.\"NgayCapNhat\" DESC,g.\"MaDanhGia\" DESC LIMIT 30 OFFSET :offset",state,q).param("offset",(page-1)*30).query().listOfRows();}
    public long dem(String state,String q){return loc("SELECT count(*) "+LOC,state,q).query(Long.class).single();}
    public Map<String,Object> tim(long id,boolean lock){return jdbc.sql("""
        SELECT g.*,s."TenSanPham",n."HoTen" FROM "DanhGia" g JOIN "SanPham" s USING ("MaSanPham") JOIN "NguoiDung" n USING ("MaNguoiDung")
        WHERE g."MaDanhGia"=:id
        """+(lock?" FOR UPDATE OF g":"")).param("id",id).query().listOfRows().stream().findFirst().orElseThrow(()->new vn.luma.business.KhongTimThayException("Không tìm thấy đánh giá."));}
    public void duyet(long id,long actor,KiemDuyetDanhGiaYeuCau f){String reply=f.phanHoi()==null||f.phanHoi().isBlank()?null:f.phanHoi().strip();jdbc.sql("""
        UPDATE "DanhGia" SET "TrangThai"=:state,"PhanHoiQuanTri"=:reply,"MaQuanTriPhanHoi"=:actor,
          "NgayPhanHoi"=CASE WHEN :hasReply THEN clock_timestamp() ELSE NULL END,"NgayCapNhat"=clock_timestamp() WHERE "MaDanhGia"=:id
        """).param("id",id).param("actor",reply==null?null:actor,java.sql.Types.BIGINT).param("hasReply",reply!=null).param("reply",reply).param("state",f.hienThi()?"HIEN_THI":"BI_AN").update();}
}
