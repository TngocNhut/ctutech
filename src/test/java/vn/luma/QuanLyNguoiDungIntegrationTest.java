package vn.luma;

import java.net.*;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.business.*;
import vn.luma.data.TaiKhoanDAO;
import vn.luma.web.NguoiDungYeuCau;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false"})
class QuanLyNguoiDungIntegrationTest {
    static final String ADMIN="admin@luma.local",PASSWORD="StrongDemo!2026";
    @Autowired QuanLyNguoiDungService admin;
    @Autowired TaiKhoanService users;
    @Autowired TaiKhoanDAO accounts;
    @Autowired JdbcClient jdbc;
    @LocalServerPort int port;
    String user(){String email="user-admin-"+UUID.randomUUID()+"@example.test";users.seedDemo(email,"Khách quản trị thử","KhachHang",PASSWORD);return email;}
    @Test @Transactional void roleOrStatusChangeRevokesSessionsAndWritesPrivateAuditReason(){
        String email=user();long id=users.hienTai(email).maNguoiDung();users.ghiPhien(email,"test-session");assertThat(users.phienHopLe(email,"test-session")).isTrue();
        admin.luu(ADMIN,id,new NguoiDungYeuCau("QuanTri","HOAT_DONG","Cấp quyền thử"));assertThat(users.hienTai(email).vaiTro()).isEqualTo("QuanTri");assertThat(users.phienHopLe(email,"test-session")).isFalse();
        assertThat(jdbc.sql("SELECT \"GhiChu\" FROM \"NhatKyQuanTri\" WHERE \"MaDoiTuong\"=:id AND \"HanhDong\"='SUA_NGUOI_DUNG' ORDER BY \"MaNhatKy\" DESC LIMIT 1").param("id",Long.toString(id)).query(String.class).single()).isEqualTo("Cấp quyền thử");
        users.ghiPhien(email,"second-session");admin.luu(ADMIN,id,new NguoiDungYeuCau("KhachHang","BI_KHOA","Khóa thử"));assertThat(users.phienHopLe(email,"second-session")).isFalse();
        assertThatThrownBy(()->users.ghiPhien(email,"new-session")).isInstanceOf(IllegalArgumentException.class);
        admin.luu(ADMIN,id,new NguoiDungYeuCau("KhachHang","HOAT_DONG","Mở lại"));assertThat(users.phienHopLe(email,"test-session")).isFalse();
    }
    @Test @Transactional void rememberedSessionHasLongerExpiryAndCanBeRevoked(){
        String email=user();users.ghiPhien(email,"remember-session",true);users.ghiPhien(email,"ordinary-session");
        long remembered=jdbc.sql("SELECT extract(epoch FROM (\"NgayHetHan\"-now()))::bigint FROM \"PhienDangNhap\" WHERE \"MaPhienBam\"=:hash").param("hash",TaiKhoanService.bam("remember-session")).query(Long.class).single();
        long ordinary=jdbc.sql("SELECT extract(epoch FROM (\"NgayHetHan\"-now()))::bigint FROM \"PhienDangNhap\" WHERE \"MaPhienBam\"=:hash").param("hash",TaiKhoanService.bam("ordinary-session")).query(Long.class).single();
        assertThat(remembered).isGreaterThan(29L*24*3600);assertThat(ordinary).isLessThanOrEqualTo(8L*3600);
        assertThat(users.phienHopLe(email,"remember-session")).isTrue();users.thuHoiPhien("remember-session");assertThat(users.phienHopLe(email,"remember-session")).isFalse();
    }
    @Test @Transactional void guardsSelfChangesUnverifiedPromotionAndCustomerAccess(){
        String email=user();long actor=users.hienTai(ADMIN).maNguoiDung();
        assertThatThrownBy(()->admin.luu(ADMIN,actor,new NguoiDungYeuCau("KhachHang","HOAT_DONG","Tự hạ quyền"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->admin.luu(ADMIN,actor,new NguoiDungYeuCau("QuanTri","BI_KHOA","Tự khóa"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->admin.danhSach(email,"",1)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        long unverified=accounts.tao("Chưa xác minh","unverified-"+UUID.randomUUID()+"@example.test","unused-test-hash","KhachHang",false);
        assertThatThrownBy(()->admin.luu(ADMIN,unverified,new NguoiDungYeuCau("QuanTri","HOAT_DONG","Thử cấp"))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("xác minh");
        assertThat(admin.danhSach(ADMIN,email,1)).hasSize(1);assertThat(admin.dem(ADMIN,email)).isEqualTo(1);
    }
    @Test void revokedBrowserSessionAndLoginThrottleAreEnforcedOverHttp()throws Exception{
        String email=user();long id=users.hienTai(email).maNguoiDung();var browser=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String html=get(browser,"/login").body();var csrf=java.util.regex.Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);assertThat(csrf.find()).isTrue();
        assertThat(login(browser,email,PASSWORD,csrf.group(1)).statusCode()).isEqualTo(302);assertThat(get(browser,"/tai-khoan").statusCode()).isEqualTo(200);
        admin.thuHoi(ADMIN,id);assertThat(get(browser,"/tai-khoan").headers().firstValue("Location").orElse("")).endsWith("/login?expired");
        html=get(browser,"/login").body();csrf=java.util.regex.Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);assertThat(csrf.find()).isTrue();String token=csrf.group(1);
        String unknown="no-account-"+UUID.randomUUID()+"@example.test";
        for(int i=0;i<10;i++)assertThat(login(browser,unknown,"incorrect",token).statusCode()).isEqualTo(302);
        var limited=login(browser,unknown,"incorrect",token);assertThat(limited.statusCode()).isEqualTo(429);assertThat(limited.headers().firstValue("Retry-After")).isPresent();assertThat(limited.body()).contains("Đã thử đăng nhập quá nhiều lần");
        assertThat(jdbc.sql("SELECT count(*) FROM \"NhatKyBaoMat\" WHERE \"LoaiSuKien\"='DANG_NHAP' AND \"KetQua\"='BI_CHAN' AND \"MaNguoiDung\" IS NULL").query(Long.class).single()).isGreaterThan(0);
    }
    HttpResponse<String> get(HttpClient client,String path)throws Exception{return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
    HttpResponse<String> login(HttpClient client,String email,String password,String csrf)throws Exception{
        String body="username="+URLEncoder.encode(email,java.nio.charset.StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(password,java.nio.charset.StandardCharsets.UTF_8)+"&_csrf="+URLEncoder.encode(csrf,java.nio.charset.StandardCharsets.UTF_8);
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/login")).header("Accept","text/html").header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
}
