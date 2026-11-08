package vn.luma;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.business.*;
import vn.luma.web.*;
import static vn.luma.data.ThanhToanDAO.id;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.mail.local-directory=.local/test-mail","app.refunds.proof-directory=.local/test-refund-proofs","app.scheduling.enabled=false"})
@Transactional
class BaoCaoSoLieuIntegrationTest {
    static final String ADMIN="admin@luma.local";
    @Autowired BaoCaoService reports;
    @Autowired TaiKhoanService users;
    @Autowired GioHangService carts;
    @Autowired DonHangService orders;
    @Autowired GiaoNhanService shipping;
    @Autowired ThanhToanService payments;
    @Autowired TraHangService returns;
    @Autowired HoanTienService refunds;
    @Autowired HoaDonService invoices;
    @Autowired DanhGiaService reviews;
    @Autowired CauHinhHeThongService settings;
    @Autowired BanDoService maps;
    @Autowired JdbcClient jdbc;
    record Order(String email,String code,long id,long line) { }
    String user(){String e="report-data-"+UUID.randomUUID()+"@example.test";users.seedDemo(e,"Khách báo cáo","KhachHang","ReportsDemo!2026");return e;}
    Order create(String email,int qty,String method){carts.sua(email,1,qty,true);var a=new DiaChiYeuCau("Khách báo cáo","0901234567","TP.HCM","Phường thử","123 Đường thử","","",null,null,false);String code=orders.datHang(email,new DatHangYeuCau(UUID.randomUUID().toString(),orders.noiDungGio(email),"",a,method),maps.chuanHoa(a));return new Order(email,code,id(orders.chiTiet(email,code),"MaDonHang"),id(orders.dong(email,code).getFirst(),"MaChiTietDonHang"));}
    Order paid(String email,int qty){var o=create(email,qty,"COD");shipping.xacNhan(ADMIN,o.code(),true);long carrier=jdbc.sql("SELECT \"MaDonViVanChuyen\" FROM \"DonViVanChuyen\" WHERE \"KyHieu\"='LUMA_NOI_BO'").query(Long.class).single();shipping.taoVanDon(ADMIN,o.code(),new TaoVanDonYeuCau(carrier,"REPORT-"+o.code(),null,null));shipping.capNhat(ADMIN,o.code(),event("BAN_GIAO"));shipping.capNhat(ADMIN,o.code(),event("DA_GIAO"));shipping.thuCOD(ADMIN,o.code(),new ThuCODYeuCau((BigDecimal)orders.chiTiet(email,o.code()).get("TongThanhToan"),"REPORT-"+o.code(),true));return o;}
    SuKienGiaoNhanYeuCau event(String action){return new SuKienGiaoNhanYeuCau(action,UUID.randomUUID().toString(),"Sự kiện fixture báo cáo");}
    void date(Order o,String date){var timestamp=OffsetDateTime.parse(date);jdbc.sql("UPDATE \"DonHang\" SET \"NgayHoanThanh\"=:date WHERE \"MaDonHang\"=:id").param("date",timestamp).param("id",o.id()).update();jdbc.sql("UPDATE \"GiaoDichThanhToan\" g SET \"NgayGiaoDich\"=:date FROM \"PhienThanhToan\" p WHERE g.\"MaPhienThanhToan\"=p.\"MaPhienThanhToan\" AND p.\"MaDonHang\"=:id").param("date",timestamp).param("id",o.id()).update();}
    long refund(Order o,boolean success){long ticket=returns.tao(o.email(),o.code(),new TaoTraHangYeuCau(UUID.randomUUID().toString(),"Trả hàng fixture báo cáo",List.of(new TaoTraHangYeuCau.Dong(o.line(),1))),false);returns.duyet(ADMIN,ticket,true,BigDecimal.ZERO,"Duyệt kiểm tra fixture");long line=id(returns.dong(ADMIN,ticket).getFirst(),"MaChiTietTraHang");returns.kiemHang(ADMIN,ticket,new KiemHangYeuCau(true,List.of(new KiemHangYeuCau.Dong(line,1,1,1,"Đã nhận đủ và kiểm hàng fixture"))));long refund=id(refunds.theoDon(o.email(),o.code()).stream().filter(r->r.get("MaPhieuTraHang")!=null&&id(r,"MaPhieuTraHang")==ticket).findFirst().orElseThrow(),"MaHoanTien");if(success)refunds.xacNhan(ADMIN,refund,new XacNhanHoanYeuCau(new BigDecimal("1000"),"1234567890","NGAN HANG DEMO","KHACH THU","REPORT-"+UUID.randomUUID(),true),invoices.tai(o.email(),o.code()));else refunds.danhDauLoi(ADMIN,refund,"Chưa xác minh được khoản chuyển trả fixture");jdbc.sql("UPDATE \"PhieuTraHang\" SET \"NgayNhanHang\"='2001-01-20T10:00:00+07' WHERE \"MaPhieuTraHang\"=:id").param("id",ticket).update();if(success)jdbc.sql("UPDATE \"HoanTien\" SET \"NgayHoanThanh\"='2001-01-20T10:00:00+07' WHERE \"MaHoanTien\"=:id").param("id",refund).update();return refund;}
    @Test @SuppressWarnings("unchecked") void knownSalesCashRefundsBoundariesReturningCustomersAndWeightedRatingsMatchHandCalculation(){
        jdbc.sql("UPDATE \"BienTheSanPham\" SET \"GiaBan\"=1000,\"GiaNiemYet\"=2000,\"GiaVon\"=600 WHERE \"MaBienThe\"=1").update();jdbc.sql("UPDATE \"TonKho\" SET \"SoLuongTon\"=\"SoLuongGiu\"+100 WHERE \"MaBienThe\"=1 AND \"MaKhoHang\"=1").update();settings.luu(ADMIN,new CauHinhHeThongYeuCau(false,BigDecimal.ZERO,""));String a=user(),b=user(),c=user();var previous=paid(a,1);var two=paid(a,2);var one=paid(a,1);var b1=paid(b,1);var b2=paid(b,1);var cPrevious=paid(c,1);var feb=paid(c,1);var cancelled=create(c,1,"COD");orders.huy(c,cancelled.code());refund(two,true);refund(two,false);
        var late=create(c,1,"PAYOS_MO_PHONG");payments.taoLink(c,late.code());payments.huy(c,late.code());payments.moPhong(c,late.code(),new BigDecimal("300"),UUID.randomUUID().toString());jdbc.sql("UPDATE \"GiaoDichThanhToan\" g SET \"NgayGiaoDich\"='2001-01-21T12:00:00+07' FROM \"PhienThanhToan\" p WHERE g.\"MaPhienThanhToan\"=p.\"MaPhienThanhToan\" AND p.\"MaDonHang\"=:id").param("id",late.id()).update();
        reviews.luu(a,1,new DanhGiaYeuCau(5,"Tốt","Đánh giá năm sao của fixture báo cáo"));reviews.luu(b,1,new DanhGiaYeuCau(1,"Chưa phù hợp","Đánh giá một sao của fixture báo cáo"));reviews.luu(c,1,new DanhGiaYeuCau(1,"Cần cải thiện","Đánh giá một sao tiếp theo của fixture báo cáo"));for(String email:List.of(a,b,c))jdbc.sql("UPDATE \"DanhGia\" SET \"NgayTao\"='2001-01-25T12:00:00+07' WHERE \"MaNguoiDung\"=(SELECT \"MaNguoiDung\" FROM \"NguoiDung\" WHERE \"ThuDienTu\"=:email)").param("email",email).update();
        date(previous,"2000-12-31T23:59:59+07:00");date(cPrevious,"2000-12-20T12:00:00+07:00");date(two,"2001-01-15T12:00:00+07:00");date(one,"2001-01-31T23:59:59+07:00");date(b1,"2001-01-01T00:00:00+07:00");date(b2,"2001-01-26T12:00:00+07:00");date(feb,"2001-02-01T00:00:00+07:00");
        for(String group:List.of("day","month","year")){var r=reports.xem(ADMIN,LocalDate.of(2001,1,1),LocalDate.of(2001,1,31),group);assertThat((BigDecimal)r.get("tongDoanhSo")).isEqualByComparingTo("5000");assertThat((BigDecimal)r.get("laiGop")).isEqualByComparingTo("2000");var cash=((List<Map<String,Object>>)r.get("tien")).stream().filter(x->x.get("mode").equals("MO_PHONG")).findFirst().orElseThrow();assertThat((BigDecimal)cash.get("da_thu")).isEqualByComparingTo("5300");assertThat((BigDecimal)cash.get("da_hoan")).isEqualByComparingTo("1000");assertThat((BigDecimal)cash.get("thu_rong")).isEqualByComparingTo("4300");assertThat((BigDecimal)r.get("tyLeQuayLai")).isEqualByComparingTo("50");assertThat((BigDecimal)r.get("saoTrungBinh")).isEqualByComparingTo("2.33");assertThat((BigDecimal)r.get("tyLeHaiLong")).isEqualByComparingTo("33.33");assertThat((List<Map<String,Object>>)r.get("banChay")).singleElement().satisfies(row->{assertThat(((Number)row.get("so_may")).longValue()).isEqualTo(5);assertThat((BigDecimal)row.get("doanh_thu")).isEqualByComparingTo("5000");});var returned=((List<Map<String,Object>>)r.get("traHang")).getFirst();assertThat(((Number)returned.get("so_phieu")).intValue()).isEqualTo(2);assertThat((BigDecimal)returned.get("tien_duyet")).isEqualByComparingTo("2000");}
    }
}
