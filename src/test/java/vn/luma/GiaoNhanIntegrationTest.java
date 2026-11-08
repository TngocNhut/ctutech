package vn.luma;

import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.business.*;
import vn.luma.data.HoaDonDAO;
import vn.luma.web.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test",
    "app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false"})
@Transactional
class GiaoNhanIntegrationTest {
    static final String ADMIN="admin@luma.local";
    @Autowired GiaoNhanService shipping;
    @Autowired DonHangService orders;
    @Autowired GioHangService carts;
    @Autowired TaiKhoanService users;
    @Autowired BanDoService maps;
    @Autowired HoaDonService invoices;
    @Autowired HoaDonPdf pdf;
    @Autowired JdbcClient jdbc;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @MockitoSpyBean HoaDonDAO invoiceDao;
    record Order(String email,String code,long id){}
    String user(){String email="ship-"+UUID.randomUUID()+"@example.test";users.seedDemo(email,"Nguyễn Thị Ánh","KhachHang","StrongDemo!2026");return email;}
    Order order(){
        String email=user();carts.sua(email,1,1,true);
        var address=new DiaChiYeuCau("Nguyễn Thị Ánh","0901234567","Thành phố Hồ Chí Minh","Phường Bến Nghé","123 Nguyễn Huệ","Gọi trước khi giao","",null,null,false);
        var form=new DatHangYeuCau(UUID.randomUUID().toString(),orders.noiDungGio(email),"",address);
        String code=orders.datCOD(email,form,maps.chuanHoa(address));
        return new Order(email,code,((Number)orders.chiTiet(email,code).get("MaDonHang")).longValue());
    }
    TaoVanDonYeuCau waybill(Order order){
        long carrier=jdbc.sql("SELECT \"MaDonViVanChuyen\" FROM \"DonViVanChuyen\" WHERE \"KyHieu\"='LUMA_NOI_BO'").query(Long.class).single();
        return new TaoVanDonYeuCau(carrier,"TEST-"+order.code(),new BigDecimal("26000"),null);
    }
    SuKienGiaoNhanYeuCau event(String action){return new SuKienGiaoNhanYeuCau(action,UUID.randomUUID().toString(),"Ghi nhận thử nghiệm "+action);}
    void dispatch(Order order){shipping.xacNhan(ADMIN,order.code(),true);shipping.taoVanDon(ADMIN,order.code(),waybill(order));shipping.capNhat(ADMIN,order.code(),event("BAN_GIAO"));}
    void deliver(Order order){dispatch(order);shipping.capNhat(ADMIN,order.code(),event("DA_GIAO"));}
    ThuCODYeuCau receipt(Order order){return new ThuCODYeuCau((BigDecimal)orders.chiTiet(ADMIN,order.code()).get("TongThanhToan"),"COD-"+order.code(),true);}
    int stock(String column){return jdbc.sql("SELECT \""+column+"\" FROM \"TonKho\" WHERE \"MaKhoHang\"=1 AND \"MaBienThe\"=1").query(Integer.class).single();}
    int count(String table,long order){return jdbc.sql("SELECT count(*) FROM \""+table+"\" WHERE \"MaDonHang\"=:d").param("d",order).query(Integer.class).single();}
    @Test void dispatchRequiresConfirmationOneParcelAndDeductsStockOnce(){
        Order order=order();int physical=stock("SoLuongTon"),held=stock("SoLuongGiu");
        assertThatThrownBy(()->shipping.taoVanDon(ADMIN,order.code(),waybill(order))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->shipping.xacNhan(ADMIN,order.code(),false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->shipping.xacNhan(order.email(),order.code(),true)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        shipping.xacNhan(ADMIN,order.code(),true);shipping.xacNhan(ADMIN,order.code(),true);
        var waybill=waybill(order);shipping.taoVanDon(ADMIN,order.code(),waybill);shipping.taoVanDon(ADMIN,order.code(),waybill);
        assertThat(count("VanDon",order.id())).isEqualTo(1);
        assertThatThrownBy(()->shipping.taoVanDon(ADMIN,order.code(),new TaoVanDonYeuCau(waybill.maDonViVanChuyen(),"OTHER",null,null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->shipping.capNhat(ADMIN,order.code(),event("DA_GIAO"))).isInstanceOf(IllegalArgumentException.class);
        var handoff=event("BAN_GIAO");shipping.capNhat(ADMIN,order.code(),handoff);shipping.capNhat(ADMIN,order.code(),handoff);shipping.capNhat(ADMIN,order.code(),event("BAN_GIAO"));
        assertThat(stock("SoLuongTon")).isEqualTo(physical-1);assertThat(stock("SoLuongGiu")).isEqualTo(held-1);
        assertThat(orders.chiTiet(order.email(),order.code())).containsEntry("TrangThaiKho","DA_XUAT");
        assertThatThrownBy(()->orders.huy(order.email(),order.code())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->shipping.capNhat(ADMIN,order.code(),new SuKienGiaoNhanYeuCau("GIAO_THAT_BAI",handoff.khoaSuKien(),handoff.ghiChu()))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->shipping.thuCOD(ADMIN,order.code(),receipt(order))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void deliveryDoesNotCollectCodAndReceiptAndInvoiceAreIdempotent()throws Exception{
        Order order=order();deliver(order);
        assertThat(orders.chiTiet(order.email(),order.code())).containsEntry("TrangThaiThanhToan","CHUA_THU").containsEntry("TrangThaiDon","CHO_GIAO_HANG");
        assertThat(invoices.daPhatHanh(order.email(),order.code())).isFalse();
        assertThatThrownBy(()->shipping.thuCOD(ADMIN,order.code(),new ThuCODYeuCau(BigDecimal.ONE,"WRONG",true))).isInstanceOf(IllegalArgumentException.class);
        var receipt=receipt(order);shipping.thuCOD(ADMIN,order.code(),receipt);shipping.thuCOD(ADMIN,order.code(),receipt);
        assertThat(count("PhienThanhToan",order.id())).isEqualTo(1);assertThat(count("HoaDon",order.id())).isEqualTo(1);
        assertThat(orders.chiTiet(order.email(),order.code())).containsEntry("TrangThaiDon","HOAN_THANH").containsEntry("TrangThaiThanhToan","DA_THU");
        assertThatThrownBy(()->shipping.thuCOD(ADMIN,order.code(),new ThuCODYeuCau(receipt.soTien(),"OTHER",true))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->invoices.tai(user(),order.code())).isInstanceOf(KhongTimThayException.class);
        byte[] document=invoices.tai(order.email(),order.code());
        try(var parsed=Loader.loadPDF(document)){
            String text=new PDFTextStripper().getText(parsed);assertThat(text).contains("Nguyễn Thị Ánh","123 Nguyễn Huệ",order.code(),"KHÔNG XÁC NHẬN CHUYỂN TIỀN THẬT");assertThat(parsed.getNumberOfPages()).isEqualTo(1);
        }
        Path output=Path.of(".local/qa/invoice-demo.pdf");Files.createDirectories(output.getParent());Files.write(output,document);
        jdbc.sql("UPDATE \"DonHang\" SET \"TenNguoiNhan\"='Changed after invoice',\"DiaChiNhanCuThe\"='Changed address' WHERE \"MaDonHang\"=:d").param("d",order.id()).update();
        try(var parsed=Loader.loadPDF(invoices.tai(order.email(),order.code()))){assertThat(new PDFTextStripper().getText(parsed)).contains("Nguyễn Thị Ánh","123 Nguyễn Huệ").doesNotContain("Changed after invoice","Changed address");}
    }
    @Test void deliveryFailureCreatesWorkAndRetryDoesNotDeductStockAgain(){
        Order order=order();dispatch(order);int physical=stock("SoLuongTon");var failure=event("GIAO_THAT_BAI");
        shipping.capNhat(ADMIN,order.code(),failure);shipping.capNhat(ADMIN,order.code(),failure);
        long waybill=((Number)shipping.vanDon(order.email(),order.code()).orElseThrow().get("MaVanDon")).longValue();
        var tasks=shipping.viec(ADMIN).stream().filter(t->((Number)t.get("MaVanDon")).longValue()==waybill).toList();assertThat(tasks).hasSize(1);
        long task=((Number)tasks.getFirst().get("MaViecXuLy")).longValue();shipping.nhanViec(ADMIN,task);
        shipping.capNhat(ADMIN,order.code(),event("GIAO_LAI"));shipping.capNhat(ADMIN,order.code(),event("CHO_GIAO_HANG"));shipping.capNhat(ADMIN,order.code(),event("DA_GIAO"));
        assertThat(stock("SoLuongTon")).isEqualTo(physical);
        assertThat(jdbc.sql("SELECT \"TrangThai\" FROM \"ViecXuLyGiaoNhan\" WHERE \"MaViecXuLy\"=:id").param("id",task).query(String.class).single()).isEqualTo("DA_XONG");
        assertThat(shipping.lichSu(order.email(),order.code())).hasSize(6);
        assertThatThrownBy(()->shipping.capNhat(ADMIN,order.code(),event("GIAO_THAT_BAI"))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void invoiceSupportsMultiplePagesAndLongVietnameseNames()throws Exception{
        var invoice=new HashMap<String,Object>();invoice.put("SoHoaDon","TEST-PAGES");invoice.put("MaDonCongKhai","TEST");invoice.put("NgayIn","08/11/2026");invoice.put("NguoiNhan","Nguyễn Thị Ánh");invoice.put("DiaChi","123 Nguyễn Huệ, Phường Bến Nghé, Thành phố Hồ Chí Minh");invoice.put("PhiGiao",new BigDecimal("30000"));invoice.put("TongTien",new BigDecimal("980030000"));
        List<Map<String,Object>> lines=new ArrayList<>();for(int i=0;i<70;i++)lines.add(Map.of("TenMatHang","Điện thoại thử nghiệm tiếng Việt đầy đủ dấu và tên cấu hình dài "+i,"MaHangLucDat","LUMA-TEST-"+i,"SoLuong",2,"DonGia",new BigDecimal("7000000"),"GiamGia",BigDecimal.ZERO));
        byte[] bytes=pdf.tao(invoice,lines);try(var parsed=Loader.loadPDF(bytes)){assertThat(parsed.getNumberOfPages()).isGreaterThan(2);String text=new PDFTextStripper().getText(parsed);assertThat(text).contains("cấu hình dài 69","980.030.000");}
        Files.write(Path.of(".local/qa/invoice-pagination.pdf"),bytes);
    }
    @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void invoiceFailureRollsBackReceiptAndOrderCompletion(){
        int physical=stock("SoLuongTon"),held=stock("SoLuongGiu");long[] oid={0};
        doThrow(new IllegalStateException("Injected invoice failure")).when(invoiceDao).tao(anyLong());
        try{
            var transaction=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
            assertThatThrownBy(()->transaction.execute(status->{Order order=order();oid[0]=order.id();deliver(order);shipping.thuCOD(ADMIN,order.code(),receipt(order));return null;}))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Injected invoice failure");
        }finally{reset(invoiceDao);}
        assertThat(oid[0]).isPositive();assertThat(count("PhienThanhToan",oid[0])).isZero();assertThat(count("HoaDon",oid[0])).isZero();
        assertThat(stock("SoLuongTon")).isEqualTo(physical);assertThat(stock("SoLuongGiu")).isEqualTo(held);
    }
    @Test void shippingAdjustmentsAreIdempotentAndCannotMakeCostNegative(){
        Order order=order();dispatch(order);BigDecimal fee=(BigDecimal)orders.chiTiet(ADMIN,order.code()).get("PhiVanChuyen");
        var extra=new PhiVanChuyenYeuCau(UUID.randomUUID().toString(),"PHU_PHI",new BigDecimal("5000"),"Phụ phí giao lại");shipping.themCuoc(ADMIN,order.code(),extra);shipping.themCuoc(ADMIN,order.code(),extra);
        shipping.themCuoc(ADMIN,order.code(),new PhiVanChuyenYeuCau(UUID.randomUUID().toString(),"GIAM_CUOC",new BigDecimal("1000"),"Điều chỉnh giảm"));
        assertThat(shipping.tongCuoc(ADMIN,order.code())).isEqualByComparingTo("30000");assertThat(shipping.cuoc(ADMIN,order.code())).hasSize(3);
        assertThat(orders.chiTiet(ADMIN,order.code()).get("PhiVanChuyen")).isEqualTo(fee);
        assertThatThrownBy(()->shipping.themCuoc(ADMIN,order.code(),new PhiVanChuyenYeuCau(UUID.randomUUID().toString(),"GIAM_CUOC",new BigDecimal("30001"),"Vượt cước đã ghi"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->shipping.themCuoc(ADMIN,order.code(),new PhiVanChuyenYeuCau(UUID.randomUUID().toString(),"CUOC_GOC",BigDecimal.ZERO,"Không được ghi cước gốc lần hai"))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void overdueShipmentCreatesOneTaskAndSuccessfulDeliveryClosesIt(){
        Order order=order();shipping.xacNhan(ADMIN,order.code(),true);var carrier=waybill(order);
        shipping.taoVanDon(ADMIN,order.code(),new TaoVanDonYeuCau(carrier.maDonViVanChuyen(),carrier.maTheoDoi(),null,java.time.LocalDateTime.now().minusDays(1)));
        shipping.capNhat(ADMIN,order.code(),event("BAN_GIAO"));assertThat(shipping.quaHan()).contains(order.code());
        shipping.taoViecQuaHan(order.code());shipping.taoViecQuaHan(order.code());
        long waybill=((Number)shipping.vanDon(ADMIN,order.code()).orElseThrow().get("MaVanDon")).longValue();
        assertThat(shipping.viec(ADMIN).stream().filter(t->t.get("MaVanDon")!=null&&((Number)t.get("MaVanDon")).longValue()==waybill)).hasSize(1);
        shipping.capNhat(ADMIN,order.code(),event("DA_GIAO"));shipping.taoViecQuaHan(order.code());assertThat(shipping.quaHan()).doesNotContain(order.code());
        assertThat(shipping.viec(ADMIN).stream().filter(t->t.get("MaVanDon")!=null&&((Number)t.get("MaVanDon")).longValue()==waybill)).isEmpty();
    }
    @Test void manualTaskNotesDeadlineAndClosureRespectAssigneeAndKeepBusinessStates(){
        var order=order();dispatch(order);shipping.capNhat(ADMIN,order.code(),event("GIAO_THAT_BAI"));
        var task=shipping.viec(ADMIN).stream().filter(v->order.code().equals(v.get("MaDonCongKhai"))).findFirst().orElseThrow();long id=((Number)task.get("MaViecXuLy")).longValue();shipping.nhanViec(ADMIN,id);
        String other="task-admin-"+UUID.randomUUID()+"@example.test";users.seedDemo(other,"Quản trị thử","QuanTri","StrongDemo!2026");
        var update=new ViecGiaoNhanYeuCau("Đã liên hệ khách, hẹn kiểm tra lại",java.time.LocalDateTime.now().plusDays(1).withNano(0),false);
        assertThatThrownBy(()->shipping.suaViec(other,id,update)).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->shipping.viec(order.email(),id)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        int physical=stock("SoLuongTon");var before=orders.chiTiet(ADMIN,order.code());shipping.suaViec(ADMIN,id,update);
        assertThat(shipping.viec(ADMIN,id).get("GhiChu")).isEqualTo(task.get("GhiChu"));assertThat(shipping.ghiChuViec(ADMIN,id,1)).anyMatch(n->update.ghiChu().equals(n.get("GhiChu")));
        shipping.suaViec(ADMIN,id,new ViecGiaoNhanYeuCau("Đã xử lý việc theo dõi; chờ nghiệp vụ giao lại riêng",null,true));
        assertThat(shipping.viec(ADMIN,id)).containsEntry("TrangThai","DA_XONG");assertThat(shipping.viec(ADMIN,id).get("NgayDong")).isNotNull();assertThat(shipping.viec(ADMIN,false,1)).noneMatch(v->((Number)v.get("MaViecXuLy")).longValue()==id);assertThat(shipping.viec(ADMIN,true,1)).anyMatch(v->((Number)v.get("MaViecXuLy")).longValue()==id);
        assertThat(orders.chiTiet(ADMIN,order.code())).isEqualTo(before);assertThat(stock("SoLuongTon")).isEqualTo(physical);assertThatThrownBy(()->shipping.suaViec(ADMIN,id,update)).isInstanceOf(IllegalArgumentException.class);
        shipping.capNhat(ADMIN,order.code(),event("GIAO_LAI"));shipping.capNhat(ADMIN,order.code(),event("GIAO_THAT_BAI"));assertThat(shipping.viec(ADMIN)).anyMatch(v->order.code().equals(v.get("MaDonCongKhai"))&&((Number)v.get("MaViecXuLy")).longValue()!=id);assertThat(stock("SoLuongTon")).isEqualTo(physical);
    }
    @Test void manualClosureDoesNotRecreateSameOverdueIssueUntilShipmentChanges(){
        var order=order();dispatch(order);long waybill=((Number)shipping.vanDon(ADMIN,order.code()).orElseThrow().get("MaVanDon")).longValue();
        jdbc.sql("UPDATE \"VanDon\" SET \"NgayDuKienGiao\"=clock_timestamp()-interval '1 hour',\"NgayCapNhat\"=clock_timestamp() WHERE \"MaVanDon\"=:id").param("id",waybill).update();shipping.taoViecQuaHan(order.code());
        long task=((Number)shipping.viec(ADMIN).stream().filter(v->order.code().equals(v.get("MaDonCongKhai"))).findFirst().orElseThrow().get("MaViecXuLy")).longValue();shipping.suaViec(ADMIN,task,new ViecGiaoNhanYeuCau("Đã liên hệ đối tác, kết thúc việc kiểm tra lần này",null,true));shipping.taoViecQuaHan(order.code());
        assertThat(jdbc.sql("SELECT count(*) FROM \"ViecXuLyGiaoNhan\" WHERE \"MaVanDon\"=:id AND \"LoaiViec\"='QUA_HAN_GIAO'").param("id",waybill).query(Integer.class).single()).isEqualTo(1);
        jdbc.sql("UPDATE \"VanDon\" SET \"NgayCapNhat\"=clock_timestamp() WHERE \"MaVanDon\"=:id").param("id",waybill).update();shipping.taoViecQuaHan(order.code());assertThat(jdbc.sql("SELECT count(*) FROM \"ViecXuLyGiaoNhan\" WHERE \"MaVanDon\"=:id AND \"LoaiViec\"='QUA_HAN_GIAO'").param("id",waybill).query(Integer.class).single()).isEqualTo(2);
    }
}
