package vn.luma.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import vn.luma.business.TaiKhoanService;

@Configuration
public class DuLieuDemoConfig {
    @Bean
    @ConditionalOnProperty(name="app.demo.enabled",havingValue="true",matchIfMissing=true)
    ApplicationRunner taoTaiKhoanDemo(TaiKhoanService service,@Value("${app.demo.password:LumaDemo!2026}") String password) {
        return args->{
            service.kiemTraMatKhau(password);
            service.seedDemo("admin@luma.local","Quản trị LUMA","QuanTri",password);
            service.seedDemo("khach@luma.local","Khách trải nghiệm","KhachHang",password);
        };
    }
}
