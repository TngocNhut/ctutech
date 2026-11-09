package vn.luma.web;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luma.business.*;

@Controller
public class ThanhToanController {
    private final ThanhToanService service;private final DonHangService orders;
    public ThanhToanController(ThanhToanService service,DonHangService orders){this.service=service;this.orders=orders;}
    @GetMapping({"/thanh-toan/{code}","/thanh-toan/ket-qua/{code}"})
    public String xem(@PathVariable String code,Principal user,Model model){model.addAttribute("don",orders.chiTiet(user.getName(),code));model.addAttribute("phien",service.phien(user.getName(),code));model.addAttribute("daThu",service.daThu(user.getName(),code));model.addAttribute("nonce",UUID.randomUUID().toString());return "thanh-toan";}
    @PostMapping("/thanh-toan/{code}/tao-link") public String tao(@PathVariable String code,Principal user,RedirectAttributes flash){try{service.taoLink(user.getName(),code);}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}return "redirect:/thanh-toan/"+code;}
    @PostMapping("/thanh-toan/{code}/kiem-tra") public String kiemTra(@PathVariable String code,Principal user,RedirectAttributes flash){try{service.doiSoat(user.getName(),code);flash.addFlashAttribute("thongBao","Đã cập nhật trạng thái hiện có. Các lần kiểm tra gần nhau dùng kết quả mới nhất; bạn có thể thử lại sau vài giây.");}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}return "redirect:/thanh-toan/"+code;}
    @PostMapping("/thanh-toan/{code}/mo-phong") public String moPhong(@PathVariable String code,@RequestParam BigDecimal soTien,@RequestParam String nonce,Principal user,RedirectAttributes flash){try{service.moPhong(user.getName(),code,soTien,nonce);flash.addFlashAttribute("thongBao","Đã ghi nhận khoản thu mô phỏng. Không có chuyển tiền thật.");}catch(IllegalArgumentException ex){flash.addFlashAttribute("loi",ex.getMessage());}return "redirect:/thanh-toan/"+code;}
    @PostMapping(value="/api/payos/webhook",produces=MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody public ResponseEntity<String> webhook(HttpServletRequest request)throws java.io.IOException{
        var log=org.slf4j.LoggerFactory.getLogger(ThanhToanController.class);
        log.info("PayOS webhook: request received");
        byte[] data=request.getInputStream().readNBytes(65537);
        if(data.length>65536){log.warn("PayOS webhook: payload too large");return ResponseEntity.status(413).body("{\"ok\":false}");}
        try{service.webhook(new String(data,StandardCharsets.UTF_8));return ResponseEntity.ok("{\"ok\":true}");}
        catch(IllegalStateException ex){log.warn("PayOS webhook: unavailable");return ResponseEntity.status(503).body("{\"ok\":false}");}
        catch(IllegalArgumentException ex){log.warn("PayOS webhook: rejected by handler");return ResponseEntity.badRequest().body("{\"ok\":false}");}
    }
}
