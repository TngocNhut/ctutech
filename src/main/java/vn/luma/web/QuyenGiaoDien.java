package vn.luma.web;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
@ControllerAdvice
public class QuyenGiaoDien {
 @ModelAttribute("laQuanTri") public boolean laQuanTri(Authentication auth){return auth!=null&&auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_QuanTri"));}
 @ModelAttribute("laVanHanh") public boolean laVanHanh(Authentication auth){return auth!=null&&auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_QuanTri")||a.getAuthority().equals("ROLE_NhanVien"));}
}
