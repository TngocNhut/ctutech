package vn.luma;

import java.math.BigDecimal;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import vn.luma.business.*;
import vn.luma.data.*;
import vn.luma.model.BaoThu;
import vn.luma.web.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static vn.luma.data.ThanhToanDAO.id;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.mail.local-directory=.local/test-mail",
    "app.refunds.proof-directory=.local/test-refund-proofs","app.scheduling.enabled=false"})
@Transactional
class TraHoanIntegrationTest {
    static final String ADMIN="admin@luma.local";
    @Autowired TraHangService returns;
    @Autowired HoanTienService refunds;
    @Autowired GiaoNhanService shipping;
    @Autowired DonHangService orders;
    @Autowired GioHangService carts;
    @Autowired TaiKhoanService users;
    @Autowired BanDoService maps;
    @Autowired HoaDonService invoices;
    @Autowired ThanhToanService payments;
    @Autowired ThanhToanGhiSoService ledger;
    @Autowired JdbcClient jdbc;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @MockitoSpyBean TraHangDAO returnDao;
    @MockitoSpyBean HoanTienDAO refundDao;
    record Order(String email,String code,long id,long line){}
    String user(){String email="return-"+UUID.randomUUID()+"@example.test";users.seedDemo(email,"Khách trả hàng","KhachHang","StrongDemo!2026");return email;}
    Order order(int qty,String method,boolean discount){
        String email=user();carts.sua(email,1,qty,true);String coupon="";
        if(discount){coupon="R"+UUID.randomUUID().toString().replace("-","").toUpperCase(Locale.ROOT);jdbc.sql("INSERT INTO \"MaGiamGia\" (\"KyHieu\",\"MoTa\",\"KieuGiam\",\"GiaTriGiam\",\"DonToiThieu\",\"NgayBatDau\",\"NgayKetThuc\",\"TongLuotToiDa\",\"LuotMoiKhach\",\"CongKhai\",\"DangHoatDong\",\"NgayCapNhat\") VALUES (:c,'Refund allocation test','SO_TIEN',100001,0,now()-interval '1 day',now()+interval '1 day',1,1,false,true,now())").param("c",coupon).update();}
        var address=new DiaChiYeuCau("Khách trả hàng","0901234567","Thành phố Hồ Chí Minh","Phường Bến Nghé","123 Nguyễn Huệ","","",null,null,false);
        var request=new DatHangYeuCau(UUID.randomUUID().toString(),orders.noiDungGio(email),coupon,address,method);
        String code=orders.datHang(email,request,maps.chuanHoa(address));long oid=id(orders.chiTiet(email,code),"MaDonHang");return new Order(email,code,oid,id(orders.dong(email,code).getFirst(),"MaChiTietDonHang"));
    }
    void dispatch(Order order){shipping.xacNhan(ADMIN,order.code(),true);long carrier=jdbc.sql("SELECT \"MaDonViVanChuyen\" FROM \"DonViVanChuyen\" WHERE \"KyHieu\"='LUMA_NOI_BO'").query(Long.class).single();shipping.taoVanDon(ADMIN,order.code(),new TaoVanDonYeuCau(carrier,"RET-TEST-"+order.code(),null,null));shipping.capNhat(ADMIN,order.code(),event("BAN_GIAO"));}
    SuKienGiaoNhanYeuCau event(String action){return new SuKienGiaoNhanYeuCau(action,UUID.randomUUID().toString(),"Sự kiện kiểm thử "+action);}
    void paidCod(Order order){dispatch(order);shipping.capNhat(ADMIN,order.code(),event("DA_GIAO"));shipping.thuCOD(ADMIN,order.code(),new ThuCODYeuCau((BigDecimal)orders.chiTiet(ADMIN,order.code()).get("TongThanhToan"),"TEST-"+order.code(),true));}
    TaoTraHangYeuCau request(Order order,int qty){return new TaoTraHangYeuCau(UUID.randomUUID().toString(),"Khách muốn trả sau khi kiểm tra",List.of(new TaoTraHangYeuCau.Dong(order.line(),qty)));}
    long ticket(Order order,int qty,BigDecimal fee){long id=returns.tao(order.email(),order.code(),request(order,qty),false);returns.duyet(ADMIN,id,true,fee,"Đã kiểm tra chính sách và đồng ý nhận hàng");return id;}
    KiemHangYeuCau inspection(long ticket,int received,int approved,int restock){long line=id(returns.dong(ADMIN,ticket).getFirst(),"MaChiTietTraHang");return new KiemHangYeuCau(true,List.of(new KiemHangYeuCau.Dong(line,received,approved,restock,"Đã kiểm ngoại quan, phụ kiện và điều kiện bán lại")));}
    int stock(){return jdbc.sql("SELECT \"SoLuongTon\" FROM \"TonKho\" WHERE \"MaKhoHang\"=1 AND \"MaBienThe\"=1").query(Integer.class).single();}
    List<Map<String,Object>> refundRows(Order order){return refunds.theoDon(order.email(),order.code());}
    XacNhanHoanYeuCau confirmation(BigDecimal amount){return new XacNhanHoanYeuCau(amount,"1234567890","NGAN HANG DEMO","KHACH TRA HANG","REF-"+UUID.randomUUID(),true);}
    byte[] proof(Order order){return invoices.tai(order.email(),order.code());}
    void settle(Order order,long ticket){for(var row:refundRows(order)){if(row.get("MaPhieuTraHang")!=null&&id(row,"MaPhieuTraHang")==ticket)refunds.xacNhan(ADMIN,id(row,"MaHoanTien"),confirmation((BigDecimal)row.get("SoTienHoan")),proof(order));}}
    @Test void partialReturnsUseCumulativeNetPriceAndRestockOnlyEligibleUnits(){
        Order order=order(3,"COD",true);paidCod(order);int physical=stock();byte[] originalInvoice=proof(order);
        var request=request(order,1);long first=returns.tao(order.email(),order.code(),request,false);
        assertThat(returns.tao(order.email(),order.code(),request,false)).isEqualTo(first);
        assertThatThrownBy(()->returns.tao(order.email(),order.code(),request(order,1),false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->returns.chiTiet(user(),first)).isInstanceOf(KhongTimThayException.class);
        returns.duyet(ADMIN,first,true,BigDecimal.ZERO,"Nhận kiểm hàng lần một");returns.dangHoan(ADMIN,first);
        var inspected=inspection(first,1,1,0);returns.kiemHang(ADMIN,first,inspected);returns.kiemHang(ADMIN,first,inspected);
        assertThat(stock()).isEqualTo(physical);assertThat(refundRows(order)).hasSize(1);
        assertThat((BigDecimal)refundRows(order).getFirst().get("SoTienHoan")).isEqualByComparingTo("10956666");
        long refund=id(refundRows(order).getFirst(),"MaHoanTien");var confirm=confirmation(new BigDecimal("10956666"));
        refunds.danhDauLoi(ADMIN,refund,"Chưa xác minh được thông tin nhận");refunds.thuLai(ADMIN,refund);refunds.xacNhan(ADMIN,refund,confirm,originalInvoice);refunds.xacNhan(ADMIN,refund,confirm,originalInvoice);
        assertThat(refunds.chungTu(order.email(),refund)).isEqualTo(originalInvoice);
        assertThatThrownBy(()->refunds.chungTu(user(),refund)).isInstanceOf(KhongTimThayException.class);
        assertThat(returns.chiTiet(order.email(),first)).containsEntry("TrangThai","HOAN_THANH");
        assertThat(orders.chiTiet(order.email(),order.code())).containsEntry("TrangThaiDon","HOAN_THANH").containsEntry("TrangThaiThanhToan","HOAN_MOT_PHAN");
        assertThatThrownBy(()->returns.tao(order.email(),order.code(),request(order,3),false)).isInstanceOf(IllegalArgumentException.class);
        long second=ticket(order,2,new BigDecimal("30000"));returns.kiemHang(ADMIN,second,inspection(second,2,2,1));assertThat(stock()).isEqualTo(physical+1);settle(order,second);
        BigDecimal returned=refundRows(order).stream().map(r->(BigDecimal)r.get("SoTienHoan")).reduce(BigDecimal.ZERO,BigDecimal::add);
        assertThat(returned).isEqualByComparingTo((BigDecimal)orders.chiTiet(ADMIN,order.code()).get("TongThanhToan"));
        assertThat(orders.chiTiet(order.email(),order.code())).containsEntry("TrangThaiDon","HOAN_THANH").containsEntry("TrangThaiThanhToan","DA_HOAN");
        assertThat(jdbc.sql("SELECT count(*) FROM \"ChungTuHoanTien\" c JOIN \"HoaDon\" h USING (\"MaHoaDon\") WHERE h.\"MaDonHang\"=:d").param("d",order.id()).query(Integer.class).single()).isEqualTo(2);
        assertThatThrownBy(()->returns.tao(order.email(),order.code(),request(order,1),false)).isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.sql("SELECT \"TongTien\" FROM \"HoaDon\" WHERE \"MaDonHang\"=:d").param("d",order.id()).query(BigDecimal.class).single()).isEqualByComparingTo(returned);
    }
    @Test void returnShippingCostsShareIdempotentLedgerWithoutChangingCustomerFeeRefundOrInvoice(){
        var order=order(1,"COD",false);paidCod(order);long ticket=ticket(order,1,BigDecimal.ZERO);long waybill=id(returns.vanDon(ADMIN,ticket).orElseThrow(),"MaVanDon");
        var beforeOrder=orders.chiTiet(ADMIN,order.code());var beforeTicket=returns.chiTiet(ADMIN,ticket);var invoice=jdbc.sql("SELECT * FROM \"HoaDon\" WHERE \"MaDonHang\"=:id").param("id",order.id()).query().singleRow();
        assertThat(shipping.cuocVanDon(ADMIN,waybill)).isEmpty();shipping.themCuocVanDon(ADMIN,waybill,new PhiVanChuyenYeuCau(UUID.randomUUID().toString(),"CUOC_GOC",BigDecimal.ZERO,"Đối tác miễn cước hoàn"));
        var extra=new PhiVanChuyenYeuCau(UUID.randomUUID().toString(),"PHU_PHI",new BigDecimal("5000"),"Phụ phí hoàn mẫu");shipping.themCuocVanDon(ADMIN,waybill,extra);shipping.themCuocVanDon(ADMIN,waybill,extra);shipping.themCuocVanDon(ADMIN,waybill,new PhiVanChuyenYeuCau(UUID.randomUUID().toString(),"GIAM_CUOC",new BigDecimal("1000"),"Giảm theo đối soát"));
        assertThat(shipping.cuocVanDon(ADMIN,waybill)).hasSize(3);assertThat(shipping.cuocVanDon(ADMIN,waybill).stream().map(r->"GIAM_CUOC".equals(r.get("LoaiPhi"))?((BigDecimal)r.get("SoTien")).negate():(BigDecimal)r.get("SoTien")).reduce(BigDecimal.ZERO,BigDecimal::add)).isEqualByComparingTo("4000");
        assertThatThrownBy(()->shipping.themCuocVanDon(ADMIN,waybill,new PhiVanChuyenYeuCau(extra.khoaChongLap(),"PHU_PHI",new BigDecimal("5001"),extra.ghiChu()))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->shipping.themCuocVanDon(ADMIN,waybill,new PhiVanChuyenYeuCau(UUID.randomUUID().toString(),"GIAM_CUOC",new BigDecimal("4001"),"Quá tổng cước"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->shipping.themCuocVanDon(order.email(),waybill,extra)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(orders.chiTiet(ADMIN,order.code())).isEqualTo(beforeOrder);assertThat(returns.chiTiet(ADMIN,ticket)).isEqualTo(beforeTicket);assertThat(jdbc.sql("SELECT * FROM \"HoaDon\" WHERE \"MaDonHang\"=:id").param("id",order.id()).query().singleRow()).isEqualTo(invoice);
    }
    @Test void unpaidFailedCodParcelReturnsToStockBeforeOrderIsCancelled(){
        Order order=order(1,"COD",false);int physical=stock();dispatch(order);shipping.capNhat(ADMIN,order.code(),event("GIAO_THAT_BAI"));
        assertThatThrownBy(()->returns.tao(order.email(),order.code(),request(order,1),true)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        long ticket=returns.tao(ADMIN,order.code(),request(order,1),true);assertThat(orders.chiTiet(ADMIN,order.code())).containsEntry("TrangThaiDon","CHO_HANG_HOAN_VE");
        assertThatThrownBy(()->shipping.capNhat(ADMIN,order.code(),event("GIAO_LAI"))).isInstanceOf(IllegalArgumentException.class);
        returns.duyet(ADMIN,ticket,true,BigDecimal.ZERO,"Nhận lại kiện không giao được");var inspected=inspection(ticket,1,0,1);returns.kiemHang(ADMIN,ticket,inspected);returns.kiemHang(ADMIN,ticket,inspected);
        assertThat(stock()).isEqualTo(physical);assertThat(orders.chiTiet(ADMIN,order.code())).containsEntry("TrangThaiDon","DA_HUY").containsEntry("TrangThaiThanhToan","CHUA_THU");
        assertThat(refundRows(order)).isEmpty();assertThat(returns.chiTiet(ADMIN,ticket)).containsEntry("TrangThai","HOAN_THANH");
        assertThatThrownBy(()->returns.kiemHang(ADMIN,ticket,inspection(ticket,1,0,0))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void overpaymentApprovalPreservesPrincipalAndRefundIsCountedOnce(){
        Order order=order(1,"PAYOS_MO_PHONG",false);var session=payments.phien(order.email(),order.code());BigDecimal due=(BigDecimal)session.get("SoTienYeuCau");
        var receipt=new BaoThu(id(session,"MaDonPayOS"),"mock-"+id(session,"MaPhienThanhToan"),"EXCESS-"+UUID.randomUUID(),due.add(new BigDecimal("1234")),"VND",OffsetDateTime.now());
        ledger.xuLy(ledger.nhanDaXacMinh("PAYOS_LUMA_MOCK",receipt));
        assertThatThrownBy(()->shipping.xacNhan(ADMIN,order.code(),true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->refunds.chapNhanThuDu(order.email(),order.code(),true,"Đã kiểm tra")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        refunds.chapNhanThuDu(ADMIN,order.code(),true,"Đã kiểm tra thu dư và phần tiền hàng còn nguyên");refunds.chapNhanThuDu(ADMIN,order.code(),true,"Đã kiểm tra thu dư và phần tiền hàng còn nguyên");
        assertThat(orders.chiTiet(ADMIN,order.code())).containsEntry("TrangThaiThanhToan","DA_THU");
        var refund=refundRows(order).getFirst();long rid=id(refund,"MaHoanTien");var confirm=confirmation((BigDecimal)refund.get("SoTienHoan"));
        assertThatThrownBy(()->refunds.xacNhan(ADMIN,rid,confirmation(BigDecimal.ONE),proof(order))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->refunds.xacNhan(ADMIN,rid,confirm,"not a PDF".getBytes())).isInstanceOf(IllegalArgumentException.class);
        refunds.xacNhan(ADMIN,rid,confirm,proof(order));refunds.xacNhan(ADMIN,rid,confirm,proof(order));
        assertThat(orders.chiTiet(ADMIN,order.code())).containsEntry("TrangThaiThanhToan","DA_THU");assertThat(refundRows(order)).hasSize(1);
        dispatch(order);shipping.capNhat(ADMIN,order.code(),event("DA_GIAO"));assertThat(orders.chiTiet(ADMIN,order.code())).containsEntry("TrangThaiDon","HOAN_THANH");
    }
    @Test void returnRejectionAndWithdrawalDoNotChangeStockOrCreateRefunds(){
        Order order=order(1,"COD",false);paidCod(order);int physical=stock();
        long first=returns.tao(order.email(),order.code(),request(order,1),false);returns.huy(order.email(),first);returns.huy(order.email(),first);
        long second=returns.tao(order.email(),order.code(),request(order,1),false);returns.duyet(ADMIN,second,false,BigDecimal.ZERO,"Không đủ điều kiện theo kết quả đối chiếu");
        assertThat(returns.chiTiet(order.email(),second)).containsEntry("TrangThai","TU_CHOI");assertThat(stock()).isEqualTo(physical);assertThat(refundRows(order)).isEmpty();
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED) void inspectionFailureRollsBackStockAndRefundQueue(){
        int physical=stock();long[] ticket={0};doThrow(new IllegalStateException("Injected return refund failure")).when(returnDao).taoHoan(anyLong(),anyLong(),any());
        try{assertThatThrownBy(()->new TransactionTemplate(transactionManager).execute(status->{Order order=order(1,"COD",false);paidCod(order);ticket[0]=ticket(order,1,BigDecimal.ZERO);returns.kiemHang(ADMIN,ticket[0],inspection(ticket[0],1,1,1));return null;})).isInstanceOf(IllegalStateException.class).hasMessageContaining("Injected return refund failure");}finally{reset(returnDao);}
        assertThat(stock()).isEqualTo(physical);assertThat(jdbc.sql("SELECT count(*) FROM \"PhieuTraHang\" WHERE \"MaPhieuTraHang\"=:id").param("id",ticket[0]).query(Integer.class).single()).isZero();
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED) void failedRefundTransactionRemovesItsPrivateUploadedFile()throws Exception{
        Path dir=Path.of(".local/test-refund-proofs");Files.createDirectories(dir);long before;try(var files=Files.list(dir)){before=files.count();}
        doThrow(new IllegalStateException("Injected refund document failure")).when(refundDao).chungTu(anyLong());
        try{assertThatThrownBy(()->new TransactionTemplate(transactionManager).execute(status->{Order order=order(1,"COD",false);paidCod(order);long ticket=ticket(order,1,BigDecimal.ZERO);returns.kiemHang(ADMIN,ticket,inspection(ticket,1,1,1));settle(order,ticket);return null;})).isInstanceOf(IllegalStateException.class).hasMessageContaining("Injected refund document failure");}finally{reset(refundDao);}
        try(var files=Files.list(dir)){assertThat(files.count()).isEqualTo(before);}
    }
}
