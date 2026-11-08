package vn.luma.web;

import java.io.IOException;
import java.security.Principal;
import java.time.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.NoiDungService;

@Controller
public class NoiDungController {
    private final NoiDungService service;
    public NoiDungController(NoiDungService service){this.service=service;}
    private LocalDateTime now(){return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).withSecond(0).withNano(0);}
    private LocalDateTime vn(Object value){return value==null?null:vn.luma.data.ThanhToanDAO.instant(value).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime();}
    @GetMapping("/su-kien") public String suKien(@RequestParam(defaultValue="1") int trang,Model m){m.addAttribute("suKien",service.suKien(trang));m.addAttribute("quanTri",false);m.addAttribute("trang",trang);return "noi-dung/su-kien";}
    @GetMapping("/su-kien/{slug}") public String suKien(@PathVariable String slug,Model m){m.addAttribute("suKien",service.suKien(slug));return "noi-dung/su-kien-chi-tiet";}
    @GetMapping("/quan-tri/su-kien") public String suKienQuanTri(@RequestParam(defaultValue="1") int trang,Principal u,Model m){m.addAttribute("suKien",service.suKien(u.getName(),trang));m.addAttribute("quanTri",true);m.addAttribute("trang",trang);return "noi-dung/su-kien";}
    @GetMapping({"/quan-tri/su-kien/moi","/quan-tri/su-kien/{id}"}) public String suaSuKien(@PathVariable(required=false) Long id,Principal u,Model m){service.viTri(u.getName());m.addAttribute("maSuKien",id);if(!m.containsAttribute("form")){if(id==null)m.addAttribute("form",new SuKienYeuCau(null,"","","","NHAP",now(),null));else{var e=service.suKien(u.getName(),id);m.addAttribute("form",new SuKienYeuCau((Long)e.get("MaChuongTrinh"),(String)e.get("TieuDe"),(String)e.get("DuongDan"),(String)e.get("NoiDung"),(String)e.get("TrangThai"),vn(e.get("NgayBatDau")),vn(e.get("NgayKetThuc"))));}}return "noi-dung/su-kien-form";}
    @PostMapping({"/quan-tri/su-kien/moi","/quan-tri/su-kien/{id}"}) public String luuSuKien(@PathVariable(required=false) Long id,@Valid @ModelAttribute("form") SuKienYeuCau f,BindingResult errors,Principal u,Model m,RedirectAttributes flash){if(!errors.hasErrors())try{long saved=service.luuSuKien(u.getName(),id,f);flash.addFlashAttribute("thongBao","Đã lưu nội dung và lịch sự kiện.");return "redirect:/quan-tri/su-kien/"+saved;}catch(IllegalArgumentException ex){m.addAttribute("loi",ex.getMessage());}return suaSuKien(id,u,m);}
    @GetMapping("/quan-tri/banner") public String banner(@RequestParam(defaultValue="1") int trang,Principal u,Model m){m.addAttribute("banner",service.banner(u.getName(),trang));m.addAttribute("trang",trang);return "noi-dung/banner";}
    @GetMapping({"/quan-tri/banner/moi","/quan-tri/banner/{id}"}) public String suaBanner(@PathVariable(required=false) Long id,Principal u,Model m){m.addAttribute("viTri",service.viTri(u.getName()));m.addAttribute("maBanner",id);if(!m.containsAttribute("form")){if(id==null)m.addAttribute("form",new BannerYeuCau(0,null,"","","",0,false,now(),null));else{var b=service.banner(u.getName(),id);m.addAttribute("form",new BannerYeuCau(((Number)b.get("MaViTriHienThi")).longValue(),(Long)b.get("MaSuKien"),(String)b.get("TieuDe"),(String)b.get("VanBanThayThe"),(String)b.get("DuongDanDich"),((Number)b.get("ThuTu")).intValue(),Boolean.TRUE.equals(b.get("DangHienThi")),vn(b.get("NgayBatDau")),vn(b.get("NgayKetThuc"))));}}return "noi-dung/banner-form";}
    @PostMapping({"/quan-tri/banner/moi","/quan-tri/banner/{id}"}) public String luuBanner(@PathVariable(required=false) Long id,@Valid @ModelAttribute("form") BannerYeuCau f,BindingResult errors,@RequestParam(required=false) MultipartFile tep,Principal u,Model m,RedirectAttributes flash)throws IOException{if(!errors.hasErrors())try{long saved=service.luuBanner(u.getName(),id,f,tep==null?null:tep.getBytes());flash.addFlashAttribute("thongBao","Đã lưu banner. Hiển thị còn phụ thuộc lịch và trạng thái sự kiện đích.");return "redirect:/quan-tri/banner/"+saved;}catch(IllegalArgumentException ex){m.addAttribute("loi",ex.getMessage());}return suaBanner(id,u,m);}
    private ResponseEntity<byte[]> anh(byte[] bytes,boolean png){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(png?MediaType.IMAGE_PNG:MediaType.IMAGE_JPEG).body(bytes);}
    @GetMapping("/anh-banner/{file}") public ResponseEntity<byte[]> anh(@PathVariable String file){return anh(service.anh(file),file.endsWith(".png"));}
    @GetMapping("/quan-tri/banner/{id}/anh") public ResponseEntity<byte[]> anhQuanTri(@PathVariable long id,Principal u){var b=service.banner(u.getName(),id);return anh(service.anh(u.getName(),id),((String)b.get("DuongDanAnh")).endsWith(".png"));}
}
