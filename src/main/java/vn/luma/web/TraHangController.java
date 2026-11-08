package vn.luma.web;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.*;

@Controller
public class TraHangController {
    private final TraHangService service;private final DonHangService orders;private final GiaoNhanService shipping;
    public TraHangController(TraHangService service,DonHangService orders,GiaoNhanService shipping){this.service=service;this.orders=orders;this.shipping=shipping;}
    @GetMapping("/don-hang/{code}/tra-hang") public String tao(@PathVariable String code,Principal user,Model model){
        model.addAttribute("don",orders.chiTiet(user.getName(),code));var lines=service.coTheTra(user.getName(),code);model.addAttribute("dong",lines);
        if(!model.containsAttribute("form"))model.addAttribute("form",new TaoTraHangYeuCau(UUID.randomUUID().toString(),"",lines.stream().map(l->new TaoTraHangYeuCau.Dong(((Number)l.get("MaChiTietDonHang")).longValue(),0)).toList()));return "tra-hang/tao";
    }
    @PostMapping("/don-hang/{code}/tra-hang") public String tao(@PathVariable String code,@Valid @ModelAttribute("form") TaoTraHangYeuCau form,BindingResult errors,Principal user,Model model){
        if(!errors.hasErrors())try{long id=service.tao(user.getName(),code,form,false);return "redirect:/don-hang/tra-hang/"+id;}catch(IllegalArgumentException ex){model.addAttribute("loi",ex.getMessage());}return tao(code,user,model);
    }
    @GetMapping("/don-hang/tra-hang/{id}") public String xem(@PathVariable long id,Principal user,Model model){duLieu(user.getName(),id,model);return "tra-hang/chi-tiet";}
    private void duLieu(String email,long id,Model model){model.addAttribute("phieu",service.chiTiet(email,id));var lines=service.dong(email,id);model.addAttribute("dong",lines);model.addAttribute("vanDon",service.vanDon(email,id).orElse(null));model.addAttribute("suKien",service.lichSuVanDon(email,id));if(!model.containsAttribute("kiemForm"))model.addAttribute("kiemForm",new KiemHangYeuCau(false,lines.stream().map(l->new KiemHangYeuCau.Dong(((Number)l.get("MaChiTietTraHang")).longValue(),0,0,0,"")).toList()));}
    @PostMapping("/don-hang/tra-hang/{id}/huy") public String huy(@PathVariable long id,Principal user,RedirectAttributes flash){try{service.huy(user.getName(),id);flash.addFlashAttribute("thongBao","Đã rút yêu cầu trả hàng.");}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}return "redirect:/don-hang/tra-hang/"+id;}
    @GetMapping("/quan-tri/tra-hang") public String danhSach(@RequestParam(defaultValue="1") int trang,Principal user,Model model){model.addAttribute("trang",trang);model.addAttribute("phieuTra",service.danhSach(user.getName(),trang));return "quan-tri/tra-hang";}
    @GetMapping("/quan-tri/tra-hang/{id}") public String quanTri(@PathVariable long id,Principal user,Model model){
        duLieu(user.getName(),id,model);var waybill=service.vanDon(user.getName(),id);
        var fees=waybill.map(w->shipping.cuocVanDon(user.getName(),((Number)w.get("MaVanDon")).longValue())).orElse(List.of());model.addAttribute("cuocHoan",fees);
        model.addAttribute("daCoCuocGoc",fees.stream().anyMatch(f->"CUOC_GOC".equals(f.get("LoaiPhi"))));model.addAttribute("tongCuocHoan",fees.stream().map(f->"GIAM_CUOC".equals(f.get("LoaiPhi"))?((BigDecimal)f.get("SoTien")).negate():(BigDecimal)f.get("SoTien")).reduce(BigDecimal.ZERO,BigDecimal::add));
        if(!model.containsAttribute("cuocForm"))model.addAttribute("cuocForm",new PhiVanChuyenYeuCau(UUID.randomUUID().toString(),fees.stream().anyMatch(f->"CUOC_GOC".equals(f.get("LoaiPhi")))?"PHU_PHI":"CUOC_GOC",null,""));return "quan-tri/chi-tiet-tra";
    }
    @PostMapping("/quan-tri/tra-hang/{id}/cuoc") public String cuoc(@PathVariable long id,@Valid @ModelAttribute("cuocForm") PhiVanChuyenYeuCau form,BindingResult errors,Principal user,Model model,RedirectAttributes flash){
        service.chiTiet(user.getName(),id);
        if(!errors.hasErrors())try{var waybill=service.vanDon(user.getName(),id).orElseThrow(()->new IllegalArgumentException("Chưa có vận đơn hoàn."));shipping.themCuocVanDon(user.getName(),((Number)waybill.get("MaVanDon")).longValue(),form);flash.addFlashAttribute("thongBao","Đã ghi cước chiều hoàn. Phí khách trả và khoản hoàn tiền trên phiếu giữ nguyên.");return "redirect:/quan-tri/tra-hang/"+id;}catch(IllegalArgumentException ex){model.addAttribute("loi",ex.getMessage());}return quanTri(id,user,model);
    }
    @PostMapping("/quan-tri/don-hang/{code}/nhan-hoan") public String nhanHoan(@PathVariable String code,@RequestParam String lyDo,Principal user,RedirectAttributes flash){
        try{var lines=service.coTheTra(user.getName(),code);var form=new TaoTraHangYeuCau(UUID.randomUUID().toString(),lyDo,lines.stream().map(l->new TaoTraHangYeuCau.Dong(((Number)l.get("MaChiTietDonHang")).longValue(),((Number)l.get("ConCoTheTra")).intValue())).toList());return "redirect:/quan-tri/tra-hang/"+service.tao(user.getName(),code,form,true);}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());return "redirect:/quan-tri/don-hang/"+code;}
    }
    @PostMapping("/quan-tri/tra-hang/{id}/duyet") public String duyet(@PathVariable long id,@RequestParam boolean chapNhan,@RequestParam BigDecimal phiGiaoHoan,@RequestParam String ghiChu,Principal user,RedirectAttributes flash){try{service.duyet(user.getName(),id,chapNhan,phiGiaoHoan,ghiChu);flash.addFlashAttribute("thongBao",chapNhan?"Đã duyệt nhận hàng và tạo vận đơn hoàn nội bộ.":"Đã từ chối yêu cầu trả.");}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}return "redirect:/quan-tri/tra-hang/"+id;}
    @PostMapping("/quan-tri/tra-hang/{id}/dang-hoan") public String dangHoan(@PathVariable long id,Principal user,RedirectAttributes flash){try{service.dangHoan(user.getName(),id);flash.addFlashAttribute("thongBao","Đã ghi nhận kiện đang về kho.");}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}return "redirect:/quan-tri/tra-hang/"+id;}
    @PostMapping("/quan-tri/tra-hang/{id}/kiem-hang") public String kiem(@PathVariable long id,@Valid @ModelAttribute("kiemForm") KiemHangYeuCau form,BindingResult errors,Principal user,Model model,RedirectAttributes flash){if(!errors.hasErrors())try{service.kiemHang(user.getName(),id,form);flash.addFlashAttribute("thongBao","Đã chốt kiểm hàng, nhập lại phần đủ điều kiện và lập yêu cầu hoàn nếu có.");return "redirect:/quan-tri/tra-hang/"+id;}catch(IllegalArgumentException ex){model.addAttribute("loi",ex.getMessage());}return quanTri(id,user,model);}
}
