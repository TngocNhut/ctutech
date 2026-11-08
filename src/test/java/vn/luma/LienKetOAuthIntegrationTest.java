package vn.luma;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.simple.JdbcClient;
import vn.luma.business.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false"})
@Transactional
class LienKetOAuthIntegrationTest {
    @Autowired LienKetOAuthService links;
    @Autowired TaiKhoanService users;
    @Autowired JdbcClient jdbc;
    static final String PASSWORD="OAuthDemo!2026";
    String user(){String e="oauth-"+UUID.randomUUID()+"@example.test";users.seedDemo(e,"Khách OAuth","KhachHang",PASSWORD);return e;}
    LienKetOAuthService.DanhTinh identity(String email){return new LienKetOAuthService.DanhTinh("FACEBOOK",UUID.randomUUID().toString(),email,Instant.now().plusSeconds(300));}
    @Test void matchingEmailNeverCreatesOrLinksAccountAndOnlyConfirmedOwnerCanLink(){String a=user(),b=user();var d=identity(a);assertThat(links.dangNhap(d)).isEmpty();assertThat(links.cuaToi(a)).isEmpty();links.gan(b,PASSWORD,d);assertThat(links.dangNhap(d).orElseThrow().thuDienTu()).isEqualTo(b);assertThat(links.dangNhap(d).orElseThrow().vaiTro()).isEqualTo("KhachHang");assertThatThrownBy(()->links.gan(a,PASSWORD,d)).isInstanceOf(IllegalArgumentException.class);links.gan(b,PASSWORD,d);assertThat(links.cuaToi(b)).hasSize(1);}
    @Test void passwordExpiryAndVerifiedLocalEmailAreRequired(){String a=user();var d=identity(a);assertThatThrownBy(()->links.gan(a,"wrong",d)).isInstanceOf(IllegalArgumentException.class);assertThat(links.cuaToi(a)).isEmpty();assertThatThrownBy(()->links.gan(a,PASSWORD,new LienKetOAuthService.DanhTinh("GOOGLE","expired","",Instant.now().minusSeconds(1)))).isInstanceOf(IllegalArgumentException.class);jdbc.sql("UPDATE \"NguoiDung\" SET \"NgayXacMinhThuDienTu\"=NULL WHERE \"ThuDienTu\"=:e").param("e",a).update();assertThatThrownBy(()->links.gan(a,PASSWORD,d)).isInstanceOf(IllegalArgumentException.class);}
    @Test void unlinkUsesPasswordAndDoesNotChangeLocalCredentialsOrRole(){String a=user();var d=identity(a);links.gan(a,PASSWORD,d);assertThatThrownBy(()->links.bo(a,"wrong","FACEBOOK")).isInstanceOf(IllegalArgumentException.class);assertThat(links.dangNhap(d)).isPresent();links.bo(a,PASSWORD,"FACEBOOK");assertThat(links.dangNhap(d)).isEmpty();assertThat(users.hienTai(a).vaiTro()).isEqualTo("KhachHang");assertThat(jdbc.sql("SELECT \"MatKhauBam\" IS NOT NULL FROM \"NguoiDung\" WHERE \"ThuDienTu\"=:e").param("e",a).query(Boolean.class).single()).isTrue();}
    @Test void linkingGoogleIsDisabled(){var email=user();var google=new LienKetOAuthService.DanhTinh("GOOGLE",UUID.randomUUID().toString(),email,Instant.now().plusSeconds(300));assertThatThrownBy(()->links.gan(email,PASSWORD,google)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("không khả dụng");}
    @Test void lockedUserCannotUseLinkedIdentity(){String a=user();var d=identity(a);links.gan(a,PASSWORD,d);jdbc.sql("UPDATE \"NguoiDung\" SET \"TrangThai\"='BI_KHOA' WHERE \"ThuDienTu\"=:e").param("e",a).update();assertThatThrownBy(()->links.dangNhap(d)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);}
}
