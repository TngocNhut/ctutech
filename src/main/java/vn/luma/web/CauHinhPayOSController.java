package vn.luma.web;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.CauHinhPayOSService;
@Controller @RequestMapping("/quan-tri/payos")
public class CauHinhPayOSController {
 private final CauHinhPayOSService service;
 public CauHinhPayOSController(CauHinhPayOSService service){this.service=service;}
 @GetMapping public String xem(Principal user,Model model){model.addAttribute("payos",service.trangThai(user.getName()));return "quan-tri/payos";}
 @PostMapping public String luu(Principal user,@RequestParam(defaultValue="false") boolean bat,@RequestParam String publicUrl,@RequestParam(defaultValue="") String clientId,@RequestParam(defaultValue="") String apiKey,@RequestParam(defaultValue="") String checksumKey,RedirectAttributes flash){try{boolean restart=service.luu(user.getName(),bat,publicUrl,clientId,apiKey,checksumKey);flash.addFlashAttribute("thongBao",restart?"Đã lưu cấu hình. Khởi động lại website để áp dụng.":"Đã lưu cấu hình PayOS đã mã hóa vào cơ sở dữ liệu và áp dụng ngay.");}catch(IllegalArgumentException|IllegalStateException ex){flash.addFlashAttribute("loi",ex.getMessage());}return "redirect:/quan-tri/payos";}
}
