package vn.luma;

import java.net.URI;
import java.net.http.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import vn.luma.business.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false"})
class MoHinhIntegrationTest {
    @Autowired SanPhamService products;
    @Autowired JdbcClient jdbc;
    @Autowired ObjectMapper json;
    @LocalServerPort int port;
    @Test @SuppressWarnings("unchecked") void bothOriginalGlbsAreSelfContainedPackagedAndHaveValidHeaders()throws Exception{
        var http=HttpClient.newHttpClient();for(String slug:List.of("iphone-13","macbook-air-m2")){long sku=products.bienThe(slug).getFirst().maBienThe();var model=products.moHinh(slug,sku).orElseThrow();String path=(String)model.get("DuongDanTep");assertThat(model.get("ThongTinSuDung").toString()).contains("do dự án tạo","không đại diện chính xác");var response=http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());assertThat(response.statusCode()).isEqualTo(200);byte[] data=response.body();assertThat(new String(data,0,4,StandardCharsets.US_ASCII)).isEqualTo("glTF");var bytes=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);assertThat(bytes.getInt(4)).isEqualTo(2);assertThat(bytes.getInt(8)).isEqualTo(data.length);int jsonLength=bytes.getInt(12);assertThat(new String(data,16,4,StandardCharsets.US_ASCII)).isEqualTo("JSON");var document=json.readValue(Arrays.copyOfRange(data,20,20+jsonLength),Map.class);assertThat((Map<String,Object>)document.get("asset")).containsEntry("version","2.0");assertThat((List<Map<String,Object>>)document.get("buffers")).allMatch(b->!b.containsKey("uri"));assertThat(document.get("images")).isNull();assertThat((List<?>)document.get("meshes")).isNotEmpty();}
    }
    @Test @Transactional void modelMetadataFollowsPublicProductSkuAndMediaVisibility(){
        assertThat(products.moHinh("iphone-13",1)).isPresent();assertThatThrownBy(()->products.moHinh("iphone-13",3)).isInstanceOf(KhongTimThayException.class);jdbc.sql("UPDATE \"TepSanPham\" SET \"DangHienThi\"=false WHERE \"MaSanPham\"=1 AND \"LoaiTep\"='MO_HINH_3D'").update();assertThat(products.moHinh("iphone-13",1)).isEmpty();jdbc.sql("UPDATE \"SanPham\" SET \"DangHienThi\"=false WHERE \"MaSanPham\"=1").update();assertThatThrownBy(()->products.moHinh("iphone-13",1)).isInstanceOf(KhongTimThayException.class);
    }
}
