package vn.luma.web;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import vn.luma.business.TaiKhoanService;
@Controller
public class DangKyMaController {
 private final TaiKhoanService users;
 public DangKyMaController(TaiKhoanService users){this.users=users;}
 @GetMapping("/tao-tai-khoan") public String form(Model model){model.addAttribute("form",new DangKyMoiYeuCau("","",null,"","",""));return "tai-khoan/dang-ky-moi";}
 @PostMapping("/tao-tai-khoan") public String submit(@Valid @ModelAttribute("form") DangKyMoiYeuCau form,BindingResult errors,Model model){
  if(errors.hasErrors())return "tai-khoan/dang-ky-moi";
  try{users.dangKyTrucTiep(form);return "redirect:/login?registered";}
  catch(IllegalArgumentException ex){model.addAttribute("loi",ex.getMessage());return "tai-khoan/dang-ky-moi";}
 }
}
