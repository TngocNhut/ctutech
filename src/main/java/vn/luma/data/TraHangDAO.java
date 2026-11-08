package vn.luma.data;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.luma.business.KhongTimThayException;
import vn.luma.web.KiemHangYeuCau;

@Repository
public class TraHangDAO {
    private final JdbcClient jdbc;
    public TraHangDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    public List<Map<String,Object>> danhSach(){return danhSach(1);}
    public List<Map<String,Object>> danhSach(int page){return jdbc.sql("SELECT f.*,d.\"MaDonCongKhai\" FROM \"PhieuTraHang\" f JOIN \"DonHang\" d USING (\"MaDonHang\") ORDER BY f.\"MaPhieuTraHang\" DESC LIMIT 30 OFFSET :offset").param("offset",(page-1)*30).query().listOfRows();}
    public List<Map<String,Object>> theoDon(long order){return jdbc.sql("SELECT * FROM \"PhieuTraHang\" WHERE \"MaDonHang\"=:d ORDER BY \"MaPhieuTraHang\"").param("d",order).query().listOfRows();}
    public Map<String,Object> doc(long id,boolean lock){return jdbc.sql("SELECT * FROM \"PhieuTraHang\" WHERE \"MaPhieuTraHang\"=:id"+(lock?" FOR UPDATE":"")).param("id",id).query().listOfRows().stream().findFirst().orElseThrow(()->new KhongTimThayException("Không tìm thấy phiếu trả."));}
    public Optional<Map<String,Object>> theoKhoa(long order,String key){return jdbc.sql("SELECT * FROM \"PhieuTraHang\" WHERE \"MaDonHang\"=:d AND \"KhoaChongLap\"=:k").param("d",order).param("k",key).query().listOfRows().stream().findFirst();}
    public boolean dangMo(long order){return jdbc.sql("SELECT EXISTS (SELECT 1 FROM \"PhieuTraHang\" WHERE \"MaDonHang\"=:d AND \"TrangThai\" IN ('CHO_DUYET','CHO_NHAN_HANG','CHO_HOAN_TIEN'))").param("d",order).query(Boolean.class).single();}
    public List<Map<String,Object>> dong(long id){return jdbc.sql("SELECT t.*,d.\"TenSanPhamLucDat\",d.\"MaHangLucDat\",d.\"MaBienThe\",d.\"SoLuong\",d.\"DonGia\",d.\"GiamGiaPhanBo\" FROM \"ChiTietTraHang\" t JOIN \"ChiTietDonHang\" d USING (\"MaDonHang\",\"MaChiTietDonHang\") WHERE t.\"MaPhieuTraHang\"=:id ORDER BY d.\"MaBienThe\"").param("id",id).query().listOfRows();}
    public int daNhan(long line,long except){return jdbc.sql("SELECT coalesce(sum(\"SoLuongDaNhan\"),0) FROM \"ChiTietTraHang\" t JOIN \"PhieuTraHang\" f USING (\"MaPhieuTraHang\") WHERE t.\"MaChiTietDonHang\"=:l AND t.\"MaPhieuTraHang\"<>:id AND f.\"TrangThai\" IN ('CHO_HOAN_TIEN','HOAN_THANH')").param("l",line).param("id",except).query(Integer.class).single();}
    public int daDuyet(long line,long except){return jdbc.sql("SELECT coalesce(sum(\"SoLuongDuyetHoan\"),0) FROM \"ChiTietTraHang\" t JOIN \"PhieuTraHang\" f USING (\"MaPhieuTraHang\") WHERE t.\"MaChiTietDonHang\"=:l AND t.\"MaPhieuTraHang\"<>:id AND f.\"TrangThai\" IN ('CHO_HOAN_TIEN','HOAN_THANH')").param("l",line).param("id",except).query(Integer.class).single();}
    public BigDecimal phiDaDuyet(long order,long except){return jdbc.sql("SELECT coalesce(sum(\"PhiVanChuyenDuocHoan\"),0) FROM \"PhieuTraHang\" WHERE \"MaDonHang\"=:d AND \"MaPhieuTraHang\"<>:id AND \"TrangThai\" NOT IN ('TU_CHOI','DA_HUY')").param("d",order).param("id",except).query(BigDecimal.class).single();}
    public long tao(long order,long actor,String type,String key,String reason){return jdbc.sql("INSERT INTO \"PhieuTraHang\" (\"MaDonHang\",\"LoaiTraHang\",\"MaNguoiYeuCau\",\"KhoaChongLap\",\"LyDo\") VALUES (:d,:type,:u,:k,:reason) RETURNING \"MaPhieuTraHang\"").param("d",order).param("type",type).param("u",actor).param("k",key).param("reason",reason).query(Long.class).single();}
    public void themDong(long order,long ticket,long line,int qty){jdbc.sql("INSERT INTO \"ChiTietTraHang\" (\"MaDonHang\",\"MaPhieuTraHang\",\"MaChiTietDonHang\",\"SoLuongYeuCau\") VALUES (:d,:id,:l,:q)").param("d",order).param("id",ticket).param("l",line).param("q",qty).update();}
    public void duyet(long ticket,long actor,boolean accept,BigDecimal fee,String note){jdbc.sql("UPDATE \"PhieuTraHang\" SET \"TrangThai\"=:s,\"MaNguoiXuLy\"=:u,\"PhiVanChuyenDuocHoan\"=:fee,\"GhiChuXuLy\"=:n,\"NgayDong\"=CASE WHEN :accept THEN NULL ELSE now() END,\"NgayCapNhat\"=now() WHERE \"MaPhieuTraHang\"=:id").param("id",ticket).param("s",accept?"CHO_NHAN_HANG":"TU_CHOI").param("u",actor).param("fee",fee).param("n",note).param("accept",accept).update();}
    public void huy(long ticket){jdbc.sql("UPDATE \"PhieuTraHang\" SET \"TrangThai\"='DA_HUY',\"NgayDong\"=now(),\"NgayCapNhat\"=now() WHERE \"MaPhieuTraHang\"=:id").param("id",ticket).update();}
    public void kiemDong(long id,KiemHangYeuCau.Dong form,BigDecimal amount){jdbc.sql("UPDATE \"ChiTietTraHang\" SET \"SoLuongDaNhan\"=:received,\"SoLuongDuyetHoan\"=:approved,\"SoLuongNhapLai\"=:stock,\"SoTienDuocHoan\"=:money,\"GhiChuKiemHang\"=:note WHERE \"MaChiTietTraHang\"=:id").param("id",id).param("received",form.daNhan()).param("approved",form.duyetHoan()).param("stock",form.nhapLai()).param("money",amount).param("note",form.ghiChu().strip()).update();}
    public void nhapLai(long order,long line,long sku,int qty,long actor){
        jdbc.sql("UPDATE \"TonKho\" SET \"SoLuongTon\"=\"SoLuongTon\"+:q,\"NgayCapNhat\"=now() WHERE \"MaKhoHang\"=1 AND \"MaBienThe\"=:s").param("q",qty).param("s",sku).update();
        jdbc.sql("INSERT INTO \"BienDongKho\" (\"MaKhoHang\",\"MaBienThe\",\"MaDonHang\",\"MaChiTietTraHang\",\"MaNguoiThucHien\",\"LoaiBienDong\",\"ThayDoiTon\",\"KhoaChongLap\") VALUES (1,:s,:d,:l,:u,'NHAP_HANG_TRA',:q,:key)").param("s",sku).param("d",order).param("l",line).param("u",actor).param("q",qty).param("key","return:"+line).update();
    }
    public void nhanHang(long id,boolean waiting){jdbc.sql("UPDATE \"PhieuTraHang\" SET \"TrangThai\"=:s,\"NgayNhanHang\"=now(),\"NgayDong\"=CASE WHEN :waiting THEN NULL ELSE now() END,\"NgayCapNhat\"=now() WHERE \"MaPhieuTraHang\"=:id").param("id",id).param("waiting",waiting).param("s",waiting?"CHO_HOAN_TIEN":"HOAN_THANH").update();}
    public void taoHoan(long receipt,long ticket,BigDecimal amount){jdbc.sql("""
        INSERT INTO "HoanTien" ("MaGiaoDich","MaPhieuTraHang","KhoaChongLap","SoTienHoan","MaTienTe","PhuongThucHoan","LyDo","TrangThai","NgayCapNhat","LoaiHoanTien")
        VALUES (:g,:id,:key,:amount,'VND','CHUYEN_KHOAN_THU_CONG','Hoàn theo kết quả kiểm hàng','CHO_XU_LY',now(),'TRA_HANG')
        """).param("g",receipt).param("id",ticket).param("key","return:"+ticket+":"+receipt).param("amount",amount).update();}
    public void taoViec(long ticket,String type,String note){jdbc.sql("INSERT INTO \"ViecXuLyGiaoNhan\" (\"MaPhieuTraHang\",\"LoaiViec\",\"HanXuLy\",\"GhiChu\") VALUES (:id,:type,now()+interval '24 hours',:note)").param("id",ticket).param("type",type).param("note",note).update();}
    public void dongViec(long ticket){jdbc.sql("UPDATE \"ViecXuLyGiaoNhan\" SET \"TrangThai\"='DA_XONG',\"NgayDong\"=now(),\"NgayCapNhat\"=now() WHERE \"MaPhieuTraHang\"=:id AND \"TrangThai\" IN ('MOI','DANG_XU_LY')").param("id",ticket).update();}
    public long taoVanDonHoan(long order,long ticket){
        long carrier=jdbc.sql("SELECT \"MaDonViVanChuyen\" FROM \"DonViVanChuyen\" WHERE \"KyHieu\"='LUMA_NOI_BO' AND \"DangHoatDong\"").query(Long.class).single();
        long waybill=jdbc.sql("INSERT INTO \"VanDon\" (\"MaDonHang\",\"MaPhieuTraHang\",\"MaDonViVanChuyen\",\"LoaiVanDon\",\"MaTheoDoi\") VALUES (:d,:id,:c,'HOAN_VE',:tracking) RETURNING \"MaVanDon\"").param("d",order).param("id",ticket).param("c",carrier).param("tracking","RET-"+ticket+"-"+order).query(Long.class).single();
        jdbc.sql("INSERT INTO \"ChiTietVanDon\" (\"MaVanDon\",\"MaDonHang\",\"MaPhieuTraHang\",\"LoaiVanDon\",\"MaChiTietDonHang\",\"SoLuongDuKien\") SELECT :v,\"MaDonHang\",\"MaPhieuTraHang\",'HOAN_VE',\"MaChiTietDonHang\",\"SoLuongYeuCau\" FROM \"ChiTietTraHang\" WHERE \"MaPhieuTraHang\"=:id").param("v",waybill).param("id",ticket).update();return waybill;
    }
    public Optional<Map<String,Object>> vanDon(long ticket){return jdbc.sql("SELECT * FROM \"VanDon\" WHERE \"MaPhieuTraHang\"=:id").param("id",ticket).query().listOfRows().stream().findFirst();}
    public void huyDonSauNhan(long order){jdbc.sql("UPDATE \"DonHang\" SET \"TrangThaiDon\"='DA_HUY',\"LyDoHuy\"='Kiện giao thất bại đã nhận và kiểm hàng hoàn',\"NgayHuy\"=now(),\"NgayCapNhat\"=now() WHERE \"MaDonHang\"=:d").param("d",order).update();}
    public void log(long actor,long ticket,String action){jdbc.sql("INSERT INTO \"NhatKyQuanTri\" (\"MaNguoiThucHien\",\"HanhDong\",\"LoaiDoiTuong\",\"MaDoiTuong\") VALUES (:u,:action,'PHIEU_TRA_HANG',:id)").param("u",actor).param("action",action).param("id",Long.toString(ticket)).update();}
}
