package vn.luma.web;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.*;

@RestController
public class GoiYTimKiemController {
    private final JdbcClient jdbc;
    public GoiYTimKiemController(JdbcClient jdbc){this.jdbc=jdbc;}
    @GetMapping("/api/tim-kiem/goi-y")
    public List<Map<String,Object>> goiY(@RequestParam(defaultValue="") String q){
        String input=q.strip();
        if(input.length()>100)input=input.substring(0,100);
        String normalized=Normalizer.normalize(input,Normalizer.Form.NFD).replaceAll("\\p{M}","").replace('đ','d').replace('Đ','D');
        String pattern="%"+input.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
        String search="%"+normalized.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
        return jdbc.sql("""
            SELECT name, slug, image FROM (
                SELECT s."TenSanPham" AS name,s."DuongDan" AS slug,
                       (SELECT t."DuongDanTep" FROM "TepSanPham" t WHERE t."MaSanPham"=s."MaSanPham" AND t."LoaiTep"='ANH' AND t."DangHienThi" ORDER BY t."ThuTu",t."MaTep" LIMIT 1) AS image,
                       row_number() OVER (PARTITION BY p."MaPhanLoai" ORDER BY s."MaSanPham") AS category_rank
                FROM "SanPham" s JOIN "PhanLoai" p USING ("MaPhanLoai") JOIN "ThuongHieu" h USING ("MaThuongHieu")
                WHERE s."DangHienThi" AND p."DangHienThi" AND h."DangHienThi"
                  AND EXISTS (SELECT 1 FROM "BienTheSanPham" b WHERE b."MaSanPham"=s."MaSanPham" AND b."DangKinhDoanh")
                  AND (:q='' OR s."TenSanPham" ILIKE :pattern OR s."TenTimKiem" ILIKE :search)
            ) x ORDER BY CASE WHEN :q='' THEN category_rank ELSE 0 END,name LIMIT 10
            """).param("q",input).param("pattern",pattern).param("search",search).query().listOfRows();
    }
}
