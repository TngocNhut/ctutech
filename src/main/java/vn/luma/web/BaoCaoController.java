package vn.luma.web;
import java.time.*;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import vn.luma.business.BaoCaoService;

@Controller
public class BaoCaoController {
    private final BaoCaoService service;
    public BaoCaoController(BaoCaoService service){this.service=service;}
    @GetMapping({"/quan-tri","/quan-tri/bao-cao"}) public String xem(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate tu,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate den,@RequestParam(defaultValue="day") String nhom,@RequestParam(defaultValue="1") int trangTuongTac,Principal u,Model m){var today=LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));if(tu==null)tu=today.withDayOfMonth(1);if(den==null)den=today;m.addAttribute("baoCao",service.xem(u.getName(),tu,den,nhom,trangTuongTac));m.addAttribute("tu",tu);m.addAttribute("den",den);m.addAttribute("nhom",nhom);return "quan-tri/bao-cao";}
    @GetMapping("/quan-tri/bao-cao/doanh-so.csv") public org.springframework.http.ResponseEntity<byte[]> csv(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate tu,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate den,@RequestParam(defaultValue="day") String nhom,Principal u){var report=service.xem(u.getName(),tu,den,nhom);var text=new StringBuilder("\uFEFFKỳ,Số đơn,Số máy,Giá trị hàng VND,Giá vốn VND,Lãi gộp trước trả hàng VND\r\n");for(var item:(java.util.List<?>)report.get("doanhSo")){var r=(java.util.Map<?,?>)item;var revenue=(java.math.BigDecimal)r.get("doanh_thu");var cost=(java.math.BigDecimal)r.get("gia_von");text.append(r.get("ky")).append(',').append(r.get("so_don")).append(',').append(r.get("so_may")).append(',').append(revenue.toPlainString()).append(',').append(cost.toPlainString()).append(',').append(revenue.subtract(cost).toPlainString()).append("\r\n");}return org.springframework.http.ResponseEntity.ok().header("Content-Disposition","attachment; filename=\"doanh-so-"+tu+"-"+den+".csv\"").cacheControl(org.springframework.http.CacheControl.noStore()).contentType(org.springframework.http.MediaType.parseMediaType("text/csv;charset=UTF-8")).body(text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
}
