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
class CauHinhHeThongIntegrationTest {
    static final String ADMIN="admin@luma.local";
    @Autowired CauHinhHeThongService settings;
    @Autowired TaiKhoanService users;
    @Autowired DonHangService orders;
    @Autowired GioHangService carts;
    @Autowired BanDoService maps;
    @Autowired JdbcClient jdbc;
    @Test void onlyAllowlistedTypedSettingsAreSavedWithAuditAndPlainTextRoundTrips(){
        String text="Thông báo \"bản thử\" <script>test</script>\nDòng hai \\ kết thúc";var form=new CauHinhHeThongYeuCau(true,new BigDecimal("12345"),text);settings.luu(ADMIN,form);assertThat(settings.hienTai()).isEqualTo(form);assertThat(orders.phiGiao()).isEqualByComparingTo("12345");assertThat(jdbc.sql("SELECT count(*) FROM \"NhatKyQuanTri\" WHERE \"HanhDong\"='LUU_CAU_HINH_HE_THONG'").query(Long.class).single()).isPositive();
        assertThatThrownBy(()->settings.luu("khach@luma.local",new CauHinhHeThongYeuCau(false,BigDecimal.ZERO,"Đổi trái quyền"))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);assertThat(settings.hienTai()).isEqualTo(form);
    }
    @Test void invalidAmountsOrOverlongMessageDoNotPartiallyChangeSettings(){
        var original=settings.hienTai();for(String bad:List.of("-1","0.5","10000001","1e100000"))assertThatThrownBy(()->settings.luu(ADMIN,new CauHinhHeThongYeuCau(!original.batChatbot(),new BigDecimal(bad),"Không được lưu"))).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->settings.luu(ADMIN,new CauHinhHeThongYeuCau(true,BigDecimal.ZERO,"a".repeat(301)))).isInstanceOf(IllegalArgumentException.class);assertThat(settings.hienTai()).isEqualTo(original);settings.luu(ADMIN,new CauHinhHeThongYeuCau(false,BigDecimal.ZERO,""));assertThat(orders.phiGiao()).isZero();assertThat(settings.hienTai().thongBao()).isEmpty();
    }
    @Test void changedShippingFeeAffectsNewOrdersAndReplayKeepsOriginalSnapshot(){
        String email="settings-"+UUID.randomUUID()+"@example.test";users.seedDemo(email,"Khách cấu hình","KhachHang","SettingsDemo!2026");settings.luu(ADMIN,new CauHinhHeThongYeuCau(false,new BigDecimal("12345"),"Thông báo một"));var address=new DiaChiYeuCau("Khách thử","0901234567","TP.HCM","Phường thử","123 Đường thử","","",null,null,false);carts.sua(email,1,1,true);var request=new DatHangYeuCau(UUID.randomUUID().toString(),orders.noiDungGio(email),"",address);String first=orders.datCOD(email,request,maps.chuanHoa(address));
        settings.luu(ADMIN,new CauHinhHeThongYeuCau(true,new BigDecimal("54321"),"Thông báo hai"));assertThat(orders.datCOD(email,request,maps.chuanHoa(address))).isEqualTo(first);assertThat((BigDecimal)orders.chiTiet(email,first).get("PhiVanChuyen")).isEqualByComparingTo("12345");carts.sua(email,1,1,true);String second=orders.datCOD(email,new DatHangYeuCau(UUID.randomUUID().toString(),orders.noiDungGio(email),"",address),maps.chuanHoa(address));assertThat((BigDecimal)orders.chiTiet(email,second).get("PhiVanChuyen")).isEqualByComparingTo("54321");orders.huy(email,first);orders.huy(email,second);
    }
}
