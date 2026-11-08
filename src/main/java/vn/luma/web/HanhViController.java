package vn.luma.web;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.luma.business.*;

@Controller
public class HanhViController {
    public static final String CONSENT="choPhepThongKe",SESSION="phienThongKe";
    public record Phien(long id,String key,String owner,Instant expires) { }
    public record SuKien(String loai,String nhom,String khoa,Long sku,Long banner) {public SuKien(String loai,String nhom,String khoa,Long sku){this(loai,nhom,khoa,sku,null);}}
    public record Nhip(boolean hoatDong) { }
    private final HanhViService service;private final TaiKhoanService users;
    public HanhViController(HanhViService service,TaiKhoanService users){this.service=service;this.users=users;}
    private Phien phien(HttpSession session,Principal u){if(!Boolean.TRUE.equals(session.getAttribute(CONSENT)))return null;String owner=u==null?"":u.getName();if(u!=null&&java.util.Set.of("QuanTri","NhanVien").contains(users.hienTai(owner).vaiTro()))return null;synchronized(session){var old=session.getAttribute(SESSION);if(old instanceof Phien p&&p.owner().equals(owner)&&p.expires().isAfter(Instant.now()))return p;String key=UUID.randomUUID().toString();var p=new Phien(service.tao(key,u==null?null:owner),key,owner,Instant.now().plusSeconds(28800));session.setAttribute(SESSION,p);return p;}}
    @GetMapping("/quyen-rieng-tu") public String privacy(HttpSession session,Model m){m.addAttribute("choPhepThongKe",Boolean.TRUE.equals(session.getAttribute(CONSENT)));return "quyen-rieng-tu";}
    @PostMapping("/thong-ke/lua-chon") public String luaChon(@RequestParam(defaultValue="false") boolean choPhep,HttpSession session){session.setAttribute(CONSENT,choPhep);if(!choPhep)session.removeAttribute(SESSION);return "redirect:/quyen-rieng-tu";}
    @PostMapping("/thong-ke/su-kien") @ResponseBody public void suKien(@RequestBody SuKien event,HttpSession session,Principal u){var p=phien(session,u);if(p!=null)service.suKien(p.id(),p.key(),event.loai(),event.nhom(),event.khoa(),event.sku(),event.banner());}
    @PostMapping("/thong-ke/nhip") @ResponseBody public void nhip(@RequestBody Nhip value,HttpSession session,Principal u){var p=phien(session,u);if(p!=null)service.nhip(p.id(),p.key(),value.hoatDong());}
    public void theoDoi(HttpSession session,Principal u,long product){try{var p=phien(session,u);if(p!=null)service.theoDoi(p.id(),p.key(),product);}catch(RuntimeException ex){org.slf4j.LoggerFactory.getLogger(getClass()).debug("Bỏ qua thống kê theo dõi sau lỗi {}",ex.getClass().getSimpleName());}}
    public void themGio(HttpSession session,Principal u,long sku){try{var p=phien(session,u);if(p!=null)service.themGio(p.id(),p.key(),sku);}catch(RuntimeException ex){org.slf4j.LoggerFactory.getLogger(getClass()).debug("Bỏ qua thống kê giỏ sau lỗi {}",ex.getClass().getSimpleName());}}
}
