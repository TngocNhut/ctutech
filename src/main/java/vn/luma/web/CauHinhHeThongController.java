package vn.luma.web;

import java.security.Principal;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.CauHinhHeThongService;

@Controller
@RequestMapping("/quan-tri/cau-hinh")
public class CauHinhHeThongController {
    private final CauHinhHeThongService service;
    public CauHinhHeThongController(CauHinhHeThongService service){this.service=service;}
    @GetMapping public String xem(Principal u,Model m){var settings=service.quanTri(u.getName());if(!m.containsAttribute("form"))m.addAttribute("form",settings);return "quan-tri/cau-hinh";}
    @PostMapping public String luu(@Valid @ModelAttribute("form") CauHinhHeThongYeuCau f,BindingResult errors,Principal u,Model m,RedirectAttributes flash){if(!errors.hasErrors())try{service.luu(u.getName(),f);flash.addFlashAttribute("thongBao","Đã lưu cấu hình. Đơn đã đặt giữ giá và phí cũ.");return "redirect:/quan-tri/cau-hinh";}catch(IllegalArgumentException ex){m.addAttribute("loi",ex.getMessage());}return xem(u,m);}
}
