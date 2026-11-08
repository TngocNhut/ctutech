package vn.luma.web;

import java.io.IOException;
import java.security.Principal;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.*;

@Controller
public class AnhSanPhamController {
    private final AnhSanPhamService images;private final QuanLySanPhamService products;
    public AnhSanPhamController(AnhSanPhamService images,QuanLySanPhamService products){this.images=images;this.products=products;}
    private void duLieu(long product,Principal user,Model model){model.addAttribute("modelSp",products.model(user.getName(),product));model.addAttribute("sku",products.bienThe(user.getName(),product));model.addAttribute("anh",images.danhSach(user.getName(),product));}
    @GetMapping("/quan-tri/model/{product}/anh") public String danhSach(@PathVariable long product,Principal user,Model model){duLieu(product,user,model);if(!model.containsAttribute("form"))model.addAttribute("form",new AnhSanPhamYeuCau(null,"",0,"","",true));return "quan-tri/anh-san-pham";}
    @PostMapping("/quan-tri/model/{product}/anh") public String tao(@PathVariable long product,@Valid @ModelAttribute("form") AnhSanPhamYeuCau form,BindingResult errors,@RequestParam MultipartFile tep,Principal user,Model model,RedirectAttributes flash)throws IOException{
        if(!errors.hasErrors())try{images.tao(user.getName(),product,form,tep.getBytes());flash.addFlashAttribute("thongBao","Đã lưu ảnh và thông tin nguồn.");return "redirect:/quan-tri/model/"+product+"/anh";}catch(IllegalArgumentException ex){model.addAttribute("loi",ex.getMessage());}return danhSach(product,user,model);
    }
    @GetMapping("/quan-tri/model/{product}/anh/{id}") public String sua(@PathVariable long product,@PathVariable long id,Principal user,Model model){duLieu(product,user,model);var image=images.chiTiet(user.getName(),product,id);model.addAttribute("tepAnh",image);if(!model.containsAttribute("form"))model.addAttribute("form",new AnhSanPhamYeuCau(image.get("MaBienThe")==null?null:((Number)image.get("MaBienThe")).longValue(),(String)image.get("MoTaThayThe"),((Number)image.get("ThuTu")).intValue(),(String)image.get("DuongDanNguon"),(String)image.get("ThongTinSuDung"),(Boolean)image.get("DangHienThi")));return "quan-tri/anh-form";}
    @PostMapping("/quan-tri/model/{product}/anh/{id}") public String luu(@PathVariable long product,@PathVariable long id,@Valid @ModelAttribute("form") AnhSanPhamYeuCau form,BindingResult errors,Principal user,Model model,RedirectAttributes flash){if(!errors.hasErrors())try{images.sua(user.getName(),product,id,form);flash.addFlashAttribute("thongBao","Đã cập nhật ảnh.");return "redirect:/quan-tri/model/"+product+"/anh";}catch(IllegalArgumentException ex){model.addAttribute("loi",ex.getMessage());}return sua(product,id,user,model);}
    @PostMapping("/quan-tri/model/{product}/anh/{id}/an") public String an(@PathVariable long product,@PathVariable long id,Principal user,RedirectAttributes flash){images.an(user.getName(),product,id);flash.addFlashAttribute("thongBao","Đã ẩn ảnh. Có thể mở lại trong trang sửa ảnh.");return "redirect:/quan-tri/model/"+product+"/anh";}
    @GetMapping("/tep-san-pham/{file}") public ResponseEntity<byte[]> doc(@PathVariable String file,Principal user){byte[] data=images.doc(user==null?null:user.getName(),file);return ResponseEntity.ok().contentType(file.endsWith(".png")?MediaType.IMAGE_PNG:MediaType.IMAGE_JPEG).cacheControl(CacheControl.noStore()).body(data);}
}
