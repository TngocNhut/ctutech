package vn.luma.data;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.luma.model.DiaChiChot;

@Repository
public class DonHangDAO {
    private final JdbcClient jdbc;
    public DonHangDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    public Optional<Map<String,Object>> theoKhoa(long user,String key){return jdbc.sql("SELECT * FROM \"DonHang\" WHERE \"MaNguoiDung\"=:u AND \"KhoaChongLap\"=:k").param("u",user).param("k",key).query().listOfRows().stream().findFirst();}
    public Map<String,Object> theoMa(String code,boolean lock){return jdbc.sql("SELECT * FROM \"DonHang\" WHERE \"MaDonCongKhai\"=:c"+(lock?" FOR UPDATE":"")).param("c",code).query().listOfRows().stream().findFirst().orElseThrow(()->new vn.luma.business.KhongTimThayException("Không tìm thấy đơn hàng."));}
    public List<Map<String,Object>> danhSach(long user){return danhSach(user,1);}
    public List<Map<String,Object>> danhSach(long user,int page){return jdbc.sql("SELECT * FROM \"DonHang\" WHERE \"MaNguoiDung\"=:u ORDER BY \"MaDonHang\" DESC LIMIT 30 OFFSET :offset").param("u",user).param("offset",(page-1)*30).query().listOfRows();}
    public List<Map<String,Object>> dongDanhSach(long user,List<Long> orders){
        if(orders.isEmpty())return List.of();
        return jdbc.sql("""
            SELECT c."MaDonHang",c."MaBienThe",c."TenSanPhamLucDat",c."SoLuong",c."CauHinhLucDat"->>'MauSac' AS mau,
              (SELECT a."DuongDanTep" FROM "TepSanPham" a JOIN "BienTheSanPham" b ON b."MaBienThe"=a."MaBienThe" AND b."MaSanPham"=a."MaSanPham"
               WHERE a."MaBienThe"=c."MaBienThe" AND a."LoaiTep"='ANH' AND a."DangHienThi" ORDER BY a."ThuTu",a."MaTep" LIMIT 1) AS anh
            FROM "ChiTietDonHang" c JOIN "DonHang" d USING ("MaDonHang")
            WHERE d."MaNguoiDung"=:user AND c."MaDonHang" IN (:orders) ORDER BY c."MaDonHang",c."MaChiTietDonHang"
            """).param("user",user).param("orders",orders).query().listOfRows();
    }
    public List<Map<String,Object>> dong(long order){return jdbc.sql("""
        SELECT c.*,c."CauHinhLucDat"->>'CPU' AS "CpuChot",c."CauHinhLucDat"->>'RAM' AS "RamChot",
          c."CauHinhLucDat"->>'LuuTru' AS "LuuTruChot",c."CauHinhLucDat"->>'MauSac' AS "MauChot",
          a."TenChuongTrinhLucDat",a."GiaTruocKhuyenMai",a."GiaSauKhuyenMai",
          (SELECT string_agg((x.e->>'Ten')||': '||(x.e->>'GiaTri')||coalesce(' '||nullif(x.e->>'DonVi',''),''),' · ' ORDER BY x.ord)
           FROM jsonb_array_elements(coalesce(c."CauHinhLucDat"->'ThongSoMoRong','[]'::jsonb)) WITH ORDINALITY x(e,ord)) AS "ThongSoMoRongChot"
        FROM "ChiTietDonHang" c LEFT JOIN "ApDungKhuyenMai" a USING ("MaChiTietDonHang")
        WHERE c."MaDonHang"=:d ORDER BY c."MaBienThe"
        """).param("d",order).query().listOfRows();}
    public List<Map<String,Object>> lichSu(long order){return jdbc.sql("SELECT *,to_char(\"NgayTao\" AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY HH24:MI') AS \"ThoiDiem\" FROM \"LichSuDonHang\" WHERE \"MaDonHang\"=:d ORDER BY \"MaLichSu\"").param("d",order).query().listOfRows();}
    public Map<String,Object> khoaSku(long sku){return jdbc.sql("""
        SELECT b.*,s."TenSanPham",p."TenPhanLoai",t."SoLuongTon",t."SoLuongGiu",
          (s."DangHienThi" AND p."DangHienThi" AND h."DangHienThi" AND b."DangKinhDoanh" AND k."DangHoatDong") AS cho_mua
        FROM "BienTheSanPham" b JOIN "SanPham" s USING ("MaSanPham") JOIN "PhanLoai" p USING ("MaPhanLoai")
        JOIN "ThuongHieu" h USING ("MaThuongHieu") JOIN "TonKho" t ON t."MaBienThe"=b."MaBienThe" AND t."MaKhoHang"=1
        JOIN "KhoHang" k ON k."MaKhoHang"=t."MaKhoHang"
        WHERE b."MaBienThe"=:id FOR UPDATE OF b,t
        """).param("id",sku).query().listOfRows().stream().findFirst().orElseThrow(()->new IllegalArgumentException("Một cấu hình không còn khả dụng."));}
    public Map<String,Object> khoaVoucher(String code){return jdbc.sql("SELECT *, (\"NgayBatDau\"<=clock_timestamp() AND \"NgayKetThuc\">clock_timestamp()) AS con_han FROM \"MaGiamGia\" WHERE \"KyHieu\"=:c FOR UPDATE").param("c",code).query().listOfRows().stream().findFirst().orElseThrow(()->new IllegalArgumentException("Không tìm thấy mã giảm giá."));}
    public void khoaVoucherId(long id){jdbc.sql("SELECT \"MaGiamGia\" FROM \"MaGiamGia\" WHERE \"MaGiamGia\"=:id FOR UPDATE").param("id",id).query(Long.class).single();}
    public boolean voucherConHan(long id){return jdbc.sql("SELECT \"NgayBatDau\"<=clock_timestamp() AND \"NgayKetThuc\">clock_timestamp() FROM \"MaGiamGia\" WHERE \"MaGiamGia\"=:id").param("id",id).query(Boolean.class).single();}
    public List<String> donQuaHan(){return jdbc.sql("SELECT \"MaDonCongKhai\" FROM \"DonHang\" WHERE \"PhuongThucThanhToan\"='COD' AND \"TrangThaiDon\"='CHO_XAC_NHAN' AND \"HanXuLy\"<clock_timestamp() LIMIT 100").query(String.class).list();}
    public boolean quaHan(long id){return jdbc.sql("SELECT \"HanXuLy\"<clock_timestamp() FROM \"DonHang\" WHERE \"MaDonHang\"=:id").param("id",id).query(Boolean.class).single();}
    public long luotVoucher(long voucher,Long user){return jdbc.sql("SELECT count(*) FROM \"DonHang\" WHERE \"MaGiamGia\"=:v AND \"TrangThaiMaGiamGia\" IN ('DANG_GIU','DA_DUNG') AND (:u IS NULL OR \"MaNguoiDung\"=:u)")
        .param("v",voucher).param("u",user,java.sql.Types.BIGINT).query(Long.class).single();}
    public long tao(long user,String email,String code,String key,String hash,DiaChiChot d,BigDecimal subtotal,BigDecimal discount,BigDecimal fee,Map<String,Object> voucher,String method){
        return jdbc.sql("""
            INSERT INTO "DonHang" ("MaKhoHang","MaDonCongKhai","MaNguoiDung","KhoaChongLap","DauVanTayDatHang","MaGiamGia","KyHieuGiamGiaLucDat","TrangThaiMaGiamGia",
              "TenNguoiNhan","SoDienThoaiNhan","ThuDienTuNhan","TinhThanhNhan","PhuongXaNhan","DiaChiNhanCuThe","MaQuocGiaNhan","GhiChuKhach",
              "MaDiaDiemGoogleNhan","ViDoNhan","KinhDoNhan","NguonToaDoNhan","NgayLayToaDoNhan","HanLuuToaDoNhan","NgayXacNhanViTriNhan",
              "TongTienHang","TienGiamGia","PhiVanChuyen","TongThanhToan","PhuongThucThanhToan","TrangThaiDon","TrangThaiThanhToan","TrangThaiKho","HanXuLy","NgayCapNhat")
            VALUES (1,:code,:u,:key,:hash,:v,:vc,:vs,:ten,:phone,:email,:tinh,:xa,:street,'VN',:note,:place,:lat,:lng,:source,:ngay,:han,:ngay,
              :sub,:discount,:fee,:total,:method,'CHO_XAC_NHAN','CHUA_THU','DANG_GIU',now()+CASE WHEN :method='PAYOS' THEN interval '15 minutes' ELSE interval '24 hours' END,now()) RETURNING "MaDonHang"
            """).param("code",code).param("u",user).param("key",key).param("hash",hash).param("v",voucher==null?null:voucher.get("MaGiamGia"),java.sql.Types.BIGINT)
            .param("vc",voucher==null?null:voucher.get("KyHieu")).param("vs",voucher==null?"KHONG_DUNG":"DANG_GIU")
            .param("ten",d.tenNguoiNhan()).param("phone",d.soDienThoai()).param("email",email,java.sql.Types.VARCHAR).param("tinh",d.tinhThanh()).param("xa",d.phuongXa()).param("street",d.diaChiCuThe()).param("note",d.ghiChu())
            .param("place",d.placeId()).param("lat",d.viDo()).param("lng",d.kinhDo()).param("source",d.viDo()==null?null:"GOOGLE_GEOCODING")
            .param("ngay",d.ngayLay(),java.sql.Types.TIMESTAMP_WITH_TIMEZONE).param("han",d.hanLuu(),java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
            .param("method",method).param("sub",subtotal).param("discount",discount).param("fee",fee).param("total",subtotal.subtract(discount).add(fee)).query(Long.class).single();
    }
    public void themDong(long order,long sku,int quantity,BigDecimal price,BigDecimal discount){
        jdbc.sql("""
            INSERT INTO "ChiTietDonHang" ("MaDonHang","MaBienThe","TenSanPhamLucDat","TenPhanLoaiLucDat","MaHangLucDat","CauHinhLucDat","SoLuong","DonGia","GiaVonLucDat","GiamGiaPhanBo")
            SELECT :d,b."MaBienThe",s."TenSanPham",p."TenPhanLoai",b."MaHang",
              jsonb_build_object('MauSac',b."MauSac",'CPU',b."BoXuLy",'RAM',b."DungLuongRam",'LuuTru',b."DungLuongLuuTru",'ManHinh',b."KichThuocManHinh",'GPU',b."BoXuLyDoHoa",'HeDieuHanh',b."HeDieuHanh",'ThongSoMoRong',coalesce((
                SELECT jsonb_agg(jsonb_build_object('Ten',a."TenThuocTinh",'Kieu',a."KieuDuLieu",'DonVi',a."DonVi",'GiaTri',
                  CASE a."KieuDuLieu" WHEN 'CHU' THEN v."GiaTriChu" WHEN 'SO' THEN rtrim(rtrim(v."GiaTriSo"::text,'0'),'.') ELSE CASE WHEN v."GiaTriCoKhong" THEN 'Có' ELSE 'Không' END END) ORDER BY a."ThuTu",a."MaThuocTinh")
                FROM "GiaTriThuocTinhBienThe" v JOIN "ThuocTinhDanhMuc" a USING ("MaThuocTinh") WHERE v."MaBienThe"=b."MaBienThe"
              ),'[]'::jsonb)),
              :q,:price,b."GiaVon",:discount FROM "BienTheSanPham" b JOIN "SanPham" s USING ("MaSanPham") JOIN "PhanLoai" p USING ("MaPhanLoai") WHERE b."MaBienThe"=:s
            """).param("d",order).param("s",sku).param("q",quantity).param("price",price).param("discount",discount).update();
        if(jdbc.sql("UPDATE \"TonKho\" SET \"SoLuongGiu\"=\"SoLuongGiu\"+:q,\"NgayCapNhat\"=now() WHERE \"MaKhoHang\"=1 AND \"MaBienThe\"=:s AND \"SoLuongTon\"-\"SoLuongGiu\">=:q")
            .param("q",quantity).param("s",sku).update()!=1)throw new IllegalArgumentException("Sản phẩm vừa hết hàng. Hãy kiểm tra lại giỏ.");
        bienDong(order,sku,quantity,"GIU_HANG","hold:"+order);
    }
    public void bienDong(long order,long sku,int delta,String type,String key){jdbc.sql("INSERT INTO \"BienDongKho\" (\"MaKhoHang\",\"MaBienThe\",\"MaDonHang\",\"LoaiBienDong\",\"ThayDoiTon\",\"ThayDoiGiu\",\"KhoaChongLap\") VALUES (1,:s,:d,:type,0,:delta,:key)")
        .param("s",sku).param("d",order).param("type",type).param("delta",delta).param("key",key).update();}
    public long lichSu(long order,long user,String old,String state,String note){return jdbc.sql("INSERT INTO \"LichSuDonHang\" (\"MaDonHang\",\"MaNguoiThucHien\",\"TrangThaiCu\",\"TrangThaiMoi\",\"GhiChu\") VALUES (:d,:u,:old,:state,:note) RETURNING \"MaLichSu\"")
        .param("d",order).param("u",user).param("old",old).param("state",state).param("note",note).query(Long.class).single();}
    public Map<String,Object> nguoiNhanThongBao(long order){return jdbc.sql("SELECT d.\"MaNguoiDung\",d.\"MaDonCongKhai\",EXISTS(SELECT 1 FROM \"PhienThanhToan\" p WHERE p.\"MaDonHang\"=d.\"MaDonHang\" AND p.\"CheDoXuLy\"='MO_PHONG') AS mo_phong FROM \"DonHang\" d WHERE d.\"MaDonHang\"=:id").param("id",order).query().singleRow();}
    public void nhaGiu(long order,long sku,int quantity){
        if(jdbc.sql("UPDATE \"TonKho\" SET \"SoLuongGiu\"=\"SoLuongGiu\"-:q,\"NgayCapNhat\"=now() WHERE \"MaKhoHang\"=1 AND \"MaBienThe\"=:s AND \"SoLuongGiu\">=:q").param("q",quantity).param("s",sku).update()!=1)throw new IllegalStateException("Sổ giữ hàng không khớp.");
        bienDong(order,sku,-quantity,"TRA_GIU","release:"+order);
    }
    public void huy(long order,String state,String reason){jdbc.sql("UPDATE \"DonHang\" SET \"TrangThaiDon\"=:state,\"TrangThaiKho\"='DA_TRA_GIU',\"TrangThaiMaGiamGia\"=CASE WHEN \"MaGiamGia\" IS NULL THEN 'KHONG_DUNG' ELSE 'DA_TRA_LUOT' END,\"LyDoHuy\"=:reason,\"NgayHuy\"=now(),\"NgayCapNhat\"=now() WHERE \"MaDonHang\"=:d")
        .param("d",order).param("state",state).param("reason",reason).update();}
    public BigDecimal phiGiao(){return jdbc.sql("SELECT coalesce((SELECT (\"GiaTri\" #>> '{}')::numeric FROM \"CauHinhHeThong\" WHERE \"KyHieu\"='PHI_GIAO_HANG_MAC_DINH'),30000)").query(BigDecimal.class).single();}
}
