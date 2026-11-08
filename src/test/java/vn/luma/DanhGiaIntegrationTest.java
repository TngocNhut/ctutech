package vn.luma;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.business.*;
import vn.luma.web.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false"})
@Transactional
class DanhGiaIntegrationTest {
    static final String ADMIN="admin@luma.local";
    @Autowired DanhGiaService reviews;
    @Autowired TaiKhoanService users;
    @Autowired DonHangService orders;
    @Autowired GioHangService carts;
    @Autowired GiaoNhanService shipping;
    @Autowired BanDoService maps;
    @Autowired JdbcClient jdbc;
    @Autowired ThongBaoService notifications;
    record Order(String email,String code) { }
    String user(){String e="review-"+UUID.randomUUID()+"@example.test";users.seedDemo(e,"Khách nhận xét thử","KhachHang","StrongDemo!2026");return e;}
    Order pending(){String email=user();carts.sua(email,1,1,true);var address=new DiaChiYeuCau("Khách đánh giá","0901234567","TP.HCM","Phường thử","123 Đường thử","","",null,null,false);var request=new DatHangYeuCau(UUID.randomUUID().toString(),orders.noiDungGio(email),"",address);return new Order(email,orders.datCOD(email,request,maps.chuanHoa(address)));}
    void delivered(Order order){shipping.xacNhan(ADMIN,order.code(),true);long carrier=jdbc.sql("SELECT \"MaDonViVanChuyen\" FROM \"DonViVanChuyen\" WHERE \"KyHieu\"='LUMA_NOI_BO'").query(Long.class).single();shipping.taoVanDon(ADMIN,order.code(),new TaoVanDonYeuCau(carrier,"REVIEW-"+order.code(),null,null));for(String action:List.of("BAN_GIAO","DA_GIAO"))shipping.capNhat(ADMIN,order.code(),new SuKienGiaoNhanYeuCau(action,UUID.randomUUID().toString(),"Kiểm thử đánh giá sau mua"));}
    void paid(Order order){shipping.thuCOD(ADMIN,order.code(),new ThuCODYeuCau((BigDecimal)orders.chiTiet(ADMIN,order.code()).get("TongThanhToan"),"REVIEW-COD-"+order.code(),true));}
    Order completed(){var order=pending();delivered(order);paid(order);return order;}
    DanhGiaYeuCau form(int stars){return new DanhGiaYeuCau(stars,"Nhận xét thử","Nội dung nhận xét kiểm thử từ khách đã mua.");}
    long id(String email){return ((Number)reviews.banSoan(email,1).danhGia().get("MaDanhGia")).longValue();}
    @Test void onlyBuyerOfCompletedPaidOrderMayWriteOneReviewPerModel(){
        String stranger=user();assertThatThrownBy(()->reviews.luu(stranger,1,form(5))).isInstanceOf(IllegalArgumentException.class);var order=pending();
        assertThatThrownBy(()->reviews.luu(order.email(),1,form(5))).isInstanceOf(IllegalArgumentException.class);delivered(order);
        assertThatThrownBy(()->reviews.luu(order.email(),1,form(5))).isInstanceOf(IllegalArgumentException.class);paid(order);
        reviews.luu(order.email(),1,form(5));long id=id(order.email());reviews.luu(order.email(),1,form(3));assertThat(id(order.email())).isEqualTo(id);
        assertThat(reviews.daMua(order.email(),1)).hasSize(1);assertThat(((Number)reviews.banSoan(order.email(),1).danhGia().get("SoSao")).intValue()).isEqualTo(3);
        long line=((Number)orders.dong(order.email(),order.code()).getFirst().get("MaChiTietDonHang")).longValue();assertThat(reviews.banSoan(order.email(),1).danhGia().get("MaChiTietDonHang")).isEqualTo(line);
        assertThatThrownBy(()->reviews.luu(order.email(),2,form(5))).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->reviews.banSoan(stranger,1)).isInstanceOf(KhongTimThayException.class);
    }
    @Test void moderationKeepsHiddenReviewHiddenAfterOwnerEditAndStatisticsUseOnlyVisibleRows(){
        var a=completed();var b=completed();reviews.luu(a.email(),1,form(1));reviews.luu(b.email(),1,form(5));
        assertThat(reviews.congKhai("iphone-13",1).tongHop()).containsEntry("so_luot",2L);assertThat((BigDecimal)reviews.congKhai("iphone-13",1).tongHop().get("trung_binh")).isEqualByComparingTo("3.0");
        long id=id(b.email());reviews.duyet(ADMIN,id,new KiemDuyetDanhGiaYeuCau(false,"Cửa hàng đang kiểm tra nội dung","Ẩn nhận xét thử"));reviews.luu(b.email(),1,form(4));
        assertThat(reviews.banSoan(b.email(),1).danhGia()).containsEntry("TrangThai","BI_AN");assertThat(reviews.congKhai("iphone-13",1).tongHop()).containsEntry("so_luot",1L).containsEntry("sao_1",1L).containsEntry("sao_4",0L);
        assertThatThrownBy(()->reviews.duyet(a.email(),id,new KiemDuyetDanhGiaYeuCau(true,"","Tự mở"))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        reviews.duyet(ADMIN,id,new KiemDuyetDanhGiaYeuCau(true,"Cảm ơn nhận xét sau mua","Mở lại sau kiểm tra"));assertThat((BigDecimal)reviews.congKhai("iphone-13",1).tongHop().get("trung_binh")).isEqualByComparingTo("2.5");assertThat(reviews.congKhai("iphone-13",1).danhGia()).anyMatch(r->"Cảm ơn nhận xét sau mua".equals(r.get("PhanHoiQuanTri")));
        reviews.duyet(ADMIN,id,new KiemDuyetDanhGiaYeuCau(true,"Cảm ơn nhận xét sau mua","Lưu lại cùng phản hồi"));assertThat(notifications.danhSach(b.email(),false,1).stream().filter(n->"PHAN_HOI_DANH_GIA".equals(n.get("LoaiThongBao"))).count()).isEqualTo(2);
    }
    @Test void inputBoundsAndLiteralMarkupArePreservedWithoutChangingPurchaseProof(){
        var order=completed();assertThatThrownBy(()->reviews.luu(order.email(),1,form(6))).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->reviews.luu(order.email(),1,new DanhGiaYeuCau(5,"","ngắn"))).isInstanceOf(IllegalArgumentException.class);
        String body="Nội dung thử <script>window.REVIEW_XSS=true</script>";reviews.luu(order.email(),1,new DanhGiaYeuCau(4,"Tiêu đề thử",body));assertThat(reviews.congKhai("iphone-13",1).danhGia().getFirst()).containsEntry("NoiDung",body).doesNotContainKeys("ThuDienTu","MaDonCongKhai");
        assertThatThrownBy(()->reviews.duyet(ADMIN,id(order.email()),new KiemDuyetDanhGiaYeuCau(false,"",""))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void hiddenProductReviewsRemainEditableByBuyerAndAreNotPubliclyAccessible(){
        var order=completed();reviews.luu(order.email(),1,form(4));jdbc.sql("UPDATE \"SanPham\" SET \"DangHienThi\"=false WHERE \"MaSanPham\"=1").update();
        assertThatThrownBy(()->reviews.congKhai("iphone-13",1)).isInstanceOf(KhongTimThayException.class);reviews.luu(order.email(),1,form(2));assertThat(reviews.banSoan(order.email(),1).tenSanPham()).isEqualTo("iPhone 13");assertThat(reviews.daMua(order.email(),1)).hasSize(1);assertThat(reviews.quanTri(ADMIN,"TAT_CA","iPhone 13",1)).hasSize(1);
    }
}
