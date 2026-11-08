package vn.luma;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import vn.luma.business.SanPhamService;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false"})
class DanhMucBoSungIntegrationTest {
 @Autowired SanPhamService products;@Autowired JdbcClient jdbc;
 @Test void additionalModelsRemainDistinctSourcedAndHaveWorkingSkuImages()throws Exception{verify(products,jdbc,"tai-lieu/33_DanhMucBoSung.json",Map.of("dien-thoai",35,"laptop",35,"phu-kien",35));}
 static void verify(SanPhamService products,JdbcClient jdbc,String manifest,Map<String,Integer> wanted)throws Exception{
  var data=tools.jackson.databind.json.JsonMapper.builder().build().readTree(Files.readString(Path.of(manifest)));
  var rows=jdbc.sql("""
    SELECT s."DuongDan" AS slug,s."TenSanPham",s."DuongDanNguon" AS source,p."DuongDan" AS category,h."TenThuongHieu" AS brand,d."TenDongSanPham" AS line,
      b."MauSac" AS colour,b."MaBienThe",b."GiaBan",coalesce(k."SoLuongTon"-k."SoLuongGiu",0) AS available,t."DuongDanTep" AS image,t."DuongDanNguon" AS image_source
    FROM "SanPham" s JOIN "PhanLoai" p USING("MaPhanLoai") JOIN "ThuongHieu" h USING("MaThuongHieu") JOIN "BienTheSanPham" b USING("MaSanPham")
    LEFT JOIN "DongSanPham" d ON d."MaDongSanPham"=s."MaDongSanPham"
    LEFT JOIN "TonKho" k ON k."MaBienThe"=b."MaBienThe" AND k."MaKhoHang"=1
    LEFT JOIN LATERAL (SELECT x."DuongDanTep",x."DuongDanNguon" FROM "TepSanPham" x WHERE x."MaSanPham"=s."MaSanPham" AND x."MaBienThe"=b."MaBienThe" AND x."LoaiTep"='ANH' AND x."DangHienThi" ORDER BY x."ThuTu",x."MaTep" LIMIT 1) t ON true
    WHERE s."DangHienThi" AND p."DangHienThi" AND h."DangHienThi" AND b."DangKinhDoanh"
    """).query().listOfRows();
  var bySlug=new HashMap<String,List<Map<String,Object>>>();for(var row:rows)bySlug.computeIfAbsent((String)row.get("slug"),k->new ArrayList<>()).add(row);
  var counts=new HashMap<String,Integer>();var slugs=new HashSet<String>();var sampled=new HashSet<String>();
  for(var m:data.get("models")){String slug=m.get("slug").asString(),category=m.get("category").asString();assertThat(slugs.add(slug)).isTrue();var variants=bySlug.get(slug);if(variants==null && manifest.endsWith("33_DanhMucBoSung.json"))continue;assertThat(variants).as(slug).isNotNull().hasSize(m.get("variants").size());var expected=new ArrayList<String>();for(var v:m.get("variants"))expected.add(v.get("colour").asString().toLowerCase(Locale.ROOT)+"|"+v.get("image").asString());var actual=new ArrayList<String>();for(var r:variants){assertThat(r.get("category")).isEqualTo(category);assertThat(r.get("line")).isEqualTo(m.get("line").asString());assertThat((String)r.get("brand")).isEqualToIgnoringCase(m.get("brand").asString());assertThat(r.get("source")).isEqualTo(m.get("source").asString());assertThat(r.get("image_source")).isEqualTo(m.get("source").asString());assertThat((java.math.BigDecimal)r.get("GiaBan")).isPositive();if(manifest.endsWith("33_DanhMucBoSung.json"))assertThat(((Number)r.get("available")).longValue()).isPositive();else assertThat(((Number)r.get("available")).longValue()).isNotNegative();actual.add(((String)r.get("colour")).toLowerCase(Locale.ROOT)+"|"+r.get("image"));}assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);counts.merge(category,1,Integer::sum);if(sampled.add(category)){var product=products.chiTiet(slug);assertThat(products.anh(slug,product.maSkuGoiY())).isNotEmpty();}}
  for(String category:List.of("dien-thoai","laptop","phu-kien"))assertThat(counts.get(category)).isEqualTo(wanted.get(category));
  for(var image:data.get("assets")){Path file=Path.of("src/main/resources/static"+image.get("file").asString());assertThat(Files.size(file)).isEqualTo(image.get("bytes").asLong());String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));assertThat(hash).isEqualTo(image.get("sha256").asString());}
 }
}
