package vn.luma.web;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.DoiSoatService;

@Controller
@RequestMapping("/quan-tri/doi-soat")
public class DoiSoatController {
    private final DoiSoatService service;
    public DoiSoatController(DoiSoatService service){this.service=service;}
    @GetMapping public String danhSach(@RequestParam(defaultValue="CAN_KIEM_TRA") String trangThai,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="1") int trang,Principal u,Model m){m.addAttribute("phien",service.danhSach(u.getName(),trangThai,q,trang));m.addAttribute("tong",service.dem(u.getName(),trangThai,q));m.addAttribute("trangThai",trangThai);m.addAttribute("q",q);m.addAttribute("trang",trang);return "quan-tri/doi-soat";}
    @GetMapping("/{id}") public String chiTiet(@PathVariable long id,@RequestParam(defaultValue="1") int suKien,@RequestParam(defaultValue="1") int lichSu,Principal u,Model m){m.addAttribute("doiSoat",service.chiTiet(u.getName(),id,suKien,lichSu));m.addAttribute("trangSuKien",suKien);m.addAttribute("trangLichSu",lichSu);return "quan-tri/doi-soat-chi-tiet";}
    @PostMapping("/{id}/kiem-tra") public String kiemTra(@PathVariable long id,Principal u,RedirectAttributes flash){try{service.kiemTra(u.getName(),id);flash.addFlashAttribute("thongBao","Đã kiểm tra trạng thái hiện có. Các lượt gần nhau dùng kết quả mới nhất; thử lại sau vài giây khi cần.");}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}return "redirect:/quan-tri/doi-soat/"+id;}
    @GetMapping("/su-kien-chua-gan") public String chuaGan(@RequestParam(defaultValue="1") int trang,Principal u,Model m){m.addAttribute("suKien",service.chuaGan(u.getName(),trang));m.addAttribute("trang",trang);return "quan-tri/su-kien-chua-gan";}
    @PostMapping("/{id}/ghi-chu") public String ghiChu(@PathVariable long id,@RequestParam String ghiChu,Principal u,RedirectAttributes flash){try{service.ghiChu(u.getName(),id,ghiChu);flash.addFlashAttribute("thongBao","Đã ghi chú vào lịch sử đối soát. Số tiền và cờ kiểm tra được giữ nguyên.");}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());flash.addFlashAttribute("ghiChuNhap",ghiChu);}return "redirect:/quan-tri/doi-soat/"+id;}
}
