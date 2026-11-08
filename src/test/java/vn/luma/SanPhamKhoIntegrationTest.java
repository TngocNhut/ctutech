package vn.luma;

import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import vn.luma.business.*;
import vn.luma.web.CapNhatBienThe;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test",
    "app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false"})
class SanPhamKhoIntegrationTest {
    @LocalServerPort int port;
    @Autowired SanPhamService products;
    @Autowired KhoService stock;
    @Autowired JdbcClient jdbc;
    @Test void detailDisplaysSelectedSkuAndRejectsForeignSku()throws Exception{
        var client=HttpClient.newHttpClient();
        var detail=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/san-pham/iphone-13?sku=1")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertThat(detail.statusCode()).isEqualTo(200);assertThat(detail.body()).contains("DEMO-001-01","Apple A15","Chọn cấu hình");
        var wrong=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/san-pham/iphone-13?sku=24")).GET().build(),HttpResponse.BodyHandlers.ofString());assertThat(wrong.statusCode()).isEqualTo(404);
    }
    @Test @Transactional void administratorCannotLowerStockBelowReservationsAndWritesAtomicLedger(){
        var sku=products.bienThe("iphone-13").get(0);
        var change=new CapNhatBienThe(sku.giaBan().add(BigDecimal.ONE),sku.giaNiemYet(),sku.giaVon(),9,"Kiểm thử sổ kho");
        assertThatThrownBy(()->stock.capNhat("khach@luma.local",sku.maBienThe(),change)).isInstanceOf(AccessDeniedException.class);
        jdbc.sql("UPDATE \"TonKho\" SET \"SoLuongGiu\"=2 WHERE \"MaKhoHang\"=1 AND \"MaBienThe\"=:id").param("id",sku.maBienThe()).update();
        var invalid=new CapNhatBienThe(sku.giaBan(),sku.giaNiemYet(),sku.giaVon(),1,"Không được mất hàng giữ");
        assertThatThrownBy(()->stock.capNhat("admin@luma.local",sku.maBienThe(),invalid)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test @Transactional void validUpdateKeepsHistoryAndStockLedger(){
        var sku=products.bienThe("iphone-13").get(0);
        var change=new CapNhatBienThe(sku.giaBan().add(BigDecimal.ONE),sku.giaNiemYet(),sku.giaVon(),9,"Kiểm thử cập nhật");
        stock.capNhat("admin@luma.local",sku.maBienThe(),change);
        var saved=products.bienThe("iphone-13").get(0);assertThat(saved.giaBan()).isEqualByComparingTo(change.giaBan());assertThat(saved.soLuongTon()).isEqualTo(9);
        assertThat(jdbc.sql("SELECT count(*) FROM \"LichSuGiaBienThe\" WHERE \"MaBienThe\"=:id AND \"LyDo\"='Kiểm thử cập nhật'").param("id",sku.maBienThe()).query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT \"ThayDoiTon\" FROM \"BienDongKho\" WHERE \"MaBienThe\"=:id AND \"GhiChu\"='Kiểm thử cập nhật'").param("id",sku.maBienThe()).query(Integer.class).single()).isEqualTo(9-sku.soLuongTon());
    }
}
