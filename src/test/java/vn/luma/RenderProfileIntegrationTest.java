package vn.luma;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import vn.luma.business.CauHinhPayOSService;
import static org.assertj.core.api.Assertions.*;
@ActiveProfiles("render")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false"})
class RenderProfileIntegrationTest {
 @Autowired Environment env;@Autowired CauHinhPayOSService settings;
 @Test void renderBindsPublicPortAndDisablesLocalSecretsAndDemoAccounts(){assertThat(env.getProperty("server.address")).isEqualTo("0.0.0.0");assertThat(env.getProperty("server.servlet.session.cookie.secure",Boolean.class)).isTrue();assertThat(env.getProperty("app.demo.enabled",Boolean.class)).isFalse();assertThat(settings.trangThai("admin@luma.local").choPhepLuuTep()).isFalse();assertThatThrownBy(()->settings.luu("admin@luma.local",false,"http://localhost:8082","","","")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Render");}
}
