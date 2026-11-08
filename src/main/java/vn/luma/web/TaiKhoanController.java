package vn.luma.web;

import java.security.Principal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.TaiKhoanService;

@Controller
public class TaiKhoanController {
    private final vn.luma.config.OAuthCauHinh config;private final TaiKhoanService service;private final vn.luma.business.DiaChiService diaChi;
    public TaiKhoanController(TaiKhoanService service,vn.luma.config.OAuthCauHinh config,vn.luma.business.DiaChiService diaChi) {this.service=service;this.config=config;this.diaChi=diaChi;}
    @ModelAttribute("nhaCungCapOAuth") public java.util.List<String> nhaCungCap(){return config.nhaCungCap();}
    @GetMapping("/login") public String login(jakarta.servlet.http.HttpSession session,Model m){m.addAttribute("nhaCungCapOAuth",config.nhaCungCap());m.addAttribute("choLienKet",LienKetOAuthController.pending(session));return "tai-khoan/login";}
    @GetMapping("/dang-ky") public String dangKy(){return "redirect:/tao-tai-khoan";}
    @GetMapping("/tai-khoan") public String hoSo(Principal user,Model model){model.addAttribute("taiKhoan",service.hienTai(user.getName()));model.addAttribute("diaChiDaLuu",diaChi.danhSach(user.getName()));return "tai-khoan/ho-so";}
    public record HoSoYeuCau(@NotBlank @Size(max=120) String hoTen,@Pattern(regexp="[0-9+() .-]{0,20}",message="Số điện thoại chưa đúng định dạng") String soDienThoai){}
    @PostMapping("/tai-khoan/ho-so")
    public String sua(@Valid @ModelAttribute HoSoYeuCau form,BindingResult errors,Principal user,RedirectAttributes flash) {
        if(errors.hasErrors()){flash.addFlashAttribute("loi","Kiểm tra lại họ tên (tối đa120ký tự) và số điện thoại (tối đa20ký tự).");}
        else{try{service.suaHoSo(user.getName(),form.hoTen(),form.soDienThoai()==null?"":form.soDienThoai());flash.addFlashAttribute("thongBao","Đã cập nhật hồ sơ.");}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}}
        return "redirect:/tai-khoan";
    }
    @PostMapping("/tai-khoan/gui-xac-minh")
    public String guiLai(Principal user,RedirectAttributes flash){service.guiLaiXacMinh(user.getName());flash.addFlashAttribute("thongBao","Kiểm tra hộp thư thử nghiệm để xác minh email. Bạn có thể yêu cầu tối đa5lần/15phút.");return "redirect:/tai-khoan";}
    @GetMapping("/xac-minh-email") public String xacMinh(@RequestParam String token,Model model){model.addAttribute("token",token);return "tai-khoan/xac-minh";}
    @PostMapping("/xac-minh-email")
    public String xacMinhPost(@RequestParam String token,Model model){try{service.xacMinh(token);model.addAttribute("thongBao","Email đã được xác minh.");}catch(IllegalArgumentException ex){model.addAttribute("loi",ex.getMessage());}return "tai-khoan/ket-qua";}
    @GetMapping("/quen-mat-khau") public String quen(){return "tai-khoan/quen-mat-khau";}
    public record EmailYeuCau(@NotBlank @Email @Size(max=254) String email){}
    @PostMapping("/quen-mat-khau")
    public String quenPost(@Valid @ModelAttribute EmailYeuCau form,BindingResult errors,Model model){
        if(errors.hasErrors()){model.addAttribute("loi","Nhập email hợp lệ.");return "tai-khoan/quen-mat-khau";}
        service.yeuCauDatLai(form.email());model.addAttribute("thongBao","Nếu tài khoản phù hợp, liên kết đặt lại mật khẩu sẽ có trong hộp thư thử nghiệm. Liên kết có hạn30phút.");return "tai-khoan/ket-qua";
    }
    @GetMapping("/dat-lai-mat-khau") public String datLai(@RequestParam String token,Model model){model.addAttribute("token",token);return "tai-khoan/dat-lai";}
    @PostMapping("/dat-lai-mat-khau")
    public String datLaiPost(@RequestParam String token,@RequestParam String matKhau,@RequestParam String xacNhan,Model model){
        try{service.datLai(token,matKhau,xacNhan);model.addAttribute("thongBao","Mật khẩu đã đổi. Hãy đăng nhập lại trên các thiết bị của bạn.");return "tai-khoan/ket-qua";}
        catch(IllegalArgumentException ex){model.addAttribute("loi",ex.getMessage());model.addAttribute("token",token);return "tai-khoan/dat-lai";}
    }
}
