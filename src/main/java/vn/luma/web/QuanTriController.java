package vn.luma.web;

import java.security.Principal;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.KhoService;

@Controller
public class QuanTriController {
    private final KhoService service;
    public QuanTriController(KhoService service){this.service=service;}
    @GetMapping("/quan-tri/san-pham")
    public String danhSach(@RequestParam(defaultValue="1") int trang,@RequestParam(defaultValue="") String q,Principal user,Model model){model.addAttribute("trang",trang);model.addAttribute("q",q);model.addAttribute("bienThe",service.danhSach(user.getName(),q,trang));return "quan-tri/san-pham";}
    @GetMapping("/quan-tri/bien-the/{id}")
    public String sua(@PathVariable long id,Principal user,Model model){
        var sku=service.sku(user.getName(),id);
        model.addAttribute("sku",sku);model.addAttribute("form",new CapNhatBienThe(sku.giaBan(),sku.giaNiemYet(),sku.giaVon(),sku.soLuongTon(),""));return "quan-tri/bien-the";
    }
    @PostMapping("/quan-tri/bien-the/{id}")
    public String luu(@PathVariable long id,@Valid @ModelAttribute("form") CapNhatBienThe form,BindingResult errors,Principal user,Model model,RedirectAttributes flash){
        if(!errors.hasErrors())try{service.capNhat(user.getName(),id,form);flash.addFlashAttribute("thongBao","Đã cập nhật giá/tồn và ghi nhật ký.");return "redirect:/quan-tri/san-pham";}catch(IllegalArgumentException ex){model.addAttribute("loi",ex.getMessage());}
        model.addAttribute("sku",service.sku(user.getName(),id));return "quan-tri/bien-the";
    }
}
