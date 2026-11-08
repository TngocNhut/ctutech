package vn.luma.web;

import java.security.Principal;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.MuaLaiService;

@Controller
public class LuaChonController {
    private final LuaChonTrinhDuyet choices;private final MuaLaiService buyAgain;
    public LuaChonController(LuaChonTrinhDuyet choices,MuaLaiService buyAgain){this.choices=choices;this.buyAgain=buyAgain;}
    @GetMapping("/da-xem") public String daXem(HttpSession session,Model m){m.addAttribute("luaChon",choices.xem(session));return "san-pham/da-xem";}
    @PostMapping("/da-xem/xoa") public String xoaDaXem(HttpSession session){choices.xoaDaXem(session);return "redirect:/da-xem";}
    @PostMapping("/don-hang/{code}/mua-lai") public String muaLai(@PathVariable String code,Principal u,HttpSession session,RedirectAttributes flash){synchronized(session){if(!GioHangController.dungChu(session,u.getName()))throw new org.springframework.security.access.AccessDeniedException("Lựa chọn đang chờ thuộc tài khoản khác.");if(!GioHangController.guest(session).isEmpty()){flash.addFlashAttribute("loi","Giỏ còn lựa chọn chưa gộp. Hãy xử lý các lựa chọn đó trước khi thêm lại đơn.");return "redirect:/gio-hang";}try{var result=buyAgain.them(u.getName(),code);GioHangController.luuConLai(session,u.getName(),result.conLai());flash.addFlashAttribute("thongBao","Đã thêm "+result.daGop()+" máy từ đơn cũ vào giỏ theo giá hiện tại."+(result.canKiemTra()>0?" Có cấu hình ngừng bán hoặc thiếu tồn; hãy kiểm tra giỏ.":"")+(result.conLai().isEmpty()?"":" Còn lựa chọn chưa gộp do giới hạn giỏ; có thể giảm/xóa hoặc gộp lại."));}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}return "redirect:/gio-hang";}}
}
