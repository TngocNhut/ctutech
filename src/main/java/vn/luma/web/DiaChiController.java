package vn.luma.web;

import java.security.Principal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.*;

@Controller
public class DiaChiController {
    private final DiaChiService service;private final TaiKhoanService users;private final BanDoService maps;
    @Value("${app.maps.browser-key:}") String mapKey;@Value("${app.maps.map-id:DEMO_MAP_ID}") String mapId;
    public DiaChiController(DiaChiService service,TaiKhoanService users,BanDoService maps){this.service=service;this.users=users;this.maps=maps;}
    private void duLieu(Principal user,Model model){model.addAttribute("diaChi",service.danhSach(user.getName()));model.addAttribute("mapKey",mapKey);model.addAttribute("mapId",mapId);}
    @GetMapping("/tai-khoan/dia-chi") public String xem(Principal user,Model model){var u=users.hienTai(user.getName());model.addAttribute("form",DiaChiYeuCau.trong(u.hoTen(),u.soDienThoai()));duLieu(user,model);return "tai-khoan/dia-chi";}
    @PostMapping("/tai-khoan/dia-chi") public String tao(@Valid @ModelAttribute("form") DiaChiYeuCau form,BindingResult errors,@RequestParam(defaultValue="false") boolean macDinh,Principal user,Model model,RedirectAttributes flash){
        if(!errors.hasErrors())try{service.tao(user.getName(),maps.chuanHoa(form),macDinh);flash.addFlashAttribute("thongBao","Đã lưu địa chỉ.");return "redirect:/tai-khoan/dia-chi";}catch(IllegalArgumentException ex){model.addAttribute("loi",ex.getMessage());}
        duLieu(user,model);return "tai-khoan/dia-chi";
    }
    @PostMapping("/tai-khoan/dia-chi/{id}/xoa") public String xoa(@PathVariable long id,Principal user,RedirectAttributes flash){service.xoa(user.getName(),id);flash.addFlashAttribute("thongBao","Đã xóa địa chỉ. Địa chỉ chốt trên đơn vẫn được giữ.");return "redirect:/tai-khoan/dia-chi";}
    @GetMapping("/tai-khoan/dia-chi/{id}/sua") public String sua(@PathVariable long id,Principal user,Model model){
        var a=service.chiTiet(user.getName(),id);model.addAttribute("maDiaChi",id);model.addAttribute("coDiemCu",service.diemConHan(a));
        if(!model.containsAttribute("form"))model.addAttribute("form",new DiaChiYeuCau((String)a.get("TenNguoiNhan"),(String)a.get("SoDienThoaiNhan"),(String)a.get("TinhThanh"),(String)a.get("PhuongXa"),(String)a.get("DiaChiCuThe"),(String)a.get("GhiChuGiaoHang"),"",null,null,false));
        if(!model.containsAttribute("macDinh"))model.addAttribute("macDinh",a.get("LaMacDinh"));if(!model.containsAttribute("giuDiemCu"))model.addAttribute("giuDiemCu",service.diemConHan(a));
        model.addAttribute("mapKey",mapKey);model.addAttribute("mapId",mapId);return "tai-khoan/sua-dia-chi";
    }
    @PostMapping("/tai-khoan/dia-chi/{id}/sua") public String luuSua(@PathVariable long id,@Valid @ModelAttribute("form") DiaChiYeuCau form,BindingResult errors,@RequestParam(defaultValue="false") boolean macDinh,@RequestParam(defaultValue="false") boolean giuDiemCu,Principal user,Model model,RedirectAttributes flash){
        service.chiTiet(user.getName(),id);
        if(!errors.hasErrors())try{service.sua(user.getName(),id,maps.chuanHoa(form),macDinh,giuDiemCu);flash.addFlashAttribute("thongBao","Đã cập nhật địa chỉ. Địa chỉ trên các đơn đã đặt được giữ nguyên.");return "redirect:/tai-khoan/dia-chi";}catch(IllegalArgumentException ex){model.addAttribute("loi",ex.getMessage());}
        model.addAttribute("macDinh",macDinh);model.addAttribute("giuDiemCu",giuDiemCu);return sua(id,user,model);
    }
}
