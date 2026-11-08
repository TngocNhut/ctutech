package vn.luma.web;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import vn.luma.business.*;
import vn.luma.config.ThanhToanCauHinh;
import vn.luma.model.DiaChiChot;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class DatHangPayOSTest {
 @Test void createsPayOSLinkAfterOrderAndPreservesOrderWhenProviderFails(){
  var carts=mock(GioHangService.class);var orders=mock(DonHangService.class);var users=mock(TaiKhoanService.class);var maps=mock(BanDoService.class);var addresses=mock(DiaChiService.class);var payments=mock(ThanhToanService.class);
  var controller=new DatHangController(carts,orders,users,maps,addresses,payments,mock(ThanhToanCauHinh.class),mock(GiaoNhanService.class),mock(HoaDonService.class),mock(TraHangService.class),mock(HoanTienService.class));
  var request=mock(DatHangYeuCau.class);var address=mock(DiaChiYeuCau.class);var normalized=mock(DiaChiChot.class);when(request.diaChi()).thenReturn(address);when(request.phuongThuc()).thenReturn("PAYOS");when(maps.chuanHoa(address)).thenReturn(normalized);when(orders.datHang("customer@example.test",request,normalized)).thenReturn("LM-EXAMPLE");
  var flash=new RedirectAttributesModelMap();var principal=(java.security.Principal)()->"customer@example.test";var errors=new BeanPropertyBindingResult(request,"form");
  assertThat(controller.dat(request,errors,"dat",principal,new ConcurrentModel(),flash)).isEqualTo("redirect:/thanh-toan/LM-EXAMPLE");verify(payments).taoLink("customer@example.test","LM-EXAMPLE");
  doThrow(new IllegalArgumentException("Chưa tạo được liên kết")).when(payments).taoLink("customer@example.test","LM-EXAMPLE");var failure=new RedirectAttributesModelMap();
  assertThat(controller.dat(request,errors,"dat",principal,new ConcurrentModel(),failure)).isEqualTo("redirect:/thanh-toan/LM-EXAMPLE");assertThat(failure.getFlashAttributes().get("loi")).isEqualTo("Chưa tạo được liên kết");
 }
}
