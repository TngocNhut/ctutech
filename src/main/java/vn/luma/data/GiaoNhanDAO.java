package vn.luma.data;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.luma.web.TaoVanDonYeuCau;

@Repository
public class GiaoNhanDAO {
    private final JdbcClient jdbc;
    public GiaoNhanDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    public List<Map<String,Object>> donHang(String state){return donHang(state,1);}
    public List<Map<String,Object>> donHang(String state,int page){return jdbc.sql("""
        SELECT d.*,v."MaTheoDoi",v."TrangThai" AS "TrangThaiVanDon" FROM "DonHang" d
        LEFT JOIN "VanDon" v ON v."MaDonHang"=d."MaDonHang" AND v."LoaiVanDon"='GIAO_DI'
        WHERE (:state='' OR d."TrangThaiDon"=:state) ORDER BY d."MaDonHang" DESC LIMIT 30 OFFSET :offset
        """).param("offset",(page-1)*30).param("state",state).query().listOfRows();}
    public List<Map<String,Object>> donVi(){return jdbc.sql("SELECT * FROM \"DonViVanChuyen\" WHERE \"DangHoatDong\" ORDER BY \"MaDonViVanChuyen\"").query().listOfRows();}
    public Optional<Map<String,Object>> vanDon(long order,boolean lock){return jdbc.sql("SELECT * FROM \"VanDon\" WHERE \"MaDonHang\"=:d AND \"LoaiVanDon\"='GIAO_DI'"+(lock?" FOR UPDATE":"")).param("d",order).query().listOfRows().stream().findFirst();}
    public Map<String,Object> vanDonTheoId(long waybill,boolean lock){return jdbc.sql("SELECT * FROM \"VanDon\" WHERE \"MaVanDon\"=:id"+(lock?" FOR UPDATE":"")).param("id",waybill).query().listOfRows().stream().findFirst().orElseThrow(()->new vn.luma.business.KhongTimThayException("Không tìm thấy vận đơn."));}
    public List<Map<String,Object>> lichSu(long waybill){return jdbc.sql("SELECT *,to_char(\"NgayGhiNhan\" AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY HH24:MI') AS \"ThoiDiem\" FROM \"SuKienVanDon\" WHERE \"MaVanDon\"=:v ORDER BY \"MaSuKienVanDon\"").param("v",waybill).query().listOfRows();}
    public Optional<Map<String,Object>> suKien(long waybill,String key){return jdbc.sql("SELECT * FROM \"SuKienVanDon\" WHERE \"MaVanDon\"=:v AND \"KhoaSuKien\"=:k").param("v",waybill).param("k",key).query().listOfRows().stream().findFirst();}
    public BigDecimal cuocGoc(long waybill){return jdbc.sql("SELECT \"SoTien\" FROM \"PhiVanChuyen\" WHERE \"MaVanDon\"=:v AND \"LoaiPhi\"='CUOC_GOC'").param("v",waybill).query(BigDecimal.class).optional().orElse(null);}
    public List<Map<String,Object>> cuoc(long waybill){return jdbc.sql("SELECT * FROM \"PhiVanChuyen\" WHERE \"MaVanDon\"=:v ORDER BY \"MaPhiVanChuyen\"").param("v",waybill).query().listOfRows();}
    public void themCuoc(long waybill,vn.luma.web.PhiVanChuyenYeuCau form){jdbc.sql("INSERT INTO \"PhiVanChuyen\" (\"MaVanDon\",\"LoaiPhi\",\"SoTien\",\"KhoaChongLap\",\"GhiChu\") VALUES (:v,:type,:amount,:key,:note)").param("v",waybill).param("type",form.loaiPhi()).param("amount",form.soTien()).param("key",form.khoaChongLap()).param("note",form.ghiChu().strip()).update();}
    public List<String> quaHan(){return jdbc.sql("SELECT d.\"MaDonCongKhai\" FROM \"VanDon\" v JOIN \"DonHang\" d USING (\"MaDonHang\") WHERE v.\"LoaiVanDon\"='GIAO_DI' AND v.\"TrangThai\" IN ('DANG_VAN_CHUYEN','CHO_GIAO_HANG') AND v.\"NgayDuKienGiao\"<clock_timestamp() ORDER BY v.\"NgayDuKienGiao\" LIMIT 100").query(String.class).list();}
    public boolean daQuaHan(long shipment){return jdbc.sql("SELECT coalesce(\"NgayDuKienGiao\"<clock_timestamp(),false) FROM \"VanDon\" WHERE \"MaVanDon\"=:id").param("id",shipment).query(Boolean.class).single();}
    public long tao(long order,TaoVanDonYeuCau form){
        jdbc.sql("SELECT \"MaDonViVanChuyen\" FROM \"DonViVanChuyen\" WHERE \"MaDonViVanChuyen\"=:c AND \"DangHoatDong\" FOR SHARE").param("c",form.maDonViVanChuyen()).query(Long.class).optional().orElseThrow(()->new IllegalArgumentException("Đơn vị vận chuyển không khả dụng."));
        long id=jdbc.sql("INSERT INTO \"VanDon\" (\"MaDonHang\",\"MaDonViVanChuyen\",\"LoaiVanDon\",\"MaTheoDoi\",\"NgayDuKienGiao\") VALUES (:d,:c,'GIAO_DI',:tracking,:date) RETURNING \"MaVanDon\"")
            .param("d",order).param("c",form.maDonViVanChuyen()).param("tracking",form.maTheoDoi().strip()).param("date",form.ngayDuKien()==null?null:form.ngayDuKien().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime(),java.sql.Types.TIMESTAMP_WITH_TIMEZONE).query(Long.class).single();
        jdbc.sql("INSERT INTO \"ChiTietVanDon\" (\"MaVanDon\",\"MaDonHang\",\"LoaiVanDon\",\"MaChiTietDonHang\",\"SoLuongDuKien\") SELECT :v,\"MaDonHang\",'GIAO_DI',\"MaChiTietDonHang\",\"SoLuong\" FROM \"ChiTietDonHang\" WHERE \"MaDonHang\"=:d").param("v",id).param("d",order).update();
        if(form.cuocGoc()!=null)jdbc.sql("INSERT INTO \"PhiVanChuyen\" (\"MaVanDon\",\"LoaiPhi\",\"SoTien\",\"KhoaChongLap\") VALUES (:v,'CUOC_GOC',:amount,'create:base')").param("v",id).param("amount",form.cuocGoc()).update();return id;
    }
    public void capNhatVanDon(long id,String state){jdbc.sql("UPDATE \"VanDon\" SET \"TrangThai\"=:s,\"NgayCapNhat\"=now() WHERE \"MaVanDon\"=:v").param("v",id).param("s",state).update();}
    public void capNhatDon(long order,String state,boolean finish){jdbc.sql("UPDATE \"DonHang\" SET \"TrangThaiDon\"=:s,\"NgayHoanThanh\"=CASE WHEN :finish THEN coalesce(\"NgayHoanThanh\",now()) ELSE \"NgayHoanThanh\" END,\"NgayCapNhat\"=now() WHERE \"MaDonHang\"=:d").param("d",order).param("s",state).param("finish",finish).update();}
    public void xuatKho(long order,long sku,int quantity,long actor){
        if(jdbc.sql("UPDATE \"TonKho\" SET \"SoLuongTon\"=\"SoLuongTon\"-:q,\"SoLuongGiu\"=\"SoLuongGiu\"-:q,\"NgayCapNhat\"=now() WHERE \"MaKhoHang\"=1 AND \"MaBienThe\"=:s AND \"SoLuongTon\">=:q AND \"SoLuongGiu\">=:q").param("q",quantity).param("s",sku).update()!=1)throw new IllegalStateException("Tồn hoặc giữ hàng không đủ để bàn giao.");
        jdbc.sql("INSERT INTO \"BienDongKho\" (\"MaKhoHang\",\"MaBienThe\",\"MaDonHang\",\"MaNguoiThucHien\",\"LoaiBienDong\",\"ThayDoiTon\",\"ThayDoiGiu\",\"KhoaChongLap\") VALUES (1,:s,:d,:u,'XUAT_DON',:delta,:delta,:key)").param("s",sku).param("d",order).param("u",actor).param("delta",-quantity).param("key","handoff:"+order).update();
    }
    public void daXuat(long order){jdbc.sql("UPDATE \"DonHang\" SET \"TrangThaiKho\"='DA_XUAT' WHERE \"MaDonHang\"=:d").param("d",order).update();}
    public void suKien(long waybill,String key,String state,String note,long actor){jdbc.sql("INSERT INTO \"SuKienVanDon\" (\"MaVanDon\",\"KhoaSuKien\",\"NguonSuKien\",\"TrangThaiGhiNhan\",\"MaNguoiGhi\",\"NgayPhatSinh\",\"GhiChu\") VALUES (:v,:k,'NOI_BO',:s,:u,now(),:n)").param("v",waybill).param("k",key).param("s",state).param("u",actor).param("n",note).update();}
    public void nhatKy(long actor,long order,String action){jdbc.sql("INSERT INTO \"NhatKyQuanTri\" (\"MaNguoiThucHien\",\"HanhDong\",\"LoaiDoiTuong\",\"MaDoiTuong\") VALUES (:u,:action,'DON_HANG',:id)").param("u",actor).param("action",action).param("id",Long.toString(order)).update();}
    public void taoViec(long waybill,String type,String note){jdbc.sql("""
        INSERT INTO "ViecXuLyGiaoNhan" ("MaVanDon","LoaiViec","HanXuLy","GhiChu")
        SELECT :v,:t,clock_timestamp()+interval '24 hours',:n
        WHERE NOT EXISTS (SELECT 1 FROM "ViecXuLyGiaoNhan" WHERE "MaVanDon"=:v AND "LoaiViec"=:t AND "TrangThai" IN ('MOI','DANG_XU_LY'))
          AND NOT (:t='QUA_HAN_GIAO' AND EXISTS (
            SELECT 1 FROM "ViecXuLyGiaoNhan" x JOIN "VanDon" v USING ("MaVanDon")
            WHERE x."MaVanDon"=:v AND x."LoaiViec"=:t AND x."TrangThai" IN ('DA_XONG','DA_HUY')
              AND x."NgayDong">=v."NgayCapNhat" AND x."NgayDong">=v."NgayDuKienGiao"))
        """).param("v",waybill).param("t",type).param("n",note).update();}
    public void dongViec(long waybill){jdbc.sql("UPDATE \"ViecXuLyGiaoNhan\" SET \"TrangThai\"='DA_XONG',\"NgayDong\"=now(),\"NgayCapNhat\"=now() WHERE \"MaVanDon\"=:v AND \"TrangThai\" IN ('MOI','DANG_XU_LY')").param("v",waybill).update();}
    public List<Map<String,Object>> viec(){return viec(false,1);}
    public List<Map<String,Object>> viec(boolean closed,int page){return jdbc.sql("""
        SELECT x.*,v."MaTheoDoi",d."MaDonCongKhai",n."HoTen" AS "NguoiPhuTrach"
        FROM "ViecXuLyGiaoNhan" x LEFT JOIN "VanDon" v ON v."MaVanDon"=x."MaVanDon"
        LEFT JOIN "PhieuTraHang" f ON f."MaPhieuTraHang"=x."MaPhieuTraHang"
        JOIN "DonHang" d ON d."MaDonHang"=coalesce(v."MaDonHang",f."MaDonHang")
        LEFT JOIN "NguoiDung" n ON n."MaNguoiDung"=x."MaQuanTriPhuTrach"
        WHERE (:closed AND x."TrangThai" IN ('DA_XONG','DA_HUY')) OR (NOT :closed AND x."TrangThai" IN ('MOI','DANG_XU_LY')) ORDER BY CASE WHEN :closed THEN x."NgayDong" END DESC NULLS LAST,x."HanXuLy" NULLS LAST,x."MaViecXuLy" DESC LIMIT 30 OFFSET :offset
        """).param("closed",closed).param("offset",(page-1)*30).query().listOfRows();}
    public void nhanViec(long task,long actor){if(jdbc.sql("UPDATE \"ViecXuLyGiaoNhan\" SET \"TrangThai\"='DANG_XU_LY',\"MaQuanTriPhuTrach\"=:u,\"NgayCapNhat\"=now() WHERE \"MaViecXuLy\"=:id AND \"TrangThai\" IN ('MOI','DANG_XU_LY') AND (\"MaQuanTriPhuTrach\" IS NULL OR \"MaQuanTriPhuTrach\"=:u)").param("id",task).param("u",actor).update()!=1)throw new IllegalArgumentException("Việc đã đóng hoặc có người khác phụ trách.");}
    public Map<String,Object> viec(long task,boolean lock){return jdbc.sql("""
        SELECT x.*,v."MaTheoDoi",d."MaDonCongKhai",n."HoTen" AS "NguoiPhuTrach"
        FROM "ViecXuLyGiaoNhan" x LEFT JOIN "VanDon" v ON v."MaVanDon"=x."MaVanDon"
        LEFT JOIN "PhieuTraHang" f ON f."MaPhieuTraHang"=x."MaPhieuTraHang"
        JOIN "DonHang" d ON d."MaDonHang"=coalesce(v."MaDonHang",f."MaDonHang")
        LEFT JOIN "NguoiDung" n ON n."MaNguoiDung"=x."MaQuanTriPhuTrach"
        WHERE x."MaViecXuLy"=:id
        """+(lock?" FOR UPDATE OF x":"")).param("id",task).query().listOfRows().stream().findFirst().orElseThrow(()->new vn.luma.business.KhongTimThayException("Không tìm thấy việc giao nhận."));}
    public void suaViec(long task,long actor,java.time.OffsetDateTime deadline,boolean closed){jdbc.sql("""
        UPDATE "ViecXuLyGiaoNhan" SET "MaQuanTriPhuTrach"=:actor,"HanXuLy"=:deadline,"TrangThai"=:state,
          "NgayDong"=CASE WHEN :closed THEN clock_timestamp() ELSE NULL END,"NgayCapNhat"=clock_timestamp() WHERE "MaViecXuLy"=:id
        """).param("id",task).param("actor",actor).param("deadline",deadline,java.sql.Types.TIMESTAMP_WITH_TIMEZONE).param("state",closed?"DA_XONG":"DANG_XU_LY").param("closed",closed).update();}
    public List<Map<String,Object>> ghiChuViec(long task,int page){return jdbc.sql("""
        SELECT a."HanhDong",a."GhiChu",n."HoTen",to_char(a."NgayTao" AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY HH24:MI:SS') AS thoi_diem
        FROM "NhatKyQuanTri" a JOIN "NguoiDung" n ON n."MaNguoiDung"=a."MaNguoiThucHien"
        WHERE a."LoaiDoiTuong"='VIEC_GIAO_NHAN' AND a."MaDoiTuong"=:id ORDER BY a."MaNhatKy" DESC LIMIT 50 OFFSET :offset
        """).param("id",Long.toString(task)).param("offset",(page-1)*50).query().listOfRows();}
    public Optional<Map<String,Object>> codDaThu(long order){return jdbc.sql("SELECT g.*,e.\"NoiDungRutGon\"->>'ChungTu' AS \"ChungTu\" FROM \"PhienThanhToan\" p JOIN \"GiaoDichThanhToan\" g USING (\"MaPhienThanhToan\") LEFT JOIN \"SuKienThanhToan\" e ON e.\"MaGiaoDich\"=g.\"MaGiaoDich\" WHERE p.\"MaDonHang\"=:d AND p.\"NhaCungCap\"='COD'").param("d",order).query().listOfRows().stream().findFirst();}
    public long thuCOD(long order,BigDecimal amount,String proof,long actor){
        long session=jdbc.sql("""
            INSERT INTO "PhienThanhToan" ("MaDonHang","NhaCungCap","CheDoXuLy","MaKenhThanhToan","KhoaChongLap","SoTienYeuCau","MaTienTe","TrangThaiXuLy","NgayHetHan","NgayDuTien","NgayCapNhat")
            SELECT "MaDonHang",'COD','MO_PHONG','LUMA_COD_MOCK',:key,"TongThanhToan",'VND','DU_TIEN',"HanXuLy",now(),now() FROM "DonHang" WHERE "MaDonHang"=:d RETURNING "MaPhienThanhToan"
            """).param("d",order).param("key","cod:"+order).query(Long.class).single();
        long receipt=jdbc.sql("""
            INSERT INTO "GiaoDichThanhToan" ("MaPhienThanhToan","NhaCungCap","MaKenhThanhToan","KhoaChongLap","SoTien","MaTienTe","NgayGiaoDich","NgayXacMinh")
            VALUES (:p,'COD','LUMA_COD_MOCK',:key,:amount,'VND',now(),now()) RETURNING "MaGiaoDich"
            """).param("p",session).param("key","cod-receipt:"+order).param("amount",amount).query(Long.class).single();
        jdbc.sql("""
            INSERT INTO "SuKienThanhToan" ("MaPhienThanhToan","MaGiaoDich","MaKenhThanhToan","KhoaSuKienNgoai","LoaiSuKien","NoiDungRutGon","TrangThaiXuLy","NgayXuLy","NgayCapNhat")
            VALUES (:p,:g,'LUMA_COD_MOCK',:key,'THU_COD_NOI_BO',jsonb_build_object('ChungTu',CAST(:proof AS text),'NguoiGhi',:u,'SoTien',:amount),'DA_XU_LY',now(),now())
            """).param("p",session).param("g",receipt).param("key","cod:"+order).param("proof",proof).param("u",actor).param("amount",amount).update();
        jdbc.sql("UPDATE \"DonHang\" SET \"TrangThaiThanhToan\"='DA_THU',\"MaPhienThanhToanChapNhan\"=:p,\"TrangThaiMaGiamGia\"=CASE WHEN \"MaGiamGia\" IS NULL THEN 'KHONG_DUNG' ELSE 'DA_DUNG' END,\"NgayCapNhat\"=now() WHERE \"MaDonHang\"=:d").param("p",session).param("d",order).update();return session;
    }
}
