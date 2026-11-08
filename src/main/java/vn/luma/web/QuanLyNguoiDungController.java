package vn.luma.web;

import java.security.Principal;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.QuanLyNguoiDungService;

@Controller
@RequestMapping("/quan-tri")
public class QuanLyNguoiDungController {
    private final QuanLyNguoiDungService service;
    public QuanLyNguoiDungController(QuanLyNguoiDungService service){this.service=service;}
    @GetMapping("/nguoi-dung") public String danhSach(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="1") int trang,Principal u,Model m){m.addAttribute("nguoiDung",service.danhSach(u.getName(),q,trang));m.addAttribute("tong",service.dem(u.getName(),q));m.addAttribute("q",q);m.addAttribute("trang",trang);return "quan-tri/nguoi-dung";}
    @GetMapping("/nguoi-dung/{id}") public String sua(@PathVariable long id,Principal u,Model m){var user=service.chiTiet(u.getName(),id);m.addAttribute("nguoi",user);if(!m.containsAttribute("form"))m.addAttribute("form",new NguoiDungYeuCau((String)user.get("TenVaiTro"),(String)user.get("TrangThai"),""));return "quan-tri/nguoi-dung-form";}
    @PostMapping("/nguoi-dung/{id}") public String luu(@PathVariable long id,@Valid @ModelAttribute("form") NguoiDungYeuCau f,BindingResult errors,Principal u,Model m,RedirectAttributes flash){if(!errors.hasErrors())try{service.luu(u.getName(),id,f);flash.addFlashAttribute("thongBao","Đã lưu quyền và trạng thái. Khi có thay đổi, các phiên đăng nhập cũ đã bị thu hồi.");return "redirect:/quan-tri/nguoi-dung/"+id;}catch(IllegalArgumentException ex){m.addAttribute("loi",ex.getMessage());}return sua(id,u,m);}
    @PostMapping("/nguoi-dung/{id}/thu-hoi-phien") public String thuHoi(@PathVariable long id,Principal u,RedirectAttributes flash){service.thuHoi(u.getName(),id);flash.addFlashAttribute("thongBao","Đã thu hồi tất cả phiên của tài khoản này.");return "redirect:/quan-tri/nguoi-dung/"+id;}
    @GetMapping("/nhat-ky/{type}") public String nhatKy(@PathVariable String type,@RequestParam(defaultValue="1") int trang,Principal u,Model m){if(!java.util.Set.of("bao-mat","quan-tri").contains(type))throw new vn.luma.business.KhongTimThayException("Không có loại nhật ký này.");m.addAttribute("nhatKy",service.nhatKy(u.getName(),type.equals("bao-mat"),trang));m.addAttribute("loai",type);m.addAttribute("trang",trang);return "quan-tri/nhat-ky";}
}
