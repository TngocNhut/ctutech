package vn.luma.web;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.*;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.KhuyenMaiService;

@Controller
public class KhuyenMaiController {
    private final vn.luma.business.NoiDungService editorial;private final KhuyenMaiService service;
    public KhuyenMaiController(KhuyenMaiService service,vn.luma.business.NoiDungService editorial){this.service=service;this.editorial=editorial;}
    @GetMapping("/uu-dai") public String uuDai(@RequestParam(defaultValue="1") int trang,Principal u,Model m){m.addAttribute("trang",trang);m.addAttribute("bannerCongKhai",editorial.bannerCongKhai("UU_DAI"));m.addAttribute("vouchers",service.congKhai(trang));m.addAttribute("daDangNhap",u!=null);return "uu-dai";}
    @GetMapping("/quan-tri/khuyen-mai") public String danhSach(@RequestParam(defaultValue="1") int trang,Principal u,Model m){m.addAttribute("trang",trang);m.addAttribute("chuongTrinh",service.danhSach(u.getName(),trang));return "quan-tri/khuyen-mai";}
    private LocalDateTime now(){return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).withSecond(0).withNano(0);}
    private LocalDateTime vn(Object value){return vn.luma.data.ThanhToanDAO.instant(value).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime();}
    @GetMapping({"/quan-tri/khuyen-mai/moi","/quan-tri/khuyen-mai/{id}"})
    public String sua(@PathVariable(required=false) Long id,Principal u,Model m){
        service.danhSach(u.getName());m.addAttribute("maChuongTrinh",id);
        if(id==null){if(!m.containsAttribute("form"))m.addAttribute("form",new ChuongTrinhYeuCau("","","",now(),now().plusDays(7),0,"NHAP",false));}
        else{var r=service.chiTiet(u.getName(),id);m.addAttribute("quyTac",service.quyTac(u.getName(),id));if(!m.containsAttribute("form"))m.addAttribute("form",new ChuongTrinhYeuCau((String)r.get("KyHieu"),(String)r.get("TenChuongTrinh"),(String)r.get("MoTa"),vn(r.get("NgayBatDau")),vn(r.get("NgayKetThuc")),((Number)r.get("DoUuTien")).intValue(),(String)r.get("TrangThai"),(Boolean)r.get("ChoPhepKemMaGiamGia")));}
        if(!m.containsAttribute("rule"))m.addAttribute("rule",new QuyTacKhuyenMaiYeuCau(0,"SO_TIEN",null,true));return "quan-tri/khuyen-mai-form";
    }
    @PostMapping({"/quan-tri/khuyen-mai/moi","/quan-tri/khuyen-mai/{id}"})
    public String luu(@PathVariable(required=false) Long id,@Valid @ModelAttribute("form") ChuongTrinhYeuCau f,BindingResult errors,Principal u,Model m,RedirectAttributes flash){
        if(!errors.hasErrors())try{long saved=service.luu(u.getName(),id,f);flash.addFlashAttribute("thongBao","Đã lưu chiến dịch. Chỉ chiến dịch công bố, còn hạn và có quy tắc hợp lệ mới đổi giá bán.");return "redirect:/quan-tri/khuyen-mai/"+saved;}catch(IllegalArgumentException ex){m.addAttribute("loi",ex.getMessage());}
        return sua(id,u,m);
    }
    @PostMapping("/quan-tri/khuyen-mai/{id}/sku")
    public String quyTac(@PathVariable long id,@Valid @ModelAttribute("rule") QuyTacKhuyenMaiYeuCau f,BindingResult errors,Principal u,Model m,RedirectAttributes flash){
        if(!errors.hasErrors())try{service.luuQuyTac(u.getName(),id,f);flash.addFlashAttribute("thongBao","Đã lưu quy tắc SKU; bản chụp của đơn cũ được giữ nguyên.");return "redirect:/quan-tri/khuyen-mai/"+id;}catch(IllegalArgumentException ex){m.addAttribute("loi",ex.getMessage());}return sua(id,u,m);
    }
    @GetMapping("/quan-tri/ma-giam-gia") public String vouchers(@RequestParam(defaultValue="1") int trang,Principal u,Model m){m.addAttribute("trang",trang);m.addAttribute("vouchers",service.vouchers(u.getName(),trang));return "quan-tri/ma-giam-gia";}
    @GetMapping({"/quan-tri/ma-giam-gia/moi","/quan-tri/ma-giam-gia/{id}"})
    public String voucher(@PathVariable(required=false) Long id,Principal u,Model m){
        service.vouchers(u.getName());m.addAttribute("maVoucher",id);
        if(!m.containsAttribute("form")){if(id==null)m.addAttribute("form",new VoucherYeuCau("","","SO_TIEN",null,null,BigDecimal.ZERO,now(),now().plusDays(7),100,1,false,false));
            else{var r=service.voucher(u.getName(),id);m.addAttribute("form",new VoucherYeuCau((String)r.get("KyHieu"),(String)r.get("MoTa"),(String)r.get("KieuGiam"),(BigDecimal)r.get("GiaTriGiam"),(BigDecimal)r.get("GiamToiDa"),(BigDecimal)r.get("DonToiThieu"),vn(r.get("NgayBatDau")),vn(r.get("NgayKetThuc")),((Number)r.get("TongLuotToiDa")).intValue(),((Number)r.get("LuotMoiKhach")).intValue(),(Boolean)r.get("CongKhai"),(Boolean)r.get("DangHoatDong")));}}
        return "quan-tri/ma-giam-gia-form";
    }
    @PostMapping({"/quan-tri/ma-giam-gia/moi","/quan-tri/ma-giam-gia/{id}"})
    public String luuVoucher(@PathVariable(required=false) Long id,@Valid @ModelAttribute("form") VoucherYeuCau f,BindingResult errors,Principal u,Model m,RedirectAttributes flash){
        if(!errors.hasErrors())try{long saved=service.luuVoucher(u.getName(),id,f);flash.addFlashAttribute("thongBao","Đã lưu mã giảm giá. Lượt chỉ được giữ khi đặt đơn thành công.");return "redirect:/quan-tri/ma-giam-gia/"+saved;}catch(IllegalArgumentException ex){m.addAttribute("loi",ex.getMessage());}return voucher(id,u,m);
    }
}
