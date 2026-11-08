package vn.luma.web;
import java.security.Principal;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.QuanLyNguoiDungService;
@Controller @RequestMapping("/quan-tri/nhan-vien")
public class NhanVienController {
 private final QuanLyNguoiDungService service;
 public NhanVienController(QuanLyNguoiDungService service){this.service=service;}
 @GetMapping public String xem(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="1") int trang,Principal u,Model m){m.addAttribute("nhanVien",service.nhanVien(u.getName(),q,trang));m.addAttribute("tong",service.demNhanVien(u.getName(),q));m.addAttribute("q",q);m.addAttribute("trang",trang);if(!m.containsAttribute("form"))m.addAttribute("form",new DangKyYeuCau("","","",""));return "quan-tri/nhan-vien";}
 @PostMapping public String tao(@Valid @ModelAttribute("form") DangKyYeuCau form,BindingResult errors,Principal u,Model m,RedirectAttributes flash){if(!errors.hasErrors())try{long id=service.taoNhanVien(u.getName(),form);flash.addFlashAttribute("thongBao","Đã tạo nhân viên. Nhân viên có thể đăng nhập bằng email và mật khẩu ban đầu.");return "redirect:/quan-tri/nguoi-dung/"+id;}catch(IllegalArgumentException ex){m.addAttribute("loi",ex.getMessage());}m.addAttribute("form",new DangKyYeuCau(form.hoTen(),form.email(),"",""));return xem("",1,u,m);}
}
