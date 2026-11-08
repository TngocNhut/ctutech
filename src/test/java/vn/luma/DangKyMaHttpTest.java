package vn.luma;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import vn.luma.business.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.scheduling.enabled=false"})
class DangKyMaHttpTest {
 @LocalServerPort int port;@MockitoBean TwilioVerifyClient verify;@Autowired TaiKhoanService accounts;
 String enc(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);} String base(){return "http://127.0.0.1:"+port;}
 HttpResponse<String> get(HttpClient c,String path)throws Exception{return c.send(HttpRequest.newBuilder(URI.create(base()+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
 HttpResponse<String> post(HttpClient c,String path,Map<String,String> p)throws Exception{String body=p.entrySet().stream().map(e->enc(e.getKey())+"="+enc(e.getValue())).collect(java.util.stream.Collectors.joining("&"));return c.send(HttpRequest.newBuilder(URI.create(base()+path)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());}
 String csrf(String html){var m=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);assertThat(m.find()).isTrue();return m.group(1);}
 @Test void emailRegistrationWorksWithoutOtpAndPhoneLoginIsUnavailable()throws Exception{
  var c=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();String email="new-"+UUID.randomUUID()+"@example.test";
  var form=get(c,"/tao-tai-khoan");assertThat(form.statusCode()).isEqualTo(200);assertThat(form.body()).contains("Ngày sinh").doesNotContain("Tạo tài khoản với Google", "Email hoặc số điện thoại");String token=csrf(form.body());
  var phone=post(c,"/tao-tai-khoan",Map.of("_csrf",token,"ho","Nguyễn","ten","An","ngaySinh","2000-01-01","taiKhoan","0901234567","matKhau","SecureDemo!2026","xacNhanMatKhau","SecureDemo!2026"));assertThat(phone.statusCode()).isEqualTo(200);assertThat(phone.body().toLowerCase(Locale.ROOT)).contains("email");
  var start=post(c,"/tao-tai-khoan",Map.of("_csrf",csrf(phone.body()),"ho","Nguyễn","ten","An","ngaySinh","2000-01-01","taiKhoan",email,"matKhau","SecureDemo!2026","xacNhanMatKhau","SecureDemo!2026","vaiTro","QuanTri"));assertThat(start.headers().firstValue("Location").orElseThrow()).endsWith("/login?registered");assertThat(accounts.hienTai(email).daXacMinh()).isFalse();assertThat(accounts.hienTai(email).vaiTro()).isEqualTo("KhachHang");verifyNoInteractions(verify);
  var login=get(c,"/login");assertThat(login.body()).doesNotContain("Đăng nhập với Google", "Email hoặc số điện thoại");var logged=post(c,"/login",Map.of("_csrf",csrf(login.body()),"username",email,"password","SecureDemo!2026"));assertThat(logged.headers().firstValue("Location").orElseThrow()).endsWith("/tai-khoan");assertThat(get(c,"/tai-khoan").statusCode()).isEqualTo(200);assertThat(get(c,"/oauth2/authorization/google").statusCode()).isIn(302,403,404);assertThat(get(c,"/xac-nhan-dang-ky").statusCode()).isIn(302,403,404);
 }
}
