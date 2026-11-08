package vn.luma.data;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.luma.business.TaiKhoanService;
import vn.luma.model.BaoThu;

@Repository
public class ThanhToanDAO {
    private final JdbcClient jdbc;
    public ThanhToanDAO(JdbcClient jdbc){this.jdbc=jdbc;}
    public static long id(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
    public static Instant instant(Object value){return value instanceof OffsetDateTime t?t.toInstant():((java.sql.Timestamp)value).toInstant();}
    public void taoPhien(long order,String channel,String mode){
        jdbc.sql("""
            INSERT INTO "PhienThanhToan" ("MaDonHang","NhaCungCap","CheDoXuLy","MaKenhThanhToan","KhoaChongLap","MaDonPayOS","SoTienYeuCau","MaTienTe","TrangThaiXuLy","NgayHetHan","NgayCapNhat")
            SELECT "MaDonHang",'PAYOS',:mode,:channel,:key,nextval('"MaDonPayOSSeq"'),"TongThanhToan",'VND','MOI_TAO',"HanXuLy",now()
            FROM "DonHang" WHERE "MaDonHang"=:d
            """).param("d",order).param("mode",mode).param("channel",channel).param("key","payos:"+order).update();
    }
    public Optional<Map<String,Object>> phienDon(long order,boolean lock){return jdbc.sql("SELECT * FROM \"PhienThanhToan\" WHERE \"MaDonHang\"=:d AND \"NhaCungCap\"='PAYOS' ORDER BY \"MaPhienThanhToan\" DESC LIMIT 1"+(lock?" FOR UPDATE":"")).param("d",order).query().listOfRows().stream().findFirst();}
    public Optional<Map<String,Object>> phienMa(String channel,long code){return jdbc.sql("SELECT * FROM \"PhienThanhToan\" WHERE \"MaKenhThanhToan\"=:c AND \"MaDonPayOS\"=:code").param("c",channel).param("code",code).query().listOfRows().stream().findFirst();}
    public Map<String,Object> khoaPhien(long id){return jdbc.sql("SELECT * FROM \"PhienThanhToan\" WHERE \"MaPhienThanhToan\"=:p FOR UPDATE").param("p",id).query().singleRow();}
    public Map<String,Object> phien(long id){return jdbc.sql("SELECT * FROM \"PhienThanhToan\" WHERE \"MaPhienThanhToan\"=:p").param("p",id).query().singleRow();}
    public String maDon(long order){return jdbc.sql("SELECT \"MaDonCongKhai\" FROM \"DonHang\" WHERE \"MaDonHang\"=:d").param("d",order).query(String.class).single();}
    public boolean nhanTao(long id){return jdbc.sql("UPDATE \"PhienThanhToan\" SET \"MaLoi\"='DANG_TAO_LINK',\"NgayThuTaoTiepTheo\"=clock_timestamp()+interval '30 seconds',\"NgayCapNhat\"=now() WHERE \"MaPhienThanhToan\"=:p AND \"TrangThaiXuLy\"='MOI_TAO' AND \"MaLoi\" IS NULL").param("p",id).update()==1;}
    public boolean nhanTaoLai(long id){return jdbc.sql("UPDATE \"PhienThanhToan\" SET \"MaLoi\"='DANG_TAO_LINK',\"NgayThuTaoTiepTheo\"=clock_timestamp()+interval '30 seconds',\"NgayCapNhat\"=now() WHERE \"MaPhienThanhToan\"=:p AND coalesce(\"NgayThuTaoTiepTheo\",'-infinity'::timestamptz)<=clock_timestamp()").param("p",id).update()==1;}
    public boolean nhanTraCuu(long id){return jdbc.sql("UPDATE \"PhienThanhToan\" SET \"NgayTraCuuTiepTheo\"=clock_timestamp()+interval '5 seconds' WHERE \"MaPhienThanhToan\"=:p AND coalesce(\"NgayTraCuuTiepTheo\",'-infinity'::timestamptz)<=clock_timestamp()").param("p",id).update()==1;}
    public void ghiLink(long id,String link,String url,String providerState){jdbc.sql("UPDATE \"PhienThanhToan\" SET \"MaLienKetPayOS\"=:link,\"DuongDanThanhToan\"=:url,\"TrangThaiCong\"=:state,\"TrangThaiXuLy\"=CASE WHEN \"TrangThaiXuLy\"='MOI_TAO' THEN 'CHO_THANH_TOAN' ELSE \"TrangThaiXuLy\" END,\"MaLoi\"=CASE WHEN \"MaLoi\" IN ('DANG_TAO_LINK','TAO_LINK_CHUA_RO','TRA_CUU_CHUA_RO') OR (\"MaLoi\"='CHO_HUY_LINK' AND :state='CANCELLED') THEN NULL ELSE \"MaLoi\" END,\"NgayCapNhat\"=now() WHERE \"MaPhienThanhToan\"=:p")
        .param("p",id).param("link",link).param("url",url).param("state",providerState).update();}
    public void trangThaiPhien(long id,String state,String reason){jdbc.sql("UPDATE \"PhienThanhToan\" SET \"TrangThaiXuLy\"=:state,\"MaLoi\"=:reason,\"NgayCapNhat\"=now() WHERE \"MaPhienThanhToan\"=:p").param("p",id).param("state",state).param("reason",reason).update();}
    public void trangThaiDon(long id,String state){jdbc.sql("UPDATE \"DonHang\" SET \"TrangThaiThanhToan\"=:s,\"NgayCapNhat\"=now() WHERE \"MaDonHang\"=:d").param("d",id).param("s",state).update();}
    public long luuSuKien(String channel,BaoThu thu){
        String key=TaiKhoanService.bam(thu.orderCode()+"|"+thu.linkId()+"|"+thu.reference()+"|"+thu.amount().toPlainString()+"|"+thu.time().toInstant());
        return jdbc.sql("""
            INSERT INTO "SuKienThanhToan" ("MaKenhThanhToan","KhoaSuKienNgoai","LoaiSuKien","NoiDungRutGon","TrangThaiXuLy","NgayCapNhat")
            VALUES (:channel,:key,'NHAN_CHUYEN_KHOAN',jsonb_build_object('orderCode',:code,'link',:link,'reference',:ref,'amount',:amount,'currency','VND','time',CAST(:time AS text)),'CHO_XU_LY',now())
            ON CONFLICT ("MaKenhThanhToan","KhoaSuKienNgoai") DO UPDATE SET "NgayCapNhat"="SuKienThanhToan"."NgayCapNhat" RETURNING "MaSuKien"
            """).param("channel",channel).param("key",key).param("code",thu.orderCode()).param("link",thu.linkId()).param("ref",thu.reference()).param("amount",thu.amount()).param("time",thu.time().toString()).query(Long.class).single();
    }
    public Map<String,Object> suKien(long id,boolean lock){return jdbc.sql("SELECT *,\"NoiDungRutGon\"->>'orderCode' AS code,\"NoiDungRutGon\"->>'link' AS link,\"NoiDungRutGon\"->>'reference' AS ref,\"NoiDungRutGon\"->>'amount' AS amount,\"NoiDungRutGon\"->>'time' AS time FROM \"SuKienThanhToan\" WHERE \"MaSuKien\"=:id"+(lock?" FOR UPDATE":"")).param("id",id).query().singleRow();}
    public void xongSuKien(long event,long session,Long receipt,String state){jdbc.sql("UPDATE \"SuKienThanhToan\" SET \"MaPhienThanhToan\"=:p,\"MaGiaoDich\"=:g,\"TrangThaiXuLy\"=:s,\"NgayXuLy\"=now(),\"NgayCapNhat\"=now() WHERE \"MaSuKien\"=:e").param("e",event).param("p",session).param("g",receipt,java.sql.Types.BIGINT).param("s",state).update();}
    public Map<String,Object> ghiThu(Map<String,Object> session,BaoThu thu){
        long id=id(session,"MaPhienThanhToan");String channel=(String)session.get("MaKenhThanhToan");
        jdbc.sql("""
            INSERT INTO "GiaoDichThanhToan" ("MaPhienThanhToan","NhaCungCap","MaKenhThanhToan","MaThamChieuNgoai","KhoaChongLap","SoTien","MaTienTe","NgayGiaoDich","NgayXacMinh")
            VALUES (:p,'PAYOS',:c,:ref,:key,:amount,'VND',:time,now()) ON CONFLICT ("MaKenhThanhToan","MaThamChieuNgoai") DO NOTHING
            """).param("p",id).param("c",channel).param("ref",thu.reference()).param("key",TaiKhoanService.bam(channel+":"+thu.reference())).param("amount",thu.amount()).param("time",thu.time()).update();
        return jdbc.sql("SELECT * FROM \"GiaoDichThanhToan\" WHERE \"MaKenhThanhToan\"=:c AND \"MaThamChieuNgoai\"=:r FOR UPDATE").param("c",channel).param("r",thu.reference()).query().singleRow();
    }
    public BigDecimal tongThu(long id){return jdbc.sql("SELECT coalesce(sum(\"SoTien\"),0) FROM \"GiaoDichThanhToan\" WHERE \"MaPhienThanhToan\"=:p").param("p",id).query(BigDecimal.class).single();}
    public BigDecimal tongDaHoan(long id){return jdbc.sql("SELECT coalesce(sum(h.\"SoTienHoan\"),0) FROM \"HoanTien\" h JOIN \"GiaoDichThanhToan\" g USING (\"MaGiaoDich\") WHERE g.\"MaPhienThanhToan\"=:p AND h.\"TrangThai\"='THANH_CONG'").param("p",id).query(BigDecimal.class).single();}
    public List<Map<String,Object>> khoanThu(long session){return jdbc.sql("SELECT * FROM \"GiaoDichThanhToan\" WHERE \"MaPhienThanhToan\"=:p ORDER BY \"MaGiaoDich\" DESC FOR UPDATE").param("p",session).query().listOfRows();}
    public BigDecimal daGiuHoan(long receipt){return jdbc.sql("SELECT coalesce(sum(\"SoTienHoan\"),0) FROM \"HoanTien\" WHERE \"MaGiaoDich\"=:g").param("g",receipt).query(BigDecimal.class).single();}
    public void yeuCauHoan(long receipt,BigDecimal amount,BigDecimal previouslyClaimed,String type){
        jdbc.sql("""
            INSERT INTO "HoanTien" ("MaGiaoDich","KhoaChongLap","SoTienHoan","MaTienTe","PhuongThucHoan","LyDo","TrangThai","NgayCapNhat","LoaiHoanTien")
            VALUES (:g,:key,:amount,'VND','CHUYEN_KHOAN_THU_CONG',:reason,'CHO_XU_LY',now(),:type) ON CONFLICT ("KhoaChongLap") DO NOTHING
            """).param("g",receipt).param("key","auto:"+type+":"+receipt+":"+previouslyClaimed.toPlainString()).param("amount",amount).param("reason",type.equals("THU_DU")?"Khoản tiền vượt tổng đơn cần hoàn và đối soát":"Đơn đã hủy hoặc hết hạn, khoản thu cần hoàn").param("type",type).update();
    }
    public void chapNhan(long order,long session){
        jdbc.sql("UPDATE \"PhienThanhToan\" SET \"TrangThaiXuLy\"='DU_TIEN',\"NgayDuTien\"=coalesce(\"NgayDuTien\",now()),\"MaLoi\"=NULL,\"NgayCapNhat\"=now() WHERE \"MaPhienThanhToan\"=:p").param("p",session).update();
        jdbc.sql("UPDATE \"DonHang\" SET \"MaPhienThanhToanChapNhan\"=:p,\"TrangThaiThanhToan\"='DA_THU',\"TrangThaiMaGiamGia\"=CASE WHEN \"MaGiamGia\" IS NULL THEN 'KHONG_DUNG' ELSE 'DA_DUNG' END,\"NgayCapNhat\"=now() WHERE \"MaDonHang\"=:d").param("d",order).param("p",session).update();
    }
    public void doiSoat(long session,String source,BigDecimal provider,BigDecimal ledger,String result,String reason){jdbc.sql("INSERT INTO \"LanDoiSoatThanhToan\" (\"MaPhienThanhToan\",\"NguonKiemTra\",\"TongThuTuNguon\",\"TongThuTrongSo\",\"KetQua\",\"MaLyDo\") VALUES (:p,:source,:provider,:ledger,:result,:reason)")
        .param("p",session).param("source",source).param("provider",provider).param("ledger",ledger).param("result",result).param("reason",reason).update();}
    public List<Long> choXuLy(){return jdbc.sql("SELECT \"MaSuKien\" FROM \"SuKienThanhToan\" WHERE \"TrangThaiXuLy\"='CHO_XU_LY' ORDER BY \"MaSuKien\" LIMIT 100").query(Long.class).list();}
    public List<Long> canDoiSoat(){return jdbc.sql("""
        SELECT p."MaPhienThanhToan" FROM "PhienThanhToan" p JOIN "DonHang" d USING ("MaDonHang")
        WHERE p."NhaCungCap"='PAYOS' AND (
          (p."CheDoXuLy"='TICH_HOP_THAT' AND (p."TrangThaiXuLy" IN ('MOI_TAO','CHO_THANH_TOAN','THU_THIEU','THU_DU','CAN_DOI_SOAT')
            OR (p."TrangThaiXuLy" IN ('HET_HAN','DA_HUY') AND p."NgayCapNhat">now()-interval '7 days')))
          OR (p."CheDoXuLy"='MO_PHONG' AND d."MaPhienThanhToanChapNhan" IS NULL AND p."NgayHetHan"<clock_timestamp()
            AND p."TrangThaiXuLy" IN ('MOI_TAO','CHO_THANH_TOAN','THU_THIEU','THU_DU','CAN_DOI_SOAT')))
        ORDER BY CASE WHEN p."TrangThaiXuLy" IN ('HET_HAN','DA_HUY') THEN 1 ELSE 0 END,p."NgayCapNhat" LIMIT 30
        """).query(Long.class).list();}
}
