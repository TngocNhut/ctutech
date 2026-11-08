package vn.luma.web;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.*;

@Controller
public class ThongBaoController {
    private final ThongBaoService service;private final KhuyenMaiService offers;
    public ThongBaoController(ThongBaoService service,KhuyenMaiService offers){this.service=service;this.offers=offers;}
    @GetMapping("/tai-khoan/thong-bao/chua-doc") @ResponseBody public java.util.Map<String,Long> dem(Principal u){return java.util.Map.of("soChuaDoc",service.chuaDoc(u.getName()));}
    @GetMapping("/tai-khoan/thong-bao") public String xem(@RequestParam(defaultValue="false") boolean chuaDoc,@RequestParam(defaultValue="1") int trang,Principal u,Model m){m.addAttribute("thongBaoTrongTaiKhoan",service.danhSach(u.getName(),chuaDoc,trang));m.addAttribute("soChuaDoc",service.chuaDoc(u.getName()));m.addAttribute("chuaDoc",chuaDoc);m.addAttribute("trang",trang);return "tai-khoan/thong-bao";}
    @PostMapping("/tai-khoan/thong-bao/{id}/mo") public String mo(@PathVariable long id,Principal u){return "redirect:"+service.doc(u.getName(),id);}
    @PostMapping("/tai-khoan/thong-bao/doc-tat-ca") public String docTatCa(Principal u,RedirectAttributes flash){service.docTatCa(u.getName());flash.addFlashAttribute("thongBao","Đã đánh dấu các thông báo hiện có là đã đọc.");return "redirect:/tai-khoan/thong-bao";}
    @PostMapping("/tai-khoan/uu-dai/{id}/luu") public String luuUuDai(@PathVariable long id,Principal u,RedirectAttributes flash){try{offers.luuThongBao(u.getName(),id);flash.addFlashAttribute("thongBao","Đã lưu ưu đãi trong thông báo. Thao tác này không giữ lượt mã; điều kiện được kiểm tra lại khi đặt đơn.");}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}return "redirect:/tai-khoan/thong-bao";}
}
