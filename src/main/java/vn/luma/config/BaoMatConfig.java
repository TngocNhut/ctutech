package vn.luma.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import vn.luma.business.TaiKhoanService;
import vn.luma.business.GioHangService;
import vn.luma.web.GioHangController;

@Configuration
public class BaoMatConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService userDetailsService(JdbcClient jdbc) {
        return email -> jdbc.sql("""
            SELECT coalesce(n."ThuDienTu",n."SoDienThoai") AS "TaiKhoan", n."MatKhauBam", v."TenVaiTro", n."TrangThai"
            FROM "NguoiDung" n JOIN "VaiTro" v USING ("MaVaiTro")
            WHERE n."ThuDienTu" = :email AND n."MatKhauBam" IS NOT NULL
            """).param("email", TaiKhoanService.emailChuan(email))
            .query((r, i) -> User.withUsername(r.getString("TaiKhoan"))
                .password(r.getString("MatKhauBam")).roles(r.getString("TenVaiTro"))
                .disabled(!"HOAT_DONG".equals(r.getString("TrangThai"))).build())
            .optional().orElseThrow(() -> new UsernameNotFoundException("Tài khoản không khả dụng"));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,TaiKhoanService accounts,GioHangService carts,vn.luma.business.LienKetOAuthService identities,org.springframework.beans.factory.ObjectProvider<org.springframework.security.oauth2.client.registration.ClientRegistrationRepository> registrations,org.springframework.beans.factory.ObjectProvider<org.springframework.security.oauth2.client.OAuth2AuthorizedClientService> clients) throws Exception {
        var rateLimit=new GioiHanDangNhapFilter(accounts);
        var success=new DangNhapThanhCong(accounts,carts,rateLimit);
        if(registrations.getIfAvailable()!=null)http.oauth2Login(o->o.loginPage("/login").successHandler(new OAuthThanhCong(identities,accounts,success,clients.getIfAvailable())).failureHandler((req,res,ex)->{req.getSession().removeAttribute(vn.luma.business.LienKetOAuthService.PENDING);res.sendRedirect("/login?oauthError");}));
        return http.authorizeHttpRequests(a -> a
                .requestMatchers("/oauth2/authorization/google","/login/oauth2/code/google","/xac-nhan-dang-ky/**","/xac-nhan-dang-ky","/xac-minh-email","/tai-khoan/gui-xac-minh").denyAll()
                .requestMatchers("/quan-tri/model/**","/quan-tri/bien-the/**","/quan-tri/san-pham/**","/quan-tri/danh-muc/**","/quan-tri/thuoc-tinh/**","/quan-tri/dong-san-pham/**","/quan-tri/don-hang/**","/quan-tri/giao-nhan/**").hasAnyRole("QuanTri","NhanVien")
                .requestMatchers("/quan-tri/ho-tro/*/kiem-duyet/**").hasRole("QuanTri")
                .requestMatchers("/quan-tri/ho-tro/**").hasAnyRole("QuanTri","NhanVien")
                .requestMatchers("/quan-tri/**").hasRole("QuanTri")
                .requestMatchers("/tai-khoan/**", "/dat-hang/**", "/don-hang/**", "/thanh-toan/**","/gio-hang/chua-gop/**").authenticated()
                .anyRequest().permitAll())
            .csrf(c -> c.ignoringRequestMatchers("/api/payos/webhook"))
            .formLogin(f -> f.loginPage("/login").successHandler(success).failureHandler((req,res,ex)->{accounts.ghiDangNhapLoi(java.util.Objects.toString(req.getParameter("username"),""),false);res.sendRedirect("/login?error");}).permitAll())
            .logout(l -> l.addLogoutHandler((req,res,auth)->{
                var session=req.getSession(false);
                if(session!=null) accounts.thuHoiPhien(session.getId());
            }).logoutSuccessUrl("/").deleteCookies("JSESSIONID"))
            .addFilterAfter(new KiemTraPhienFilter(accounts),AnonymousAuthenticationFilter.class)
            .addFilterBefore(new GoogleTamTatFilter(),org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter.class)
            .addFilterBefore(new GioiHanDangKyFilter(),org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimit,org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
