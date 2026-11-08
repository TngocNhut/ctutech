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
import vn.luma.data.*;
import vn.luma.web.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false"})
@Transactional
class KhuyenMaiIntegrationTest {
    static final String ADMIN="admin@luma.local";
    @Autowired KhuyenMaiService admin;
    @Autowired KhuyenMaiDAO dao;
    @Autowired SanPhamDAO products;
    @Autowired KhoService stock;
    @Autowired GioHangService carts;
    @Autowired DonHangService orders;
    @Autowired TaiKhoanService users;
    @Autowired BanDoService maps;
    @Autowired JdbcClient jdbc;
    @Autowired KhuyenMaiController controller;
    BigDecimal money(String n){return new BigDecimal(n);}
    LocalDateTime now(){return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).withNano(0);}
    String code(){return "KM-"+UUID.randomUUID().toString().substring(0,20).toUpperCase(Locale.ROOT);}
    ChuongTrinhYeuCau form(String code,int priority,String state,boolean stack){return new ChuongTrinhYeuCau(code,"Chiến dịch thử","Mô tả",now().minusDays(1),now().plusDays(1),priority,state,stack);}
    long campaign(int priority,boolean stack){return admin.luu(ADMIN,null,form(code(),priority,"CONG_BO",stack));}
    void rule(long id,long sku,String type,String value){admin.luuQuyTac(ADMIN,id,new QuyTacKhuyenMaiYeuCau(sku,type,money(value),true));}
    String user(){String email="promotion-"+UUID.randomUUID()+"@example.test";users.seedDemo(email,"Khách thử ưu đãi","KhachHang","StrongDemo!2026");return email;}
    DiaChiYeuCau address(){return new DiaChiYeuCau("Khách thử","0901234567","Thành phố Hồ Chí Minh","Phường Bến Nghé","123 Đường thử nghiệm","","",null,null,false);}
    DatHangYeuCau request(String email,String coupon){return new DatHangYeuCau(UUID.randomUUID().toString(),orders.noiDungGio(email),coupon,address());}
    VoucherYeuCau voucher(String code,int limit,boolean active){return new VoucherYeuCau(code,"Mã giảm thử","SO_TIEN",money("100000"),null,money("1000000"),now().minusDays(1),now().plusDays(1),limit,1,true,active);}

    @Test void winnerUsesPriorityThenPriceThenStableIdAndRoundsDiscountPerUnit(){
        jdbc.sql("UPDATE \"BienTheSanPham\" SET \"GiaBan\"=10000003,\"GiaNiemYet\"=20000000 WHERE \"MaBienThe\"=1").update();
        long first=campaign(0,true);rule(first,1,"PHAN_TRAM","12.34");
        assertThat(dao.gia(List.of(1L)).get(1L).gia()).isEqualByComparingTo("8766003");
        long second=campaign(0,true);rule(second,1,"SO_TIEN","2000000");assertThat(dao.gia(List.of(1L)).get(1L).chuongTrinh()).isEqualTo(second);
        long tie=campaign(0,true);rule(tie,1,"SO_TIEN","2000000");assertThat(dao.gia(List.of(1L)).get(1L).chuongTrinh()).isEqualTo(second);
        long priority=campaign(1,false);rule(priority,1,"SO_TIEN","1");assertThat(dao.gia(List.of(1L)).get(1L).gia()).isEqualByComparingTo("10000002");
        jdbc.sql("UPDATE \"ChuongTrinhKhuyenMai\" SET \"NgayKetThuc\"=clock_timestamp()-interval '1 second' WHERE \"MaChuongTrinh\"=:id").param("id",priority).update();
        assertThat(dao.gia(List.of(1L)).get(1L).chuongTrinh()).isEqualTo(second);
        jdbc.sql("UPDATE \"ChuongTrinhKhuyenMai\" SET \"TrangThai\"='TAM_DUNG'").update();
        assertThat(dao.gia(List.of(1L)).get(1L).chuongTrinh()).isNull();
    }
    @Test void invalidCurrentRuleNeverMakesAFreeOrNegativeProduct(){
        long id=campaign(0,true);rule(id,1,"SO_TIEN","1000000");
        jdbc.sql("UPDATE \"BienTheSanPham\" SET \"GiaBan\"=1000000 WHERE \"MaBienThe\"=1").update();
        assertThat(dao.gia(List.of(1L)).get(1L).gia()).isEqualByComparingTo("1000000");assertThat(dao.gia(List.of(1L)).get(1L).chuongTrinh()).isNull();
        assertThatThrownBy(()->rule(id,1,"SO_TIEN","1000000")).isInstanceOf(IllegalArgumentException.class);
        admin.luuQuyTac(ADMIN,id,new QuyTacKhuyenMaiYeuCau(1,"SO_TIEN",money("1000000"),false));assertThat(admin.quyTac(ADMIN,id).getFirst()).containsEntry("DangHoatDong",false);
        assertThatThrownBy(()->rule(id,1,"SO_TIEN","1.5")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->rule(id,1,"PHAN_TRAM","100")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->rule(id,999999999,"SO_TIEN","1")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void catalogueFilterCartAndCheckoutSharePriceWhileAdminKeepsBasePrice(){
        var base=dao.gia(List.of(1L)).get(1L).giaGoc();long id=campaign(0,true);rule(id,1,"SO_TIEN",base.subtract(money("2222222")).toPlainString());
        var filter=new BoLocSanPham("iPhone 13","","",null,money("2500000"),null,null,"","",false,"gia-tang",1);
        assertThat(products.dem(filter)).isEqualTo(1);var card=products.loc(filter,1,12).getFirst();assertThat(card.giaTu()).isEqualByComparingTo("2222222");assertThat(card.maSkuGoiY()).isEqualTo(1);
        assertThat(products.bienThe("iphone-13").getFirst().giaBan()).isEqualByComparingTo("2222222");
        assertThat(stock.danhSach(ADMIN).stream().filter(b->b.maBienThe()==1).findFirst().orElseThrow().giaBan()).isEqualByComparingTo(base);
        String email=user();carts.sua(email,1,2,true);assertThat(carts.xem(email,Map.of()).tongTien()).isEqualByComparingTo("4444444");assertThat(orders.baoGia(email,request(email,"")).tienHang()).isEqualByComparingTo("4444444");
    }
    @Test void orderKeepsCampaignSnapshotAndVoucherAllocationAfterCampaignChanges(){
        long id=campaign(0,false);rule(id,1,"PHAN_TRAM","10");String email=user();carts.sua(email,1,2,true);
        assertThatThrownBy(()->orders.baoGia(email,request(email,"LUMA100"))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("không cho dùng kèm");
        assertThat(jdbc.sql("SELECT count(*) FROM \"DonHang\" WHERE \"MaNguoiDung\"=:id").param("id",users.hienTai(email).maNguoiDung()).query(Integer.class).single()).isZero();
        String programCode=(String)admin.chiTiet(ADMIN,id).get("KyHieu");admin.luu(ADMIN,id,form(programCode,0,"CONG_BO",true));
        var offer=dao.gia(List.of(1L)).get(1L);var r=request(email,"LUMA100");String order=orders.datCOD(email,r,maps.chuanHoa(r.diaChi()));
        var line=orders.dong(email,order).getFirst();assertThat((BigDecimal)line.get("DonGia")).isEqualByComparingTo(offer.gia());assertThat(line).containsEntry("TenChuongTrinhLucDat","Chiến dịch thử");
        assertThat((BigDecimal)line.get("GiamGiaPhanBo")).isEqualByComparingTo("100000");
        admin.luu(ADMIN,id,new ChuongTrinhYeuCau(programCode,"Tên đổi sau đặt","",now().minusDays(1),now().plusDays(1),0,"TAM_DUNG",false));rule(id,1,"PHAN_TRAM","25");
        assertThat(orders.dong(email,order).getFirst()).containsEntry("TenChuongTrinhLucDat","Chiến dịch thử");assertThat((BigDecimal)orders.dong(email,order).getFirst().get("DonGia")).isEqualByComparingTo(offer.gia());
        assertThat(orders.datCOD(email,r,maps.chuanHoa(r.diaChi()))).isEqualTo(order);
        assertThat(jdbc.sql("SELECT count(*) FROM \"ApDungKhuyenMai\" a JOIN \"ChiTietDonHang\" d USING (\"MaChiTietDonHang\") JOIN \"DonHang\" o USING (\"MaDonHang\") WHERE o.\"MaDonCongKhai\"=:code").param("code",order).query(Integer.class).single()).isEqualTo(1);
        orders.huy(email,order);
    }
    @Test void voucherAdminHonoursRolesPublicVisibilityLimitsAndHistoricalOrders(){
        String coupon=code(),email=user();long voucher=admin.luuVoucher(ADMIN,null,voucher(coupon,2,true));assertThat(admin.congKhai()).anyMatch(v->coupon.equals(v.get("KyHieu")));
        assertThatThrownBy(()->admin.luuVoucher(email,null,voucher(code(),1,true))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        carts.sua(email,1,1,true);var r=request(email,coupon);String order=orders.datCOD(email,r,maps.chuanHoa(r.diaChi()));
        admin.luuVoucher(ADMIN,voucher,voucher(coupon,2,false));assertThat(admin.congKhai()).noneMatch(v->coupon.equals(v.get("KyHieu")));assertThat((BigDecimal)orders.chiTiet(email,order).get("TienGiamGia")).isEqualByComparingTo("100000");
        carts.sua(email,1,1,true);assertThatThrownBy(()->orders.baoGia(email,request(email,coupon))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->admin.luuVoucher(ADMIN,voucher,voucher(code(),2,true))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("giữ cố định");orders.huy(email,order);
    }
    @Test void rejectsInvalidVoucherPercentCapAndPeriod(){
        String c=code();var start=now();assertThatThrownBy(()->admin.luu(ADMIN,null,new ChuongTrinhYeuCau(c,"Thử","",start,start,0,"NHAP",false))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->admin.luuVoucher(ADMIN,null,new VoucherYeuCau(c,"Thử","PHAN_TRAM",money("10"),null,BigDecimal.ZERO,start,start.plusDays(1),1,1,true,true))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->admin.luuVoucher(ADMIN,null,new VoucherYeuCau(c,"Thử","SO_TIEN",money("10"),money("1"),BigDecimal.ZERO,start,start.plusDays(1),1,1,true,true))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void adminFormsConvertJdbcTimestampsToVietnameseLocalInputs(){
        var time=now();var c=new ChuongTrinhYeuCau(code(),"Thời gian thử","",time,time.plusDays(1),0,"NHAP",false);
        long id=admin.luu(ADMIN,null,c);var model=new org.springframework.ui.ExtendedModelMap();
        assertThat(controller.sua(id,()->ADMIN,model)).isEqualTo("quan-tri/khuyen-mai-form");
        assertThat(((ChuongTrinhYeuCau)model.get("form")).batDau()).isEqualTo(time);
        long v=admin.luuVoucher(ADMIN,null,voucher(code(),2,true));model.clear();
        assertThat(controller.voucher(v,()->ADMIN,model)).isEqualTo("quan-tri/ma-giam-gia-form");
        assertThat(((VoucherYeuCau)model.get("form")).batDau()).isBetween(time.minusDays(1),now().minusDays(1));
    }
}
