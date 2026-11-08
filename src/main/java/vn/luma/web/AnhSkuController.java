package vn.luma.web;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
@Controller public class AnhSkuController {
 private final JdbcClient jdbc;
 public AnhSkuController(JdbcClient jdbc){this.jdbc=jdbc;}
 @GetMapping("/anh-sku/{id}") public String anh(@PathVariable long id){var image=jdbc.sql("""
 SELECT t."DuongDanTep" FROM "BienTheSanPham" b JOIN "TepSanPham" t ON t."MaSanPham"=b."MaSanPham" WHERE b."MaBienThe"=:id AND t."LoaiTep"='ANH' AND t."DangHienThi" AND (t."MaBienThe"=b."MaBienThe" OR t."MaBienThe" IS NULL) ORDER BY t."MaBienThe" NULLS LAST,t."ThuTu",t."MaTep" LIMIT 1
 """).param("id",id).query(String.class).optional().filter(x->x.startsWith("/media/")&&!x.contains("..")&&!x.contains("\\"));return "redirect:"+image.orElse("/media/product-placeholder.svg");}
}
