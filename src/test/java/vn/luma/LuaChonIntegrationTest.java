package vn.luma;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.business.*;
import vn.luma.web.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false"})
@Transactional
class LuaChonIntegrationTest {
    @Autowired LuaChonTrinhDuyet choices;
    @Autowired MuaLaiService buyAgain;
    @Autowired SanPhamService products;
    @Autowired TaiKhoanService users;
    @Autowired DonHangService orders;
    @Autowired GioHangService carts;
    @Autowired BanDoService maps;
    @Autowired JdbcClient jdbc;
    String user(){String email="choices-"+UUID.randomUUID()+"@example.test";users.seedDemo(email,"Khách lựa chọn","KhachHang","ChoiceDemo!2026");return email;}
    String order(String email){carts.sua(email,1,2,true);var a=new DiaChiYeuCau("Khách thử","0901234567","TP.HCM","Phường thử","123 Đường thử","","",null,null,false);return orders.datCOD(email,new DatHangYeuCau(UUID.randomUUID().toString(),orders.noiDungGio(email),"",a),maps.chuanHoa(a));}
    @Test void recentModelsRememberLatestVariantAreBoundedAndCanBeCleared(){
        var session=new MockHttpSession();choices.daXem(session,1);choices.daXem(session,2);assertThat(choices.xem(session)).singleElement().satisfies(v->assertThat(v.sku().maBienThe()).isEqualTo(2));for(var p:products.danhSach("",""))choices.daXem(session,p.maSkuGoiY());assertThat(choices.xem(session)).hasSize(12);assertThat(choices.xem(new MockHttpSession())).isEmpty();choices.xoaDaXem(session);assertThat(choices.xem(session)).isEmpty();
    }
    @Test void buyingAgainIsOwnerOnlyAndUsesCurrentPricesWithoutCreatingAnOrder(){
        String email=user(),other=user(),code=order(email);assertThatThrownBy(()->buyAgain.them(email,code)).isInstanceOf(IllegalArgumentException.class);orders.huy(email,code);BigDecimal newPrice=products.cauHinh(1).giaBan().subtract(BigDecimal.ONE);jdbc.sql("UPDATE \"BienTheSanPham\" SET \"GiaBan\"=:price,\"DangKinhDoanh\"=false WHERE \"MaBienThe\"=1").param("price",newPrice).update();var result=buyAgain.them(email,code);assertThat(result.daGop()).isEqualTo(2);assertThat(result.canKiemTra()).isEqualTo(1);var cart=carts.xem(email,Map.of());assertThat(cart.dong()).singleElement().satisfies(d->{assertThat(d.soLuong()).isEqualTo(2);assertThat(d.sku().giaBan()).isEqualByComparingTo(newPrice);assertThat(d.coTheMua()).isFalse();});assertThat(orders.danhSach(email)).hasSize(1);assertThat(orders.chiTiet(email,code)).containsEntry("TrangThaiDon","DA_HUY");assertThatThrownBy(()->buyAgain.them(other,code)).isInstanceOf(KhongTimThayException.class);assertThatThrownBy(()->buyAgain.them("admin@luma.local",code)).isInstanceOf(KhongTimThayException.class);assertThat(carts.xem(other,Map.of()).dong()).isEmpty();
    }
    @Test void buyingAgainRetainsOverflowInsteadOfSilentlyDroppingIt(){
        String email=user(),code=order(email);orders.huy(email,code);jdbc.sql("UPDATE \"TonKho\" SET \"SoLuongTon\"=\"SoLuongGiu\"+200 WHERE \"MaBienThe\"=1 AND \"MaKhoHang\"=1").update();carts.sua(email,1,98,false);var result=buyAgain.them(email,code);assertThat(result.daGop()).isEqualTo(1);assertThat(result.conLai()).containsExactly(Map.entry(1L,1));assertThat(carts.xem(email,Map.of()).dong().getFirst().soLuong()).isEqualTo(99);
    }
}
