package vn.luma.data;

import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.luma.web.NguoiDungYeuCau;

@Repository
public class QuanLyNguoiDungDAO {
    private final JdbcClient jdbc;
    public QuanLyNguoiDungDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    private static final String LOC="""
        FROM "NguoiDung" n JOIN "VaiTro" v USING ("MaVaiTro")
        WHERE (:q='' OR n."HoTen" ILIKE :pattern OR n."ThuDienTu" ILIKE :pattern)
        """;
    private JdbcClient.StatementSpec loc(String sql,String q){return jdbc.sql(sql).param("q",q).param("pattern","%"+q.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%");}
    public List<Map<String,Object>> nhanVien(String q,int page){return loc("SELECT n.\"MaNguoiDung\",n.\"HoTen\",n.\"ThuDienTu\",n.\"TrangThai\",n.\"NgayXacMinhThuDienTu\",v.\"TenVaiTro\" "+LOC+" AND v.\"TenVaiTro\"='NhanVien' ORDER BY n.\"MaNguoiDung\" DESC LIMIT 30 OFFSET :offset",q).param("offset",(page-1)*30).query().listOfRows();}
    public long demNhanVien(String q){return loc("SELECT count(*) "+LOC+" AND v.\"TenVaiTro\"='NhanVien'",q).query(Long.class).single();}
    public long dem(String q){return loc("SELECT count(*) "+LOC,q).query(Long.class).single();}
    public List<Map<String,Object>> danhSach(String q,int page){return loc("SELECT n.\"MaNguoiDung\",n.\"HoTen\",n.\"ThuDienTu\",n.\"TrangThai\",n.\"NgayXacMinhThuDienTu\",v.\"TenVaiTro\" "+LOC+" ORDER BY n.\"MaNguoiDung\" DESC LIMIT 30 OFFSET :offset",q).param("offset",(page-1)*30).query().listOfRows();}
    public Map<String,Object> tim(long id){return jdbc.sql("""
        SELECT n."MaNguoiDung",n."HoTen",n."ThuDienTu",n."TrangThai",n."NgayXacMinhThuDienTu",v."TenVaiTro",
          (SELECT count(*) FROM "PhienDangNhap" p WHERE p."MaNguoiDung"=n."MaNguoiDung" AND p."NgayThuHoi" IS NULL AND p."NgayHetHan">clock_timestamp()) AS phien_con
        FROM "NguoiDung" n JOIN "VaiTro" v USING ("MaVaiTro") WHERE n."MaNguoiDung"=:id
        """).param("id",id).query().listOfRows().stream().findFirst().orElseThrow(()->new vn.luma.business.KhongTimThayException("Không tìm thấy người dùng."));}
    public void khoaQuanTri(){jdbc.sql("SELECT pg_advisory_xact_lock(747051013)").query().singleRow();}
    public long soQuanTri(){return jdbc.sql("SELECT count(*) FROM \"NguoiDung\" n JOIN \"VaiTro\" v USING (\"MaVaiTro\") WHERE v.\"TenVaiTro\"='QuanTri' AND n.\"TrangThai\"='HOAT_DONG'").query(Long.class).single();}
    public void luu(long id,NguoiDungYeuCau f){jdbc.sql("UPDATE \"NguoiDung\" SET \"MaVaiTro\"=(SELECT \"MaVaiTro\" FROM \"VaiTro\" WHERE \"TenVaiTro\"=:role),\"TrangThai\"=:state,\"NgayCapNhat\"=now() WHERE \"MaNguoiDung\"=:id").param("id",id).param("role",f.vaiTro()).param("state",f.trangThai()).update();}
    public int thuHoi(long id){return jdbc.sql("UPDATE \"PhienDangNhap\" SET \"NgayThuHoi\"=now() WHERE \"MaNguoiDung\"=:id AND \"NgayThuHoi\" IS NULL").param("id",id).update();}

    public void baoMat(Long user,Long session,String type,String result,String reason){jdbc.sql("INSERT INTO \"NhatKyBaoMat\" (\"MaNguoiDung\",\"MaPhienDangNhap\",\"LoaiSuKien\",\"KetQua\",\"MaLyDo\") VALUES (:u,:session,:type,:result,:reason)").param("u",user,java.sql.Types.BIGINT).param("session",session,java.sql.Types.BIGINT).param("type",type).param("result",result).param("reason",reason).update();}
    public List<Map<String,Object>> nhatKy(boolean security,int page){return jdbc.sql(security?"SELECT a.\"MaSuKienBaoMat\" AS ma,a.\"LoaiSuKien\" AS hanh_dong,a.\"KetQua\" AS doi_tuong,a.\"MaLyDo\" AS ly_do,a.\"MaNguoiDung\" AS nguoi,to_char(a.\"NgayTao\" AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY HH24:MI:SS') AS ngay FROM \"NhatKyBaoMat\" a ORDER BY a.\"MaSuKienBaoMat\" DESC LIMIT 50 OFFSET :offset":"SELECT a.\"MaNhatKy\" AS ma,a.\"HanhDong\" AS hanh_dong,a.\"LoaiDoiTuong\" AS doi_tuong,coalesce(a.\"GhiChu\",a.\"MaDoiTuong\") AS ly_do,a.\"MaNguoiThucHien\" AS nguoi,to_char(a.\"NgayTao\" AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY HH24:MI:SS') AS ngay FROM \"NhatKyQuanTri\" a ORDER BY a.\"MaNhatKy\" DESC LIMIT 50 OFFSET :offset").param("offset",(page-1)*50).query().listOfRows();}
}
