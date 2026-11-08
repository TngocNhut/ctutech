package vn.luma.web;

import java.security.Principal;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.*;

@Controller
public class GiaoNhanController {
    private final GiaoNhanService service;private final DonHangService orders;private final HoaDonService invoices;private final TraHangService returns;private final HoanTienService refunds;private final ThanhToanService payments;
    public GiaoNhanController(GiaoNhanService service,DonHangService orders,HoaDonService invoices,TraHangService returns,HoanTienService refunds,ThanhToanService payments){this.service=service;this.orders=orders;this.invoices=invoices;this.returns=returns;this.refunds=refunds;this.payments=payments;}
    @GetMapping("/quan-tri/don-hang") public String danhSach(@RequestParam(defaultValue="") String trangThai,@RequestParam(defaultValue="1") int trang,Principal user,Model model){model.addAttribute("trang",trang);model.addAttribute("donHang",service.danhSach(user.getName(),trangThai,trang));model.addAttribute("trangThai",trangThai);return "quan-tri/don-hang";}
    @GetMapping("/quan-tri/don-hang/{code}") public String chiTiet(@PathVariable String code,Principal user,Model model){
        var order=orders.chiTiet(user.getName(),code);var carriers=service.donVi(user.getName());
        model.addAttribute("don",order);model.addAttribute("dong",orders.dong(user.getName(),code));model.addAttribute("lichSu",orders.lichSu(user.getName(),code));model.addAttribute("donVi",carriers);
        model.addAttribute("vanDon",service.vanDon(user.getName(),code).orElse(null));model.addAttribute("suKienVanDon",service.lichSu(user.getName(),code));model.addAttribute("cuoc",service.cuoc(user.getName(),code));model.addAttribute("coHoaDon",invoices.daPhatHanh(user.getName(),code));model.addAttribute("phieuTra",returns.theoDon(user.getName(),code));model.addAttribute("khoanHoan",refunds.theoDon(user.getName(),code));if("PAYOS".equals(order.get("PhuongThucThanhToan")))model.addAttribute("phienPayos",payments.phien(user.getName(),code));
        if(!model.containsAttribute("vanDonForm"))model.addAttribute("vanDonForm",new TaoVanDonYeuCau(carriers.isEmpty()?0:((Number)carriers.get(0).get("MaDonViVanChuyen")).longValue(),"LV-"+code,null,null));
        if(!model.containsAttribute("suKienForm"))model.addAttribute("suKienForm",new SuKienGiaoNhanYeuCau("BAN_GIAO",UUID.randomUUID().toString(),""));
        if(!model.containsAttribute("thuForm"))model.addAttribute("thuForm",new ThuCODYeuCau((java.math.BigDecimal)order.get("TongThanhToan"),"",false));
        model.addAttribute("tongCuoc",service.tongCuoc(user.getName(),code));
        if(!model.containsAttribute("cuocForm"))model.addAttribute("cuocForm",new PhiVanChuyenYeuCau(UUID.randomUUID().toString(),"PHU_PHI",null,""));
        return "quan-tri/chi-tiet-don";
    }
    @PostMapping("/quan-tri/don-hang/{code}/xac-nhan") public String xacNhan(@PathVariable String code,@RequestParam(defaultValue="false") boolean daKiemTraDiaChi,Principal user,RedirectAttributes flash){try{service.xacNhan(user.getName(),code,daKiemTraDiaChi);flash.addFlashAttribute("thongBao","Đã xác nhận đơn.");}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}return "redirect:/quan-tri/don-hang/"+code;}
    @PostMapping("/quan-tri/don-hang/{code}/van-don") public String tao(@PathVariable String code,@Valid @ModelAttribute("vanDonForm") TaoVanDonYeuCau form,BindingResult errors,Principal user,Model model,RedirectAttributes flash){
        if(!errors.hasErrors())try{service.taoVanDon(user.getName(),code,form);flash.addFlashAttribute("thongBao","Đã tạo vận đơn và danh sách hàng trong kiện.");return "redirect:/quan-tri/don-hang/"+code;}catch(IllegalArgumentException ex){model.addAttribute("loi",ex.getMessage());}
        return chiTiet(code,user,model);
    }
    @PostMapping("/quan-tri/don-hang/{code}/giao-nhan") public String capNhat(@PathVariable String code,@Valid @ModelAttribute("suKienForm") SuKienGiaoNhanYeuCau form,BindingResult errors,Principal user,Model model,RedirectAttributes flash){
        if(!errors.hasErrors())try{service.capNhat(user.getName(),code,form);flash.addFlashAttribute("thongBao","Đã cập nhật giao nhận.");return "redirect:/quan-tri/don-hang/"+code;}catch(IllegalArgumentException ex){model.addAttribute("loi",ex.getMessage());}
        return chiTiet(code,user,model);
    }
    @PostMapping("/quan-tri/don-hang/{code}/thu-cod") public String thu(@PathVariable String code,@Valid @ModelAttribute("thuForm") ThuCODYeuCau form,BindingResult errors,Principal user,Model model,RedirectAttributes flash){
        if(!errors.hasErrors())try{service.thuCOD(user.getName(),code,form);flash.addFlashAttribute("thongBao","Đã ghi thu COD mô phỏng và phát hành hóa đơn.");return "redirect:/quan-tri/don-hang/"+code;}catch(IllegalArgumentException ex){model.addAttribute("loi",ex.getMessage());}
        return chiTiet(code,user,model);
    }
    @GetMapping("/quan-tri/giao-nhan") public String viec(@RequestParam(defaultValue="false") boolean daDong,@RequestParam(defaultValue="1") int trang,Principal user,Model model){model.addAttribute("viec",service.viec(user.getName(),daDong,trang));model.addAttribute("daDong",daDong);model.addAttribute("trang",trang);return "quan-tri/giao-nhan";}
    @GetMapping("/quan-tri/giao-nhan/{id}") public String viecChiTiet(@PathVariable long id,@RequestParam(defaultValue="1") int trang,Principal user,Model model){
        var task=service.viec(user.getName(),id);model.addAttribute("viec",task);model.addAttribute("coTheSua",service.duocSuaViec(user.getName(),task));model.addAttribute("ghiChu",service.ghiChuViec(user.getName(),id,trang));model.addAttribute("trang",trang);
        if(!model.containsAttribute("form"))model.addAttribute("form",new ViecGiaoNhanYeuCau("",task.get("HanXuLy")==null?null:vn.luma.data.ThanhToanDAO.instant(task.get("HanXuLy")).atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime().withNano(0),false));return "quan-tri/viec-giao-nhan";
    }
    @PostMapping("/quan-tri/giao-nhan/{id}") public String suaViec(@PathVariable long id,@Valid @ModelAttribute("form") ViecGiaoNhanYeuCau form,BindingResult errors,Principal user,Model model,RedirectAttributes flash){if(!errors.hasErrors())try{service.suaViec(user.getName(),id,form);flash.addFlashAttribute("thongBao",form.dongViec()?"Đã đóng việc và lưu ghi chú. Trạng thái giao nhận/đơn/phiếu giữ theo nghiệp vụ thực tế.":"Đã lưu ghi chú và hạn xử lý.");return "redirect:/quan-tri/giao-nhan/"+id;}catch(IllegalArgumentException ex){model.addAttribute("loi",ex.getMessage());}return viecChiTiet(id,1,user,model);}
    @PostMapping("/quan-tri/don-hang/{code}/cuoc") public String cuoc(@PathVariable String code,@Valid @ModelAttribute("cuocForm") PhiVanChuyenYeuCau form,BindingResult errors,Principal user,Model model,RedirectAttributes flash){if(!errors.hasErrors())try{service.themCuoc(user.getName(),code,form);flash.addFlashAttribute("thongBao","Đã ghi cước vận đơn. Phí khách đã trả giữ theo đơn gốc.");return "redirect:/quan-tri/don-hang/"+code;}catch(IllegalArgumentException ex){model.addAttribute("loi",ex.getMessage());}return chiTiet(code,user,model);}
    @PostMapping("/quan-tri/giao-nhan/{id}/nhan") public String nhan(@PathVariable long id,Principal user,RedirectAttributes flash){try{service.nhanViec(user.getName(),id);flash.addFlashAttribute("thongBao","Đã nhận việc.");}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}return "redirect:/quan-tri/giao-nhan";}
}
