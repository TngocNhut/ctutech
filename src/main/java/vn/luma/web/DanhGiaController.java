package vn.luma.web;

import java.security.Principal;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.DanhGiaService;

@Controller
public class DanhGiaController {
    private final DanhGiaService service;
    public DanhGiaController(DanhGiaService service){this.service=service;}
    @GetMapping("/san-pham/{slug}/danh-gia") public String congKhai(@PathVariable String slug,@RequestParam(defaultValue="1") int trang,Principal u,Model m){var result=service.congKhai(slug,trang);m.addAttribute("ketQua",result);m.addAttribute("coTheDanhGia",u!=null&&service.coTheDanhGia(u.getName(),result.sanPham().maSanPham()));m.addAttribute("daDangNhap",u!=null);return "san-pham/danh-gia";}
    @GetMapping("/tai-khoan/danh-gia") public String cuaToi(@RequestParam(defaultValue="1") int trang,Principal u,Model m){m.addAttribute("daMua",service.daMua(u.getName(),trang));m.addAttribute("trang",trang);return "tai-khoan/danh-gia";}
    @GetMapping("/tai-khoan/danh-gia/{id}") public String sua(@PathVariable long id,Principal u,Model m){var draft=service.banSoan(u.getName(),id);m.addAttribute("banSoan",draft);m.addAttribute("maSanPham",id);if(!m.containsAttribute("form")){var r=draft.danhGia();m.addAttribute("form",r==null?new DanhGiaYeuCau(5,"",""):new DanhGiaYeuCau(((Number)r.get("SoSao")).intValue(),(String)r.get("TieuDe"),(String)r.get("NoiDung")));}return "tai-khoan/danh-gia-form";}
    @PostMapping("/tai-khoan/danh-gia/{id}") public String luu(@PathVariable long id,@Valid @ModelAttribute("form") DanhGiaYeuCau f,BindingResult errors,Principal u,Model m,RedirectAttributes flash){if(!errors.hasErrors())try{service.luu(u.getName(),id,f);flash.addFlashAttribute("thongBao","Đã lưu đánh giá. Nếu đánh giá đang bị ẩn, trạng thái đó được giữ để quản trị kiểm tra lại.");return "redirect:/tai-khoan/danh-gia/"+id;}catch(IllegalArgumentException ex){m.addAttribute("loi",ex.getMessage());}return sua(id,u,m);}
    @GetMapping("/quan-tri/danh-gia") public String quanTri(@RequestParam(defaultValue="TAT_CA") String trangThai,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="1") int trang,Principal u,Model m){m.addAttribute("danhGia",service.quanTri(u.getName(),trangThai,q,trang));m.addAttribute("tong",service.dem(u.getName(),trangThai,q));m.addAttribute("q",q);m.addAttribute("trangThai",trangThai);m.addAttribute("trang",trang);return "quan-tri/danh-gia";}
    @GetMapping("/quan-tri/danh-gia/{id}") public String kiemDuyet(@PathVariable long id,Principal u,Model m){var r=service.chiTiet(u.getName(),id);m.addAttribute("danhGia",r);if(!m.containsAttribute("form"))m.addAttribute("form",new KiemDuyetDanhGiaYeuCau("HIEN_THI".equals(r.get("TrangThai")),(String)r.get("PhanHoiQuanTri"),""));return "quan-tri/danh-gia-form";}
    @PostMapping("/quan-tri/danh-gia/{id}") public String duyet(@PathVariable long id,@Valid @ModelAttribute("form") KiemDuyetDanhGiaYeuCau f,BindingResult errors,Principal u,Model m,RedirectAttributes flash){if(!errors.hasErrors())try{service.duyet(u.getName(),id,f);flash.addFlashAttribute("thongBao","Đã lưu phản hồi và trạng thái hiển thị.");return "redirect:/quan-tri/danh-gia/"+id;}catch(IllegalArgumentException ex){m.addAttribute("loi",ex.getMessage());}return kiemDuyet(id,u,m);}
}
