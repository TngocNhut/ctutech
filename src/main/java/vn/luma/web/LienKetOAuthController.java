package vn.luma.web;
import java.security.Principal;
import java.time.Instant;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.LienKetOAuthService;
import vn.luma.config.OAuthCauHinh;

@Controller
public class LienKetOAuthController {
    private final LienKetOAuthService service;private final OAuthCauHinh config;
    public LienKetOAuthController(LienKetOAuthService service,OAuthCauHinh config){this.service=service;this.config=config;}
    public static LienKetOAuthService.DanhTinh pending(HttpSession session){var p=session.getAttribute(LienKetOAuthService.PENDING);if(p instanceof LienKetOAuthService.DanhTinh d&&d.conHan())return d;session.removeAttribute(LienKetOAuthService.PENDING);return null;}
    @GetMapping("/tai-khoan/lien-ket") public String xem(Principal u,HttpSession session,Model m){m.addAttribute("lienKet",service.cuaToi(u.getName()));m.addAttribute("choLienKet",pending(session));m.addAttribute("nhaCungCapOAuth",config.nhaCungCap());return "tai-khoan/lien-ket";}
    private record ThuLai(int soLan,Instant hetHan) { }
    private void gioiHan(HttpSession session){var old=session.getAttribute("thuMatKhauLienKet");if(old instanceof ThuLai t&&t.hetHan().isAfter(Instant.now())&&t.soLan()>=5)throw new IllegalArgumentException("Đã thử xác nhận 5 lần. Chờ 15 phút rồi thử lại.");}
    private void thatBai(HttpSession session){var old=session.getAttribute("thuMatKhauLienKet");var t=old instanceof ThuLai r&&r.hetHan().isAfter(Instant.now())?r:new ThuLai(0,Instant.now().plusSeconds(900));session.setAttribute("thuMatKhauLienKet",new ThuLai(t.soLan()+1,t.hetHan()));}
    @PostMapping("/tai-khoan/lien-ket/xac-nhan") public String gan(@RequestParam String matKhau,Principal u,HttpSession session,RedirectAttributes flash){synchronized(session){try{gioiHan(session);service.gan(u.getName(),matKhau,pending(session));session.removeAttribute(LienKetOAuthService.PENDING);session.removeAttribute("thuMatKhauLienKet");flash.addFlashAttribute("thongBao","Đã liên kết. Lần sau bạn có thể đăng nhập bằng nhà cung cấp này.");}catch(IllegalArgumentException ex){thatBai(session);flash.addFlashAttribute("loi",ex.getMessage());}}return "redirect:/tai-khoan/lien-ket";}
    @PostMapping("/tai-khoan/lien-ket/bo") public String bo(@RequestParam String nhaCungCap,@RequestParam String matKhau,Principal u,HttpSession session,RedirectAttributes flash){synchronized(session){try{gioiHan(session);service.bo(u.getName(),matKhau,nhaCungCap);session.removeAttribute("thuMatKhauLienKet");flash.addFlashAttribute("thongBao","Đã bỏ liên kết. Bạn vẫn có thể đăng nhập bằng email và mật khẩu.");}catch(IllegalArgumentException ex){thatBai(session);flash.addFlashAttribute("loi",ex.getMessage());}}return "redirect:/tai-khoan/lien-ket";}
    @PostMapping("/tai-khoan/lien-ket/huy") public String huy(HttpSession session){session.removeAttribute(LienKetOAuthService.PENDING);return "redirect:/tai-khoan/lien-ket";}
}
