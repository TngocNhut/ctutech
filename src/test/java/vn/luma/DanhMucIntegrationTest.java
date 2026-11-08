package vn.luma;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import vn.luma.business.SanPhamService;
import vn.luma.web.BoLocSanPham;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false"})
class DanhMucIntegrationTest {
    @Autowired SanPhamService products;
    @Autowired org.springframework.jdbc.core.simple.JdbcClient jdbc;
    @LocalServerPort int port;
    BoLocSanPham filter(String q,BigDecimal maximum,Integer storage,String sort,int page){return new BoLocSanPham(q,"","",null,maximum,null,storage,"","",false,sort,page);}
    @Test @org.springframework.transaction.annotation.Transactional void priceAndStorageMustMatchSameSkuAndCardPointsToMatchingSku(){
        jdbc.sql("UPDATE \"BienTheSanPham\" SET \"DungLuongLuuTru\"=256,\"TenBienThe\"='4GB/256GB - Hong',\"GiaBan\"=12990000,\"GiaNiemYet\"=13990000 WHERE \"MaBienThe\"=2").update();
        assertThat(products.loc(filter("iPhone 13",new BigDecimal("12000000"),256,"mac-dinh",1)).tong()).isZero();
        var match=products.loc(filter("iPhone 13",new BigDecimal("14000000"),256,"mac-dinh",1));assertThat(match.tong()).isEqualTo(1);
        var card=match.noiDung().getFirst();assertThat(card.maSkuGoiY()).isEqualTo(2);assertThat(card.cauHinhGoiY()).contains("256");
        assertThat(card.giaTu()).isEqualByComparingTo(products.bienThe("iphone-13").stream().filter(v->v.maBienThe()==2).findFirst().orElseThrow().giaBan());
        assertThat(products.loc(filter("%",null,null,"mac-dinh",1)).tong()).isZero();
    }
    @Test void pagesDoNotOverlapAndSortingRemainsDeterministic(){
        var first=products.loc(filter("",null,null,"gia-tang",1));var second=products.loc(filter("",null,null,"gia-tang",2));
        assertThat(first.tong()).isGreaterThanOrEqualTo(24);assertThat(first.noiDung()).hasSize(12);assertThat(second.noiDung()).hasSize(12);
        var ids=new HashSet<Long>();first.noiDung().forEach(p->assertThat(ids.add(p.maSanPham())).isTrue());second.noiDung().forEach(p->assertThat(ids.add(p.maSanPham())).isTrue());
        var prices=new ArrayList<BigDecimal>();first.noiDung().forEach(p->prices.add(p.giaTu()));second.noiDung().forEach(p->prices.add(p.giaTu()));assertThat(prices).isSorted();
        assertThat(products.loc(filter("",null,null,"gia-tang",9999)).trang()).isEqualTo(first.soTrang());
        var laptop=new BoLocSanPham("","laptop","lenovo",null,null,16,512,"Intel","",true,"ten",1);
        var matches=products.loc(laptop);for(var p:matches.noiDung()){assertThat(p.thuongHieu()).isEqualTo("Lenovo");var sku=products.bienThe(p.duongDan()).stream().filter(v->v.maBienThe()==p.maSkuGoiY()).findFirst().orElseThrow();assertThat(sku.dungLuongRam()).isGreaterThanOrEqualTo(16);assertThat(sku.dungLuongLuuTru()).isGreaterThanOrEqualTo(512);}
    }
    @Test void filterPageRendersAndRejectsMalformedOrInvertedBounds()throws Exception{
        var http=HttpClient.newHttpClient();String prefix="http://127.0.0.1:"+port;
        var response=http.send(HttpRequest.newBuilder(URI.create(prefix+"/san-pham?q=iPhone%2013&luuTru=128&giaDen=14000000")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);assertThat(response.body()).contains("sku=1","1 sản phẩm","128");
        var invalid=http.send(HttpRequest.newBuilder(URI.create(prefix+"/san-pham?giaTu=20000000&giaDen=10000000")).GET().build(),HttpResponse.BodyHandlers.ofString());assertThat(invalid.statusCode()).isEqualTo(200);assertThat(invalid.body()).contains("Giá thấp nhất không được lớn hơn");
        var malformed=http.send(HttpRequest.newBuilder(URI.create(prefix+"/san-pham?ram=abc")).GET().build(),HttpResponse.BodyHandlers.ofString());assertThat(malformed.statusCode()).isEqualTo(200);assertThat(malformed.body()).contains("alert-danger");
    }
    @Test void actualProductImageIsPackagedAndPubliclyServed()throws Exception{
        var product=products.chiTiet("iphone-13");assertThat(product.anhDaiDien()).startsWith("/media/catalogue/");
        var images=products.anh("iphone-13",1);assertThat(images).hasSizeGreaterThan(1);assertThat(images.getFirst().get("DuongDanNguon")).isEqualTo("https://cellphones.com.vn/iphone-13.html");
        var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+product.anhDaiDien())).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
        assertThat(response.statusCode()).isEqualTo(200);assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("image/");assertThat(response.body().length).isGreaterThan(1000);
    }
}
