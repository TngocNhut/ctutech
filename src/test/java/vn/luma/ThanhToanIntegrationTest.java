package vn.luma;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.luma.business.*;
import vn.luma.data.ThanhToanDAO;
import vn.luma.model.BaoThu;
import vn.luma.web.*;
import static vn.luma.data.ThanhToanDAO.id;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.mail.local-directory=.local/test-mail",
    "app.scheduling.enabled=false","app.payos.simulation-enabled=true","app.payos.mode=LIVE","app.payos.client-id=test-id","app.payos.api-key=test-api","app.payos.checksum-key=test-checksum"})
class ThanhToanIntegrationTest {
    @LocalServerPort int port;
    @Autowired ThanhToanService payments;@Autowired ThanhToanGhiSoService ledger;@MockitoSpyBean ThanhToanDAO dao;
    @Autowired DonHangService orders;@Autowired GioHangService carts;@Autowired TaiKhoanService users;@Autowired BanDoService maps;@Autowired JdbcClient jdbc;
    @MockitoSpyBean PayOSClient client;
    @Autowired DoiSoatService reconciliation;
    @Autowired ThongBaoService notifications;
    final List<Map.Entry<String,String>> created=new ArrayList<>();
    record Fixture(String email,String code,Map<String,Object> session,BigDecimal due){}
    @BeforeEach void noExternalRequests(){
        doThrow(new AssertionError("Unstubbed create network call")).when(client).tao(anyLong(),any(),any(),any());
        doThrow(new AssertionError("Unstubbed query network call")).when(client).tra(anyLong());
        doThrow(new AssertionError("Unstubbed cancel network call")).when(client).huy(anyLong());
    }
    @AfterEach void releaseHolds(){reset(dao);for(var e:created)ledger.ketThuc(e.getKey(),e.getValue(),false);}
    Fixture fixture(String method){
        String email="pay-"+UUID.randomUUID()+"@example.test";users.seedDemo(email,"Payment test","KhachHang","PaymentDemo!2026");carts.sua(email,1,1,true);
        var address=DiaChiYeuCau.trong("Payment test","0901234567");address=new DiaChiYeuCau(address.tenNguoiNhan(),address.soDienThoai(),"TP.HCM","Phường thử","123 Đường thử","","",null,null,false);
        var request=new DatHangYeuCau(UUID.randomUUID().toString(),orders.noiDungGio(email),"",address,method);
        String code=orders.datHang(email,request,maps.chuanHoa(address));created.add(Map.entry(email,code));var session=payments.phien(email,code);
        return new Fixture(email,code,session,(BigDecimal)session.get("SoTienYeuCau"));
    }
    BaoThu receipt(Fixture f,BigDecimal amount,String ref){return new BaoThu(id(f.session,"MaDonPayOS"),"mock-"+id(f.session,"MaPhienThanhToan"),ref,amount,"VND",OffsetDateTime.now().withNano(0));}
    void receive(Fixture f,BaoThu receipt){ledger.xuLy(ledger.nhanDaXacMinh((String)f.session.get("MaKenhThanhToan"),receipt));}
    @Test void partialThenExactPaymentIsCountedOnceAndDoesNotShipStock(){
        var f=fixture("PAYOS_MO_PHONG");payments.taoLink(f.email,f.code);var first=receipt(f,new BigDecimal("1000000"),"A-"+UUID.randomUUID());receive(f,first);receive(f,first);
        assertThat(orders.chiTiet(f.email,f.code)).containsEntry("TrangThaiThanhToan","THU_THIEU");
        receive(f,receipt(f,f.due.subtract(first.amount()),"B-"+UUID.randomUUID()));
        assertThat(orders.chiTiet(f.email,f.code)).containsEntry("TrangThaiThanhToan","DA_THU").containsEntry("TrangThaiKho","DANG_GIU");
        assertThat(notifications.danhSach(f.email,false,1)).isNotEmpty().allMatch(n->((String)n.get("TieuDe")).startsWith("Mô phỏng ·"));
        assertThat(payments.daThu(f.email,f.code)).isEqualByComparingTo(f.due);
        long order=id(f.session,"MaDonHang");assertThat(jdbc.sql("SELECT count(*) FROM \"HoaDon\" WHERE \"MaDonHang\"=:d").param("d",order).query(Integer.class).single()).isEqualTo(1);
        receive(f,first);assertThat(jdbc.sql("SELECT count(*) FROM \"GiaoDichThanhToan\" WHERE \"MaPhienThanhToan\"=:p").param("p",id(f.session,"MaPhienThanhToan")).query(Integer.class).single()).isEqualTo(2);
    }
    @Test void administratorReconciliationViewIncludesConflictReceiptsAndRefundsWithoutChangingLedger()throws Exception{
        var f=fixture("PAYOS_MO_PHONG");payments.taoLink(f.email,f.code);String ref="VIEW-"+UUID.randomUUID();
        receive(f,receipt(f,f.due.add(new BigDecimal("1234")),ref));receive(f,receipt(f,f.due.add(new BigDecimal("1235")),ref));
        long sid=id(f.session,"MaPhienThanhToan");var before=dao.phien(sid);
        var view=reconciliation.chiTiet("admin@luma.local",sid,1,1);assertThat(view.khoanThu()).hasSize(1);assertThat(view.suKien()).hasSize(2);
        assertThat(view.phien()).containsEntry("MaLoi","THAM_CHIEU_XUNG_DOT");assertThat(view.daThu()).isEqualByComparingTo(f.due.add(new BigDecimal("1234")));assertThat(view.giuHoan()).isEqualByComparingTo("1234");assertThat(view.daHoan()).isZero();
        assertThat(reconciliation.danhSach("admin@luma.local","CAN_KIEM_TRA",f.code,1)).hasSize(1);assertThat(reconciliation.dem("admin@luma.local","TAT_CA",f.code)).isEqualTo(1);
        assertThat(dao.phien(sid)).isEqualTo(before);assertThatThrownBy(()->reconciliation.chiTiet(f.email,sid,1,1)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        reconciliation.ghiChu("admin@luma.local",sid,"Đã kiểm tra bằng chứng thử; cần tiếp tục đối chiếu");
        assertThat(dao.phien(sid)).isEqualTo(before);assertThat(reconciliation.chiTiet("admin@luma.local",sid,1,1).lichSu()).anyMatch(h->"Đã kiểm tra bằng chứng thử; cần tiếp tục đối chiếu".equals(h.get("GhiChuQuanTri")));
        assertThatThrownBy(()->reconciliation.danhSach("admin@luma.local","invalid","",1)).isInstanceOf(IllegalArgumentException.class);
        var unknown=new BaoThu(8999999999999000L,"unknown-test-link","UNKNOWN-"+UUID.randomUUID(),new BigDecimal("1"),"VND",OffsetDateTime.now());long event=ledger.nhanDaXacMinh((String)f.session.get("MaKenhThanhToan"),unknown);ledger.xuLy(event);
        assertThat(reconciliation.chuaGan("admin@luma.local",1)).anyMatch(e->id(e,"MaSuKien")==event);
        var browser=java.net.http.HttpClient.newBuilder().cookieHandler(new java.net.CookieManager(null,java.net.CookiePolicy.ACCEPT_ALL)).build();String base="http://127.0.0.1:"+port;
        String html=browser.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+"/login")).GET().build(),java.net.http.HttpResponse.BodyHandlers.ofString()).body();var csrf=java.util.regex.Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);assertThat(csrf.find()).isTrue();
        var login=java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+"/login")).header("Content-Type","application/x-www-form-urlencoded").POST(java.net.http.HttpRequest.BodyPublishers.ofString("username=admin%40luma.local&password=LumaDemo%212026&_csrf="+java.net.URLEncoder.encode(csrf.group(1),java.nio.charset.StandardCharsets.UTF_8))).build();assertThat(browser.send(login,java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(302);
        for(String path:List.of("/quan-tri/doi-soat?q="+f.code,"/quan-tri/doi-soat/"+sid,"/quan-tri/doi-soat/su-kien-chua-gan")){
            var rendered=browser.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+path)).header("Accept","text/html").GET().build(),java.net.http.HttpResponse.BodyHandlers.ofString());assertThat(rendered.statusCode()).as(path).isEqualTo(200);
        }
        assertThat(dao.phien(sid)).isEqualTo(before);
    }
    @Test void excessQueuesOnlyTheExcessAndWaitsForReconciliation(){
        var f=fixture("PAYOS_MO_PHONG");receive(f,receipt(f,f.due.add(new BigDecimal("1234")),"EX-"+UUID.randomUUID()));
        assertThat(orders.chiTiet(f.email,f.code)).containsEntry("TrangThaiThanhToan","CAN_DOI_SOAT");
        assertThat(refundTotal(f)).isEqualByComparingTo("1234");
        ledger.ketThuc(f.email,f.code,false);assertThat(refundTotal(f)).isEqualByComparingTo(f.due.add(new BigDecimal("1234")));
    }
    BigDecimal refundTotal(Fixture f){return jdbc.sql("SELECT coalesce(sum(h.\"SoTienHoan\"),0) FROM \"HoanTien\" h JOIN \"GiaoDichThanhToan\" g USING (\"MaGiaoDich\") WHERE g.\"MaPhienThanhToan\"=:p").param("p",id(f.session,"MaPhienThanhToan")).query(BigDecimal.class).single();}
    @Test void lateCashExpiresOrderAndQueuesFullRefundWithoutRevival(){
        var f=fixture("PAYOS_MO_PHONG");jdbc.sql("UPDATE \"DonHang\" SET \"HanXuLy\"=now()-interval '1 second' WHERE \"MaDonCongKhai\"=:c").param("c",f.code).update();
        var receipt=receipt(f,f.due,"LATE-"+UUID.randomUUID());receive(f,receipt);receive(f,receipt);
        assertThat(orders.chiTiet(f.email,f.code)).containsEntry("TrangThaiDon","HET_HAN").containsEntry("TrangThaiThanhToan","CHO_HOAN").containsEntry("TrangThaiKho","DA_TRA_GIU");assertThat(refundTotal(f)).isEqualByComparingTo(f.due);
    }
    @Test void conflictingReferenceCannotIncreaseReceiptsOrConfirmOrder(){
        var f=fixture("PAYOS_MO_PHONG");String reference="CONFLICT-"+UUID.randomUUID();receive(f,receipt(f,BigDecimal.ONE,reference));receive(f,receipt(f,f.due,reference));
        assertThat(payments.daThu(f.email,f.code)).isEqualByComparingTo("1");assertThat(orders.chiTiet(f.email,f.code)).containsEntry("TrangThaiThanhToan","CAN_DOI_SOAT");
        ledger.ghiLink(id(f.session,"MaPhienThanhToan"),"mock-"+id(f.session,"MaPhienThanhToan"),"/thanh-toan/"+f.code,"PENDING");
        assertThat(payments.phien(f.email,f.code)).containsEntry("MaLoi","THAM_CHIEU_XUNG_DOT");
    }
    @Test void acceptedPaymentIsPreservedWhenAnotherReceiptArrives(){
        var f=fixture("PAYOS_MO_PHONG");receive(f,receipt(f,f.due,"EXACT-"+UUID.randomUUID()));receive(f,receipt(f,new BigDecimal("5000"),"EXTRA-"+UUID.randomUUID()));
        assertThat(orders.chiTiet(f.email,f.code)).containsEntry("TrangThaiThanhToan","DA_THU");assertThat(refundTotal(f)).isEqualByComparingTo("5000");
    }
    @Test void cancelledOrderReceivesLateMoneyAsRefundAndCannotSwitchModes(){
        var f=fixture("PAYOS_MO_PHONG");ledger.ketThuc(f.email,f.code,false);receive(f,receipt(f,f.due,"CANCELLED-"+UUID.randomUUID()));
        assertThat(orders.chiTiet(f.email,f.code)).containsEntry("TrangThaiDon","DA_HUY").containsEntry("TrangThaiThanhToan","CHO_HOAN");assertThat(refundTotal(f)).isEqualByComparingTo(f.due);
        var real=fixture("PAYOS");assertThatThrownBy(()->payments.moPhong(real.email,real.code,BigDecimal.ONE,UUID.randomUUID().toString())).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void timeoutUsesSameCodeAndQueriesExistingLinkOutsideDatabaseTransaction(){
        var f=fixture("PAYOS");long orderCode=id(f.session,"MaDonPayOS");
        doAnswer(call->{assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();throw new IllegalStateException("simulated timeout");}).when(client).tao(eq(orderCode),any(),any(),eq(f.code));
        assertThatThrownBy(()->payments.taoLink(f.email,f.code)).isInstanceOf(IllegalArgumentException.class);
        doAnswer(call->{assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();return new PayOSClient.DoiSoat(orderCode,"verified-"+orderCode,f.due,BigDecimal.ZERO,"PENDING",List.of());}).when(client).tra(orderCode);
        payments.taoLink(f.email,f.code);verify(client,times(1)).tao(eq(orderCode),any(),any(),eq(f.code));verify(client,times(1)).tra(orderCode);
        assertThat(payments.phien(f.email,f.code)).containsEntry("MaDonPayOS",orderCode).containsEntry("MaLienKetPayOS","verified-"+orderCode);
        assertThat(payments.phien(f.email,f.code)).containsEntry("TrangThaiXuLy","CHO_THANH_TOAN");
    }
    @Test void durableInboxSurvivesReceiptTransactionFailure(){
        var f=fixture("PAYOS_MO_PHONG");var receipt=receipt(f,f.due,"RETRY-"+UUID.randomUUID());long event=ledger.nhanDaXacMinh("PAYOS_LUMA_MOCK",receipt);
        doThrow(new IllegalStateException("simulated crash before event completion")).when(dao).xongSuKien(eq(event),anyLong(),any(),eq("DA_XU_LY"));
        try{assertThatThrownBy(()->ledger.xuLy(event)).isInstanceOf(IllegalStateException.class);}finally{reset(dao);}
        assertThat(payments.daThu(f.email,f.code)).isEqualByComparingTo(BigDecimal.ZERO);assertThat(dao.suKien(event,false)).containsEntry("TrangThaiXuLy","CHO_XU_LY");
        ledger.xuLy(event);assertThat(payments.daThu(f.email,f.code)).isEqualByComparingTo(f.due);assertThat(dao.suKien(event,false)).containsEntry("TrangThaiXuLy","DA_XU_LY");
    }
    @Test void apiQueryCannotCountSameBankReferenceTwice(){
        var f=fixture("PAYOS");var receipt=receipt(f,f.due,"API-"+UUID.randomUUID());
        doReturn(new PayOSClient.DoiSoat(id(f.session,"MaDonPayOS"),receipt.linkId(),f.due,f.due,"PAID",List.of(receipt))).when(client).tra(id(f.session,"MaDonPayOS"));
        payments.doiSoat(f.email,f.code);payments.doiSoat(f.email,f.code);verify(client,times(1)).tra(id(f.session,"MaDonPayOS"));
        jdbc.sql("UPDATE \"PhienThanhToan\" SET \"NgayTraCuuTiepTheo\"=now()-interval '1 second' WHERE \"MaPhienThanhToan\"=:p").param("p",id(f.session,"MaPhienThanhToan")).update();payments.doiSoat(f.email,f.code);verify(client,times(2)).tra(id(f.session,"MaDonPayOS"));
        assertThat(payments.daThu(f.email,f.code)).isEqualByComparingTo(f.due);assertThat(orders.chiTiet(f.email,f.code)).containsEntry("TrangThaiThanhToan","DA_THU");
        assertThat(jdbc.sql("SELECT count(*) FROM \"GiaoDichThanhToan\" WHERE \"MaPhienThanhToan\"=:p").param("p",id(f.session,"MaPhienThanhToan")).query(Integer.class).single()).isEqualTo(1);
    }
    @Test void contradictoryApiTotalsCannotAcceptAnyNewReceipt(){
        var f=fixture("PAYOS");var receipt=receipt(f,f.due,"MISMATCH-"+UUID.randomUUID());
        doReturn(new PayOSClient.DoiSoat(id(f.session,"MaDonPayOS"),receipt.linkId(),f.due,f.due.add(BigDecimal.ONE),"PAID",List.of(receipt))).when(client).tra(id(f.session,"MaDonPayOS"));
        assertThatThrownBy(()->payments.doiSoat(f.email,f.code)).isInstanceOf(IllegalArgumentException.class);
        assertThat(payments.daThu(f.email,f.code)).isEqualByComparingTo(BigDecimal.ZERO);assertThat(orders.chiTiet(f.email,f.code)).containsEntry("TrangThaiThanhToan","CAN_DOI_SOAT").containsEntry("MaPhienThanhToanChapNhan",null);
        assertThat(payments.phien(f.email,f.code)).containsEntry("MaLoi","API_KHONG_KHOP_SO");
    }
    @Test void definitiveNotFoundCanRetrySameOrderCodeAfterCreateLeaseExpires(){
        var f=fixture("PAYOS");long code=id(f.session,"MaDonPayOS"),sid=id(f.session,"MaPhienThanhToan");
        doThrow(new IllegalStateException("first request did not reach provider")).when(client).tao(eq(code),any(),any(),eq(f.code));
        assertThatThrownBy(()->payments.taoLink(f.email,f.code)).isInstanceOf(IllegalArgumentException.class);
        doThrow(new vn.payos.exception.NotFoundException("Not found",404,"NOT_FOUND","Missing payment link")).when(client).tra(code);
        assertThatThrownBy(()->payments.taoLink(f.email,f.code)).isInstanceOf(IllegalArgumentException.class);verify(client,times(1)).tao(eq(code),any(),any(),eq(f.code));
        jdbc.sql("UPDATE \"PhienThanhToan\" SET \"NgayThuTaoTiepTheo\"=now()-interval '1 second',\"NgayTraCuuTiepTheo\"=now()-interval '1 second' WHERE \"MaPhienThanhToan\"=:p").param("p",sid).update();
        doAnswer(call->{assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();return new PayOSClient.LienKet(code,"retry-"+code,f.due,"VND","https://pay.payos.vn/web/retry-"+code,"PENDING");}).when(client).tao(eq(code),any(),any(),eq(f.code));
        payments.taoLink(f.email,f.code);verify(client,times(2)).tao(eq(code),any(),any(),eq(f.code));
        assertThat(payments.phien(f.email,f.code)).containsEntry("MaDonPayOS",code).containsEntry("MaPhienThanhToan",sid).containsEntry("TrangThaiXuLy","CHO_THANH_TOAN");
        assertThat(jdbc.sql("SELECT count(*) FROM \"PhienThanhToan\" WHERE \"MaDonHang\"=:d").param("d",id(f.session,"MaDonHang")).query(Integer.class).single()).isEqualTo(1);
    }
    @Test void webhookHttpRequiresValidSignatureButNoSessionOrCsrf()throws Exception{
        var f=fixture("PAYOS");var http=java.net.http.HttpClient.newHttpClient();String endpoint="http://127.0.0.1:"+port+"/api/payos/webhook";
        var data=new TreeMap<String,Object>();data.put("orderCode",id(f.session,"MaDonPayOS"));data.put("amount",1000L);data.put("description","Test");data.put("accountNumber","000000");data.put("reference","HTTP-"+UUID.randomUUID());data.put("transactionDateTime","2026-11-08 08:30:00");data.put("currency","VND");data.put("paymentLinkId","http-"+id(f.session,"MaPhienThanhToan"));data.put("code","00");data.put("desc","success");
        for(String key:List.of("counterAccountBankId","counterAccountBankName","counterAccountName","counterAccountNumber","virtualAccountName","virtualAccountNumber"))data.put(key,"");
        String canonical=data.entrySet().stream().map(e->e.getKey()+"="+e.getValue()).collect(java.util.stream.Collectors.joining("&"));var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec("test-checksum".getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256"));String signature=HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        var json=new com.fasterxml.jackson.databind.ObjectMapper();String valid=json.writeValueAsString(Map.of("code","00","desc","success","success",true,"data",data,"signature",signature));
        var bad=http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(endpoint)).header("Content-Type","application/json").POST(java.net.http.HttpRequest.BodyPublishers.ofString(valid.replace(signature,"invalid"))).build(),java.net.http.HttpResponse.BodyHandlers.ofString());assertThat(bad.statusCode()).isEqualTo(400);
        for(int i=0;i<2;i++){var response=http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(endpoint)).header("Content-Type","application/json").POST(java.net.http.HttpRequest.BodyPublishers.ofString(valid)).build(),java.net.http.HttpResponse.BodyHandlers.ofString());assertThat(response.statusCode()).isEqualTo(200);}
        assertThat(payments.daThu(f.email,f.code)).isEqualByComparingTo("1000");
        var large=http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(endpoint)).header("Content-Type","application/json").POST(java.net.http.HttpRequest.BodyPublishers.ofString(" ".repeat(65537))).build(),java.net.http.HttpResponse.BodyHandlers.ofString());assertThat(large.statusCode()).isEqualTo(413);
    }
}
