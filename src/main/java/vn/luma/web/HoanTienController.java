package vn.luma.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.HoanTienService;

@Controller
public class HoanTienController {
    private final HoanTienService service;
    public HoanTienController(HoanTienService service){this.service=service;}
    @GetMapping("/quan-tri/hoan-tien") public String danhSach(@RequestParam(defaultValue="1") int trang,Principal user,Model model){model.addAttribute("trang",trang);model.addAttribute("khoanHoan",service.danhSach(user.getName(),trang));return "quan-tri/hoan-tien";}
    @GetMapping("/quan-tri/hoan-tien/{id}") public String chiTiet(@PathVariable long id,Principal user,Model model){var row=service.chiTiet(user.getName(),id);model.addAttribute("hoan",row);if(!model.containsAttribute("form"))model.addAttribute("form",new XacNhanHoanYeuCau((BigDecimal)row.get("SoTienHoan"),"","","","",false));return "quan-tri/chi-tiet-hoan";}
    @PostMapping("/quan-tri/hoan-tien/{id}/xac-nhan") public String xacNhan(@PathVariable long id,@Valid @ModelAttribute("form") XacNhanHoanYeuCau form,BindingResult errors,@RequestParam MultipartFile chungTu,Principal user,Model model,RedirectAttributes flash)throws IOException{
        if(!errors.hasErrors())try{service.xacNhan(user.getName(),id,form,chungTu.getBytes());flash.addFlashAttribute("thongBao","Đã ghi nhận hoàn tiền theo chứng từ. Không có thao tác chuyển tiền tự động.");return "redirect:/quan-tri/hoan-tien/"+id;}catch(IllegalArgumentException ex){model.addAttribute("loi",ex.getMessage());}return chiTiet(id,user,model);
    }
    @PostMapping("/quan-tri/hoan-tien/{id}/loi") public String loi(@PathVariable long id,@RequestParam String lyDo,Principal user,RedirectAttributes flash){try{service.danhDauLoi(user.getName(),id,lyDo);flash.addFlashAttribute("thongBao","Đã ghi thất bại; hạn mức hoàn vẫn được giữ để xử lý lại.");}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}return "redirect:/quan-tri/hoan-tien/"+id;}
    @PostMapping("/quan-tri/hoan-tien/{id}/thu-lai") public String thuLai(@PathVariable long id,Principal user,RedirectAttributes flash){try{service.thuLai(user.getName(),id);flash.addFlashAttribute("thongBao","Đã đưa khoản hoàn về hàng chờ.");}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}return "redirect:/quan-tri/hoan-tien/"+id;}
    @PostMapping("/quan-tri/don-hang/{code}/duyet-thu-du") public String thuDu(@PathVariable String code,@RequestParam(defaultValue="false") boolean daKiemTra,@RequestParam String ghiChu,Principal user,RedirectAttributes flash){try{service.chapNhanThuDu(user.getName(),code,daKiemTra,ghiChu);flash.addFlashAttribute("thongBao","Đã nhận tiền hàng sau đối soát; phần dư tiếp tục được hoàn riêng.");}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}return "redirect:/quan-tri/don-hang/"+code;}
    @GetMapping("/don-hang/hoan-tien/{id}/chung-tu") public ResponseEntity<byte[]> chungTu(@PathVariable long id,Principal user){return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(service.tenChungTu(user.getName(),id)).build().toString()).body(service.chungTu(user.getName(),id));}
}
