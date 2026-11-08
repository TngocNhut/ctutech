package vn.luma.web;

import java.security.Principal;
import java.util.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.GioHangService;

@Controller
public class GioHangController {
    private final HanhViController analytics;private final GioHangService service;
    public GioHangController(GioHangService service,HanhViController analytics){this.service=service;this.analytics=analytics;}
    @SuppressWarnings("unchecked") public static Map<Long,Integer> guest(HttpSession session){
        var value=session.getAttribute(GioHangService.SESSION_KEY);
        return value instanceof Map<?,?>?new TreeMap<>((Map<Long,Integer>)value):new TreeMap<>();
    }
    public static boolean dungChu(HttpSession session,String email){var owner=session.getAttribute(GioHangService.OWNER_KEY);return owner==null||owner.equals(email);}
    public static void luuConLai(HttpSession session,String email,Map<Long,Integer> remaining){if(remaining.isEmpty()){session.removeAttribute(GioHangService.SESSION_KEY);session.removeAttribute(GioHangService.OWNER_KEY);}else{session.setAttribute(GioHangService.SESSION_KEY,new TreeMap<>(remaining));session.setAttribute(GioHangService.OWNER_KEY,email);}}
    @GetMapping("/gio-hang") public String xem(Principal user,HttpSession session,Model model){model.addAttribute("gio",service.xem(user==null?null:user.getName(),guest(session)));model.addAttribute("chuaGop",user!=null&&dungChu(session,user.getName())?service.chuaGop(user.getName(),guest(session)):List.of());model.addAttribute("thongBaoGopGio",session.getAttribute(GioHangService.NOTICE_KEY));session.removeAttribute(GioHangService.NOTICE_KEY);return "gio-hang";}
    @PostMapping("/gio-hang/chua-gop/gop-lai") public String gopLai(Principal user,HttpSession session,RedirectAttributes flash){
        synchronized(session){if(!dungChu(session,user.getName()))throw new org.springframework.security.access.AccessDeniedException("Lựa chọn thuộc tài khoản khác.");try{var result=service.gop(user.getName(),guest(session));luuConLai(session,user.getName(),result.conLai());flash.addFlashAttribute("thongBao",result.thongBao());}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}}return "redirect:/gio-hang";
    }
    @PostMapping("/gio-hang/chua-gop/cap-nhat") public String suaChoGop(@RequestParam long sku,@RequestParam int soLuong,Principal user,HttpSession session,RedirectAttributes flash){
        synchronized(session){if(!dungChu(session,user.getName()))throw new org.springframework.security.access.AccessDeniedException("Lựa chọn thuộc tài khoản khác.");var pending=guest(session);if(!pending.containsKey(sku)||soLuong<0||soLuong>pending.get(sku)){flash.addFlashAttribute("loi","Chỉ giảm số lượng hoặc xóa lựa chọn đang chờ gộp.");}else{if(soLuong==0)pending.remove(sku);else pending.put(sku,soLuong);luuConLai(session,user.getName(),pending);flash.addFlashAttribute("thongBao","Đã cập nhật lựa chọn chưa gộp.");}}return "redirect:/gio-hang";
    }
    @PostMapping("/gio-hang/them") public String them(@RequestParam long sku,@RequestParam(defaultValue="1") int soLuong,Principal user,HttpSession session,RedirectAttributes flash){return sua(sku,soLuong,true,user,session,flash);}
    @PostMapping("/gio-hang/cap-nhat") public String capNhat(@RequestParam long sku,@RequestParam int soLuong,Principal user,HttpSession session,RedirectAttributes flash){return sua(sku,soLuong,false,user,session,flash);}
    private String sua(long sku,int quantity,boolean add,Principal user,HttpSession session,RedirectAttributes flash){
        try{
            if(user==null){
                // ponytail: per-session lock assumes one local application instance; use a shared cart store for multiple instances.
                synchronized(session){session.setAttribute(GioHangService.SESSION_KEY,service.suaKhach(guest(session),sku,quantity,add));}
            }else service.sua(user.getName(),sku,quantity,add);
            if(add)analytics.themGio(session,user,sku);
            flash.addFlashAttribute("thongBao",quantity==0?"Đã xóa sản phẩm khỏi giỏ.":"Đã cập nhật giỏ hàng.");
        }catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}
        return "redirect:/gio-hang";
    }
}
