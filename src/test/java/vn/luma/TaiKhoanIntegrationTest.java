package vn.luma;

import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.luma.business.TaiKhoanService;
import vn.luma.web.DangKyYeuCau;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test",
    "app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false"})
class TaiKhoanIntegrationTest {
    @LocalServerPort int port;
    @Autowired TaiKhoanService accounts;
    @Autowired JdbcClient jdbc;
    @Autowired PasswordEncoder encoder;
    final String password="StrongDemo!2026";
    HttpClient browser(){return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();}
    HttpResponse<String> get(HttpClient client,String route)throws Exception{
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+route)).GET().build(),HttpResponse.BodyHandlers.ofString());
    }
    String csrf(String html){var m=Pattern.compile("<input[^>]*name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);assertThat(m.find()).isTrue();return m.group(1);}
    HttpResponse<String> post(HttpClient client,String route,Map<String,String> fields)throws Exception{
        String body=fields.entrySet().stream().map(e->URLEncoder.encode(e.getKey(),StandardCharsets.UTF_8)+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&"));
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+route)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    String token(String email,String route)throws Exception{
        try(var files=Files.list(Path.of(".local/test-mail"))){
            var contents=files.map(p->{try{return Files.readString(p);}catch(Exception e){throw new RuntimeException(e);}}).filter(s->s.contains("Đến: "+email+"\n")&&s.contains(route+"?token=")).toList();
            assertThat(contents).hasSize(1);
            var m=Pattern.compile(Pattern.quote(route)+"\\?token=([A-Za-z0-9_-]{43})").matcher(contents.get(0));assertThat(m.find()).isTrue();return m.group(1);
        }
    }
    @Test void registrationValidationCsrfRolesAndProfileIsolation()throws Exception{
        String email="registration-"+UUID.randomUUID()+"@example.test";
        var client=browser();String page=get(client,"/tao-tai-khoan").body();
        assertThat(post(client,"/tao-tai-khoan",Map.of("ho","Test","ten","User","taiKhoan",email,"matKhau",password,"xacNhanMatKhau",password)).statusCode()).isEqualTo(403);
        var invalid=post(client,"/tao-tai-khoan",Map.of("_csrf",csrf(page),"ho","Test","ten","User","ngaySinh","2000-01-01","taiKhoan",email,"matKhau","short","xacNhanMatKhau","short"));
        assertThat(invalid.statusCode()).isEqualTo(200);assertThat(jdbc.sql("SELECT count(*) FROM \"NguoiDung\" WHERE \"ThuDienTu\"=:email").param("email",email).query(Integer.class).single()).isZero();
        accounts.dangKy(new DangKyYeuCau("Người thử",email.toUpperCase(Locale.ROOT),password,password));assertThat(accounts.hienTai(email).vaiTro()).isEqualTo("KhachHang");
        String hash=jdbc.sql("SELECT \"MatKhauBam\" FROM \"NguoiDung\" WHERE \"ThuDienTu\"=:email").param("email",email).query(String.class).single();assertThat(hash).isNotEqualTo(password);assertThat(encoder.matches(password,hash)).isTrue();
        String verify=token(email,"/xac-minh-email");accounts.xacMinh(verify);assertThat(accounts.hienTai(email).daXacMinh()).isTrue();assertThatThrownBy(()->accounts.xacMinh(verify)).isInstanceOf(IllegalArgumentException.class);
        assertThat(post(client,"/login",Map.of("_csrf",csrf(get(client,"/login").body()),"username",email,"password",password)).statusCode()).isEqualTo(302);
        assertThat(get(client,"/tai-khoan").body()).contains(email,"Người thử");
        assertThat(get(client,"/quan-tri").statusCode()).isEqualTo(403);
        var updated=post(client,"/tai-khoan/ho-so",Map.of("_csrf",csrf(get(client,"/tai-khoan").body()),"hoTen","Tên mới","soDienThoai","0901234567","MaNguoiDung","1"));
        assertThat(updated.statusCode()).isEqualTo(302);assertThat(accounts.hienTai(email).hoTen()).isEqualTo("Tên mới");
    }
    @Test void resetIsSingleUseAndRevokesExistingSession()throws Exception{
        String email="reset-"+UUID.randomUUID()+"@example.test";
        accounts.dangKy(new DangKyYeuCau("Reset user",email,password,password));
        var client=browser();post(client,"/login",Map.of("_csrf",csrf(get(client,"/login").body()),"username",email,"password",password));
        assertThat(get(client,"/tai-khoan").statusCode()).isEqualTo(200);
        accounts.yeuCauDatLai(email);String reset=token(email,"/dat-lai-mat-khau");
        String newPassword="ChangedDemo!2026";accounts.datLai(reset,newPassword,newPassword);
        var revoked=get(client,"/tai-khoan");assertThat(revoked.statusCode()).isEqualTo(302);assertThat(revoked.headers().firstValue("Location").orElse("")).contains("expired");
        assertThatThrownBy(()->accounts.datLai(reset,newPassword,newPassword)).isInstanceOf(IllegalArgumentException.class);
        var old=post(client,"/login",Map.of("_csrf",csrf(get(client,"/login").body()),"username",email,"password",password));assertThat(old.headers().firstValue("Location").orElse("")).contains("error");
        post(client,"/login",Map.of("_csrf",csrf(get(client,"/login").body()),"username",email,"password",newPassword));assertThat(get(client,"/tai-khoan").statusCode()).isEqualTo(200);
        post(client,"/logout",Map.of("_csrf",csrf(get(client,"/tai-khoan").body())));assertThat(get(client,"/tai-khoan").statusCode()).isEqualTo(302);
    }
}
