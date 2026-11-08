package vn.luma.web;

import java.security.Principal;
import java.nio.charset.StandardCharsets;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import vn.luma.business.HoaDonService;

@Controller
public class HoaDonController {
    private final HoaDonService service;
    public HoaDonController(HoaDonService service){this.service=service;}
    @GetMapping("/don-hang/{code}/hoa-don.pdf") public ResponseEntity<byte[]> tai(@PathVariable String code,Principal user){
        byte[] data=service.tai(user.getName(),code);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename("HoaDon-"+code+".pdf",StandardCharsets.UTF_8).build().toString()).contentLength(data.length).body(data);
    }
}
