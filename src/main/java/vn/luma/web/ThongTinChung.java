package vn.luma.web;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
@ControllerAdvice
public class ThongTinChung {
 @Value("${app.support.email:}") private String supportEmail;
 @ModelAttribute("daDangNhap") public boolean daDangNhap(Principal user){return user!=null;}
 @ModelAttribute("emailHoTro") public String emailHoTro(){return supportEmail;}
}
