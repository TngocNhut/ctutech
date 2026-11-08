package vn.luma;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UserDetailsService;
import vn.luma.business.*;
import vn.luma.web.DangKyMoiYeuCau;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.scheduling.enabled=false"})
@Transactional
class DangKyMaIntegrationTest {
 @Autowired vn.luma.data.TaiKhoanDAO accountDao;@Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;@Autowired DangKyMaService registrations;@Autowired TaiKhoanService accounts;@Autowired UserDetailsService auth;@Autowired GioHangService carts;@Autowired DonHangService orders;@Autowired BanDoService maps;
 @MockitoBean TwilioVerifyClient verify;
 @Test void emailSignupCanOrderWithoutClaimingVerifiedEmail(){
  String email="direct-"+UUID.randomUUID()+"@example.test";accounts.dangKyTrucTiep(new DangKyMoiYeuCau("Lê","Bình",LocalDate.of(1998,3,2),email,"SecureDemo!2026","SecureDemo!2026"));
  assertThat(accounts.hienTai(email).daXacMinh()).isFalse();assertThat(accounts.hienTai(email).vaiTro()).isEqualTo("KhachHang");assertThatThrownBy(()->accounts.quanTri(email)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);assertThat(auth.loadUserByUsername(email).getUsername()).isEqualTo(email);verifyNoInteractions(verify);
  carts.sua(email,1,1,true);var address=new vn.luma.web.DiaChiYeuCau("Lê Bình","0901234567","Hà Nội","Ba Đình","1 Phố Thử","","",null,null,false);var order=new vn.luma.web.DatHangYeuCau(UUID.randomUUID().toString(),orders.noiDungGio(email),"",address);assertThat(orders.datCOD(email,order,maps.chuanHoa(address))).startsWith("LM-");
  String phone="+849"+String.format("%08d",Math.abs(UUID.randomUUID().getMostSignificantBits()%100000000));accountDao.taoDaXacMinh("Người dùng điện thoại",null,phone,LocalDate.of(2000,1,1),encoder.encode("SecureDemo!2026"));assertThatThrownBy(()->auth.loadUserByUsername(phone)).isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
 }
 @Test void fiveWrongAttemptsCannotCreateAnAccount(){
  when(verify.available()).thenReturn(true);String email="limit-"+UUID.randomUUID()+"@example.test";
  var pending=registrations.batDau(new DangKyMoiYeuCau("Nguyễn","An",LocalDate.of(2000,1,1),email,"SecureDemo!2026","SecureDemo!2026"));
  for(int i=0;i<5;i++)pending=registrations.thuSai(pending);
  var locked=pending;assertThatThrownBy(()->registrations.xacNhan(locked,"123456")).isInstanceOf(IllegalArgumentException.class);
  verify(verify,never()).check(anyString(),anyString());assertThatThrownBy(()->accounts.hienTai(email)).isInstanceOf(IllegalArgumentException.class);
 }

}

