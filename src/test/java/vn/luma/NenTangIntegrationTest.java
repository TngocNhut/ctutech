package vn.luma;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import vn.luma.business.SanPhamService;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test",
    "app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false"})
class NenTangIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcClient jdbc;
    @Autowired SanPhamService sanPham;

    @Test void schemaAndSeedMatchTheDesign() {
        assertThat(jdbc.sql("SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' AND table_name <> 'flyway_schema_history'").query(Integer.class).single()).isEqualTo(64);
        assertThat(sanPham.danhSach("", "")).hasSize(100);
        assertThat(jdbc.sql("SELECT count(*) FROM \"BienTheSanPham\"").query(Integer.class).single()).isGreaterThan(172);
        assertThat(jdbc.sql("SELECT count(*) FROM \"TonKho\" WHERE \"SoLuongGiu\" > \"SoLuongTon\"").query(Integer.class).single()).isZero();
    }

    @Test void catalogueRendersFromDatabaseAndAdminRequiresLogin() throws Exception {
        var client = HttpClient.newHttpClient();
        var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("iPhone 13", "LUMA", "sản phẩm");
        var admin = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/quan-tri")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(admin.statusCode()).isEqualTo(302);
        assertThat(admin.headers().firstValue("Location").orElse("")).endsWith("/login");
    }
}
