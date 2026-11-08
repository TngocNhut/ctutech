package vn.luma.web;

import java.security.Principal;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.*;

@Controller
public class DatHangController {
    private final GioHangService carts;private final DonHangService orders;private final TaiKhoanService users;private final BanDoService maps;private final DiaChiService addresses;private final ThanhToanService payments;private final vn.luma.config.ThanhToanCauHinh paymentConfig;
    private final GiaoNhanService shipping;private final HoaDonService invoices;private final TraHangService returns;private final HoanTienService refunds;
    @Value("${app.maps.browser-key:}") String mapKey;@Value("${app.maps.map-id:DEMO_MAP_ID}") String mapId;
    public DatHangController(GioHangService carts,DonHangService orders,TaiKhoanService users,BanDoService maps,DiaChiService addresses,ThanhToanService payments,vn.luma.config.ThanhToanCauHinh paymentConfig,GiaoNhanService shipping,HoaDonService invoices,TraHangService returns,HoanTienService refunds){this.carts=carts;this.orders=orders;this.users=users;this.maps=maps;this.addresses=addresses;this.payments=payments;this.paymentConfig=paymentConfig;this.shipping=shipping;this.invoices=invoices;this.returns=returns;this.refunds=refunds;}
    private void duLieu(Principal user,Model model){model.addAttribute("gio",carts.xem(user.getName(),Map.of()));model.addAttribute("phiGiao",orders.phiGiao());model.addAttribute("diaChiDaLuu",addresses.danhSach(user.getName()));model.addAttribute("mapKey",mapKey);model.addAttribute("mapId",mapId);model.addAttribute("payosLive",paymentConfig.live());model.addAttribute("payosMock",paymentConfig.simulationEnabled());}
    @GetMapping("/dat-hang") public String xem(Principal user,Model model){
        var account=users.hienTai(user.getName());var saved=addresses.danhSach(user.getName());var address=saved.isEmpty()?DiaChiYeuCau.trong(account.hoTen(),account.soDienThoai()):new DiaChiYeuCau((String)saved.getFirst().get("TenNguoiNhan"),(String)saved.getFirst().get("SoDienThoaiNhan"),(String)saved.getFirst().get("TinhThanh"),(String)saved.getFirst().get("PhuongXa"),(String)saved.getFirst().get("DiaChiCuThe"),(String)saved.getFirst().get("GhiChuGiaoHang"),"",null,null,false);model.addAttribute("form",new DatHangYeuCau(UUID.randomUUID().toString(),orders.noiDungGio(user.getName()),"",address));duLieu(user,model);return "dat-hang";
    }
    @PostMapping("/dat-hang") public String dat(@Valid @ModelAttribute("form") DatHangYeuCau form,BindingResult errors,@RequestParam(defaultValue="dat") String hanhDong,Principal user,Model model,RedirectAttributes flash){
        if(!errors.hasErrors())try{
            if("bao-gia".equals(hanhDong)){model.addAttribute("baoGia",orders.baoGia(user.getName(),form));}
            else{var address=maps.chuanHoa(form.diaChi());String code=orders.datHang(user.getName(),form,address);
                if("COD".equals(form.phuongThuc()))return "redirect:/don-hang/"+code;
                try{payments.taoLink(user.getName(),code);}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}
                return "redirect:/thanh-toan/"+code;}
        }catch(IllegalArgumentException ex){model.addAttribute("loi",ex.getMessage());}
        duLieu(user,model);return "dat-hang";
    }
    @GetMapping("/don-hang") public String danhSach(@RequestParam(defaultValue="1") int trang,Principal user,Model model){model.addAttribute("trang",trang);model.addAttribute("donHang",orders.danhSach(user.getName(),trang));return "don-hang/danh-sach";}
    @GetMapping("/don-hang/{code}") public String chiTiet(@PathVariable String code,Principal user,Model model){var order=orders.chiTiet(user.getName(),code);model.addAttribute("laChuDon",((Number)order.get("MaNguoiDung")).longValue()==users.hienTai(user.getName()).maNguoiDung());model.addAttribute("don",order);model.addAttribute("dong",orders.dong(user.getName(),code));model.addAttribute("lichSu",orders.lichSu(user.getName(),code));model.addAttribute("vanDon",shipping.vanDon(user.getName(),code).orElse(null));model.addAttribute("lichSuVanDon",shipping.lichSu(user.getName(),code));model.addAttribute("coHoaDon",invoices.daPhatHanh(user.getName(),code));model.addAttribute("phieuTra",returns.theoDon(user.getName(),code));model.addAttribute("khoanHoan",refunds.theoDon(user.getName(),code));if("PAYOS".equals(order.get("PhuongThucThanhToan")))model.addAttribute("payMode",payments.phien(user.getName(),code).get("CheDoXuLy"));return "don-hang/chi-tiet";}
    @PostMapping("/don-hang/{code}/huy") public String huy(@PathVariable String code,Principal user,RedirectAttributes flash){try{if("PAYOS".equals(orders.chiTiet(user.getName(),code).get("PhuongThucThanhToan")))payments.huy(user.getName(),code);else orders.huy(user.getName(),code);flash.addFlashAttribute("thongBao","Đơn đã hủy, hàng giữ đã trả lại. Khoản đã thu, nếu có, được chuyển xử lý hoàn.");}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}return "redirect:/don-hang/"+code;}
}
