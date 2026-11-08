package vn.luma.web;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.TheoDoiService;

@Controller
@RequestMapping("/tai-khoan/theo-doi")
public class TheoDoiController {
    private final TheoDoiService service;private final HanhViController analytics;
    public TheoDoiController(TheoDoiService service,HanhViController analytics){this.service=service;this.analytics=analytics;}
    @GetMapping public String xem(@RequestParam(defaultValue="1") int trang,Principal u,Model m){m.addAttribute("theoDoi",service.danhSach(u.getName(),trang));m.addAttribute("trang",trang);return "tai-khoan/theo-doi";}
    @PostMapping("/{id}") public String luu(@PathVariable long id,@RequestParam(defaultValue="false") boolean baoGia,@RequestParam(defaultValue="false") boolean baoHang,@RequestParam(required=false) Long sku,jakarta.servlet.http.HttpSession session,Principal u,RedirectAttributes flash){try{service.luu(u.getName(),id,baoGia,baoHang,sku);analytics.theoDoi(session,u,id);flash.addFlashAttribute("thongBao","Đã lưu sản phẩm yêu thích và lựa chọn thông báo.");}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}return "redirect:/tai-khoan/theo-doi";}
    @PostMapping("/{id}/bo") public String bo(@PathVariable long id,Principal u,RedirectAttributes flash){service.xoa(u.getName(),id);flash.addFlashAttribute("thongBao","Đã bỏ theo dõi. Các thông báo trước đây được giữ trong tài khoản.");return "redirect:/tai-khoan/theo-doi";}
    @PostMapping("/kiem-tra") public String kiemTra(Principal u,RedirectAttributes flash){service.kiemTraCuaToi(u.getName());flash.addFlashAttribute("thongBao","Đã kiểm tra sản phẩm theo dõi. Mở thông báo để xem thay đổi mới đáp ứng lựa chọn của bạn.");return "redirect:/tai-khoan/theo-doi";}
}
