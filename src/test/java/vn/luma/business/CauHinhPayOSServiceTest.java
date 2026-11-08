package vn.luma.business;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import vn.luma.config.ThanhToanCauHinh;
import vn.luma.data.QuanLySanPhamDAO;
import vn.luma.model.TaiKhoan;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class CauHinhPayOSServiceTest {
 @TempDir Path dir;
 @Test void secretsStayPrivateAndPendingChangesKeepPreviousValues() throws Exception {
  var users=mock(TaiKhoanService.class);var user=mock(TaiKhoan.class);when(user.maNguoiDung()).thenReturn(7L);when(users.quanTri("admin")).thenReturn(user);
  var jdbc=mock(JdbcClient.class);var statement=mock(JdbcClient.StatementSpec.class);@SuppressWarnings("unchecked") var query=(JdbcClient.MappedQuerySpec<Boolean>)mock(JdbcClient.MappedQuerySpec.class);when(jdbc.sql(anyString())).thenReturn(statement);when(statement.query(Boolean.class)).thenReturn(query);when(query.single()).thenReturn(false);
  var file=dir.resolve("payos-local.properties");var config=new ThanhToanCauHinh("DISABLED","","","",false,"http://localhost:8082");var service=new CauHinhPayOSService(users,config,mock(QuanLySanPhamDAO.class),jdbc,file.toString());service.fileConfigEnabled=true;
  assertThatThrownBy(()->service.luu("admin",true,"http://localhost:8082","id","api","key")).isInstanceOf(IllegalArgumentException.class);assertThat(Files.exists(file)).isFalse();
  service.luu("admin",true,"https://shop.example","id-123","api-secret","checksum-secret");var state=service.trangThai("admin");assertThat(state.coBanChoKhoiDongLai()).isTrue();assertThat(state.dangBat()).isFalse();assertThat(state.batDuKien()).isTrue();assertThat(state.toString()).doesNotContain("api-secret","checksum-secret","id-123");
  service.luu("admin",true,"https://shop.example","","","");var properties=new java.util.Properties();try(var in=Files.newInputStream(file)){properties.load(in);}assertThat(properties.getProperty("app.payos.api-key")).isEqualTo("api-secret");assertThat(properties.getProperty("app.payos.checksum-key")).isEqualTo("checksum-secret");
  service.fileConfigEnabled=false;assertThatThrownBy(()->service.luu("admin",true,"https://shop.example","","","")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Render");service.fileConfigEnabled=true;
  when(query.single()).thenReturn(true);assertThatThrownBy(()->service.luu("admin",false,"https://shop.example","","","")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("đối soát");
 }
}
