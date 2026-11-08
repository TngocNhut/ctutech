package vn.luma.data;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.luma.web.XacNhanHoanYeuCau;

@Repository
public class HoanTienDAO {
    private final JdbcClient jdbc;
    public HoanTienDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    public List<Map<String,Object>> danhSach(){return danhSach(1);}
    public List<Map<String,Object>> danhSach(int page){return jdbc.sql("""
        SELECT h.*,d."MaDonCongKhai",p."CheDoXuLy" FROM "HoanTien" h JOIN "GiaoDichThanhToan" g USING ("MaGiaoDich")
        JOIN "PhienThanhToan" p USING ("MaPhienThanhToan") JOIN "DonHang" d USING ("MaDonHang") ORDER BY h."MaHoanTien" DESC LIMIT 30 OFFSET :offset
        """).param("offset",(page-1)*30).query().listOfRows();}
    public Map<String,Object> chiTiet(long id){return jdbc.sql("""
        SELECT h.*,g."MaPhienThanhToan",g."SoTien" AS "TienDaThu",p."MaDonHang",p."CheDoXuLy",d."MaDonCongKhai"
        FROM "HoanTien" h JOIN "GiaoDichThanhToan" g USING ("MaGiaoDich") JOIN "PhienThanhToan" p USING ("MaPhienThanhToan") JOIN "DonHang" d USING ("MaDonHang") WHERE h."MaHoanTien"=:id
        """).param("id",id).query().listOfRows().stream().findFirst().orElseThrow(()->new vn.luma.business.KhongTimThayException("Không tìm thấy khoản hoàn."));}
    public void khoaKhoanThu(long id){jdbc.sql("SELECT \"MaGiaoDich\" FROM \"GiaoDichThanhToan\" WHERE \"MaGiaoDich\"=:id FOR UPDATE").param("id",id).query(Long.class).single();}
    public Map<String,Object> khoa(long id){return jdbc.sql("SELECT * FROM \"HoanTien\" WHERE \"MaHoanTien\"=:id FOR UPDATE").param("id",id).query().singleRow();}
    public BigDecimal tongHoanDon(long order){return jdbc.sql("SELECT coalesce(sum(h.\"SoTienHoan\"),0) FROM \"HoanTien\" h JOIN \"GiaoDichThanhToan\" g USING (\"MaGiaoDich\") JOIN \"PhienThanhToan\" p USING (\"MaPhienThanhToan\") WHERE p.\"MaDonHang\"=:d AND h.\"TrangThai\"='THANH_CONG'").param("d",order).query(BigDecimal.class).single();}
    public BigDecimal tongThuDon(long order){return jdbc.sql("SELECT coalesce(sum(g.\"SoTien\"),0) FROM \"GiaoDichThanhToan\" g JOIN \"PhienThanhToan\" p USING (\"MaPhienThanhToan\") WHERE p.\"MaDonHang\"=:d").param("d",order).query(BigDecimal.class).single();}
    public boolean conChoHoan(long order){return jdbc.sql("SELECT EXISTS (SELECT 1 FROM \"HoanTien\" h JOIN \"GiaoDichThanhToan\" g USING (\"MaGiaoDich\") JOIN \"PhienThanhToan\" p USING (\"MaPhienThanhToan\") WHERE p.\"MaDonHang\"=:d AND h.\"TrangThai\"<>'THANH_CONG')").param("d",order).query(Boolean.class).single();}
    public void hoanThanh(long id,long actor,XacNhanHoanYeuCau form,String file){jdbc.sql("""
        UPDATE "HoanTien" SET "TrangThai"='THANH_CONG',"MaNguoiXuLy"=:u,"TaiKhoanNhanHoan"=:account,"NganHangNhanHoan"=:bank,"TenNguoiNhanHoan"=:name,
        "MaThamChieuHoan"=:ref,"DuongDanChungTu"=:file,"NgayHoanThanh"=now(),"NgayCapNhat"=now() WHERE "MaHoanTien"=:id
        """).param("id",id).param("u",actor).param("account",form.taiKhoan().strip()).param("bank",form.nganHang().strip()).param("name",form.nguoiNhan().strip()).param("ref",form.thamChieu().strip()).param("file",file).update();}
    public boolean trungThamChieu(String reference,long except){return jdbc.sql("SELECT EXISTS (SELECT 1 FROM \"HoanTien\" WHERE \"MaThamChieuHoan\"=:r AND \"TrangThai\"='THANH_CONG' AND \"MaHoanTien\"<>:id)").param("r",reference.strip()).param("id",except).query(Boolean.class).single();}
    public void loi(long id,long actor,String note){jdbc.sql("UPDATE \"HoanTien\" SET \"TrangThai\"='THAT_BAI',\"MaNguoiXuLy\"=:u,\"GhiChuXuLy\"=:n,\"NgayCapNhat\"=now() WHERE \"MaHoanTien\"=:id").param("id",id).param("u",actor).param("n",note).update();log(actor,id,"HOAN_TIEN_THAT_BAI");}
    public void thuLai(long id,long actor){jdbc.sql("UPDATE \"HoanTien\" SET \"TrangThai\"='CHO_XU_LY',\"NgayCapNhat\"=now() WHERE \"MaHoanTien\"=:id").param("id",id).update();log(actor,id,"HOAN_TIEN_THU_LAI");}
    public void log(long actor,long id,String action){jdbc.sql("INSERT INTO \"NhatKyQuanTri\" (\"MaNguoiThucHien\",\"HanhDong\",\"LoaiDoiTuong\",\"MaDoiTuong\") VALUES (:u,:action,'HOAN_TIEN',:id)").param("u",actor).param("action",action).param("id",Long.toString(id)).update();}
    public void trangThaiDon(long order,String state){jdbc.sql("UPDATE \"DonHang\" SET \"TrangThaiThanhToan\"=:s,\"NgayCapNhat\"=now() WHERE \"MaDonHang\"=:d").param("d",order).param("s",state).update();}
    public void duyetThuDu(long session,BigDecimal total,String note){jdbc.sql("INSERT INTO \"LanDoiSoatThanhToan\" (\"MaPhienThanhToan\",\"NguonKiemTra\",\"TongThuTuNguon\",\"TongThuTrongSo\",\"KetQua\",\"MaLyDo\",\"GhiChuQuanTri\") VALUES (:p,'QUAN_TRI',:amount,:amount,'KHOP','DUYET_THU_DU',:note)").param("p",session).param("amount",total).param("note",note).update();}
    public void chungTu(long refund){jdbc.sql("""
        INSERT INTO "ChungTuHoanTien" ("SoChungTu","MaHoaDon","MaHoanTien","SoTienHoan")
        SELECT 'LUMA-HOAN-'||h."MaHoanTien",i."MaHoaDon",h."MaHoanTien",h."SoTienHoan"
        FROM "HoanTien" h JOIN "GiaoDichThanhToan" g USING ("MaGiaoDich") JOIN "PhienThanhToan" p USING ("MaPhienThanhToan") JOIN "HoaDon" i USING ("MaDonHang")
        WHERE h."MaHoanTien"=:id AND h."TrangThai"='THANH_CONG' AND h."LoaiHoanTien" IN ('TRA_HANG','HUY_DON') ON CONFLICT ("MaHoanTien") DO NOTHING
        """).param("id",refund).update();}
    public List<Map<String,Object>> theoDon(long order){return jdbc.sql("""
        SELECT h."MaHoanTien",h."SoTienHoan",h."TrangThai",h."LyDo",h."LoaiHoanTien",h."MaPhieuTraHang",h."NgayHoanThanh",p."CheDoXuLy",c."SoChungTu"
        FROM "HoanTien" h JOIN "GiaoDichThanhToan" g USING ("MaGiaoDich") JOIN "PhienThanhToan" p USING ("MaPhienThanhToan") LEFT JOIN "ChungTuHoanTien" c USING ("MaHoanTien") WHERE p."MaDonHang"=:d ORDER BY h."MaHoanTien"
        """).param("d",order).query().listOfRows();}
    public void dongPhieuNeuDu(long id){jdbc.sql("""
        UPDATE "PhieuTraHang" f SET "TrangThai"='HOAN_THANH',"NgayDong"=now(),"NgayCapNhat"=now()
        WHERE f."MaPhieuTraHang"=:id AND f."TrangThai"='CHO_HOAN_TIEN' AND
          (SELECT coalesce(sum("SoTienHoan"),0) FROM "HoanTien" WHERE "MaPhieuTraHang"=:id AND "TrangThai"='THANH_CONG') >=
          f."PhiVanChuyenDuocHoan"+(SELECT coalesce(sum("SoTienDuocHoan"),0) FROM "ChiTietTraHang" WHERE "MaPhieuTraHang"=:id)
        """).param("id",id).update();jdbc.sql("UPDATE \"ViecXuLyGiaoNhan\" SET \"TrangThai\"='DA_XONG',\"NgayDong\"=now(),\"NgayCapNhat\"=now() WHERE \"MaPhieuTraHang\"=:id AND \"TrangThai\" IN ('MOI','DANG_XU_LY') AND EXISTS (SELECT 1 FROM \"PhieuTraHang\" WHERE \"MaPhieuTraHang\"=:id AND \"TrangThai\"='HOAN_THANH')").param("id",id).update();}
}
