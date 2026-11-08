package vn.luma;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import vn.luma.business.*;
import vn.luma.data.*;
import vn.luma.web.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false"})
@Transactional
class TheoDoiThongBaoIntegrationTest {
    @Autowired TheoDoiService following;
    @Autowired TheoDoiDAO dao;
    @Autowired ThongBaoService notifications;
    @MockitoSpyBean ThongBaoDAO notificationDAO;
    @Autowired TaiKhoanService users;
    @Autowired TaiKhoanDAO accounts;
    @Autowired KhuyenMaiService offers;
    @Autowired GioHangService carts;
    @Autowired DonHangService orders;
    @Autowired BanDoService maps;
    @Autowired JdbcClient jdbc;
    @Autowired org.springframework.transaction.PlatformTransactionManager manager;
    String user(){String email="follow-"+UUID.randomUUID()+"@example.test";users.seedDemo(email,"Khách theo dõi thử","KhachHang","StrongDemo!2026");return email;}
    long follow(String email){return ((Number)following.cuaToi(email,1).orElseThrow().get("MaTheoDoi")).longValue();}
    BigDecimal price(){return (BigDecimal)dao.tinhTrang(1).get("gia");}
    void price(BigDecimal value){jdbc.sql("UPDATE \"BienTheSanPham\" SET \"GiaBan\"=:price WHERE \"MaBienThe\"=1").param("price",value).update();}
    void availability(int n){jdbc.sql("UPDATE \"TonKho\" t SET \"SoLuongTon\"=t.\"SoLuongGiu\"+:n FROM \"BienTheSanPham\" b WHERE b.\"MaBienThe\"=t.\"MaBienThe\" AND b.\"MaSanPham\"=1 AND t.\"MaKhoHang\"=1").param("n",n).update();}
    List<Map<String,Object>> notices(String email){return notifications.danhSach(email,false,1);}

    @Test void savedVariantKeepsItsExactImageAndRejectsOtherProducts(){
        String email=user();var variant=jdbc.sql("""
            SELECT b."MaSanPham",b."MaBienThe",a."DuongDanTep" FROM "BienTheSanPham" b
            JOIN "TepSanPham" a ON a."MaBienThe"=b."MaBienThe" AND a."MaSanPham"=b."MaSanPham" AND a."LoaiTep"='ANH' AND a."DangHienThi"
            JOIN "SanPham" s ON s."MaSanPham"=b."MaSanPham" WHERE b."DangKinhDoanh" AND s."DangHienThi" ORDER BY b."MaBienThe",a."ThuTu",a."MaTep" LIMIT 1
            """).query().singleRow();long product=((Number)variant.get("MaSanPham")).longValue(),sku=((Number)variant.get("MaBienThe")).longValue();
        following.luu(email,product,false,false,sku);var row=following.danhSach(email,1).getFirst();assertThat(((Number)row.get("MaBienTheDaChon")).longValue()).isEqualTo(sku);assertThat(row.get("anh_da_chon")).isEqualTo(variant.get("DuongDanTep"));
        following.luu(email,product,true,false);assertThat(((Number)following.danhSach(email,1).getFirst().get("MaBienTheDaChon")).longValue()).isEqualTo(sku);
        long wrong=jdbc.sql("SELECT \"MaBienThe\" FROM \"BienTheSanPham\" WHERE \"MaSanPham\"<>:p AND \"DangKinhDoanh\" LIMIT 1").param("p",product).query(Long.class).single();
        assertThatThrownBy(()->following.luu(email,product,false,false,wrong)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void priceAndRestockTransitionsNotifyOnceAndAccountReadOwnershipIsEnforced(){
        String email=user(),other=user();availability(0);BigDecimal original=price();following.luu(email,1,true,true);long id=follow(email);
        price(original.subtract(new BigDecimal("1000")));availability(1);following.kiemTraMot(id);following.kiemTraMot(id);
        assertThat(notices(email)).hasSize(2).anyMatch(n->"GIAM_GIA".equals(n.get("LoaiThongBao"))).anyMatch(n->"CO_HANG".equals(n.get("LoaiThongBao")));assertThat(notices(other)).isEmpty();assertThat(notifications.chuaDoc(email)).isEqualTo(2);
        assertThat((BigDecimal)following.cuaToi(email,1).orElseThrow().get("GiaThamChieu")).isEqualByComparingTo(original.subtract(new BigDecimal("1000")));
        price(original);following.kiemTraMot(id);assertThat(notices(email)).hasSize(2);price(original.subtract(new BigDecimal("1000")));following.kiemTraMot(id);assertThat(notices(email)).hasSize(3);
        long notification=((Number)notices(email).getFirst().get("MaThongBao")).longValue();assertThatThrownBy(()->notifications.doc(other,notification)).isInstanceOf(KhongTimThayException.class);
        assertThat(notifications.doc(email,notification)).isEqualTo("/san-pham/iphone-13?sku=1");assertThat(notifications.chuaDoc(email)).isEqualTo(2);notifications.doc(email,notification);assertThat(notifications.chuaDoc(email)).isEqualTo(2);notifications.docTatCa(email);assertThat(notifications.chuaDoc(email)).isZero();assertThat(notices(email)).hasSize(3);
        following.xoa(email,1);following.kiemTraMot(id);assertThat(following.cuaToi(email,1)).isEmpty();assertThat(notices(email)).hasSize(3);
    }
    @Test void uncheckedPreferencesDoNotNotifyAndHiddenModelsKeepPreferencesWithoutPublicContent(){
        String email=user();availability(0);BigDecimal original=price();following.luu(email,1,false,false);long id=follow(email);price(original.subtract(new BigDecimal("500")));availability(1);following.kiemTraMot(id);assertThat(notices(email)).isEmpty();
        following.luu(email,1,true,true);following.kiemTraMot(id);assertThat(notices(email)).isEmpty();
        jdbc.sql("UPDATE \"SanPham\" SET \"DangHienThi\"=false WHERE \"MaSanPham\"=1").update();following.kiemTraMot(id);assertThat(notices(email)).isEmpty();assertThat(following.danhSach(email,1)).hasSize(1);assertThatThrownBy(()->following.luu(user(),1,true,true)).isInstanceOf(IllegalArgumentException.class);
        following.luu(email,1,false,false);assertThat(following.cuaToi(email,1).orElseThrow()).containsEntry("NhanBaoGiamGia",false).containsEntry("NhanBaoCoHang",false);
    }
    @Test void notificationTargetsAreInternalAndSavingPublicOfferDoesNotHoldVoucherUsage(){
        String email=user();long user=users.hienTai(email).maNguoiDung();
        for(String target:List.of("https://example.test","//example.test","/\\example.test","/%2f%2fexample.test","/san-pham/x?sku=1&next=evil","/uu-dai\r\nLocation: evil"))assertThatThrownBy(()->notifications.tao(user,"GIAM_GIA","Thử","Nội dung thử",target,"bad-target")).isInstanceOf(IllegalArgumentException.class);
        assertThat(notifications.tao(user,"GIAM_GIA","Thử","Văn bản <script>test</script>","/uu-dai","dedup-key")).isTrue();assertThat(notifications.tao(user,"GIAM_GIA","Thử","Nội dung thử","/uu-dai","dedup-key")).isFalse();
        String code="F-"+UUID.randomUUID().toString().substring(0,20).toUpperCase(Locale.ROOT);var now=LocalDateTime.now();var voucher=new VoucherYeuCau(code,"Ưu đãi thử","SO_TIEN",new BigDecimal("100000"),null,BigDecimal.ZERO,now.minusDays(1),now.plusDays(1),2,1,true,true);
        long id=offers.luuVoucher("admin@luma.local",null,voucher);offers.luuThongBao(email,id);offers.luuThongBao(email,id);assertThat(notices(email)).hasSize(2);assertThat(jdbc.sql("SELECT count(*) FROM \"DonHang\" WHERE \"MaGiamGia\"=:id").param("id",id).query(Long.class).single()).isZero();
        var privateVoucher=new VoucherYeuCau("P-"+code,"Riêng","SO_TIEN",new BigDecimal("100"),null,BigDecimal.ZERO,now.minusDays(1),now.plusDays(1),2,1,false,true);long privateId=offers.luuVoucher("admin@luma.local",null,privateVoucher);assertThatThrownBy(()->offers.luuThongBao(email,privateId)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void orderCreateReplayAndCancelUseOneNotificationPerHistoryEntry(){
        String email=user();carts.sua(email,1,1,true);var address=new DiaChiYeuCau("Khách thử","0901234567","TP.HCM","Phường thử","123 Đường thử","","",null,null,false);var request=new DatHangYeuCau(UUID.randomUUID().toString(),orders.noiDungGio(email),"",address);
        String code=orders.datCOD(email,request,maps.chuanHoa(address));assertThat(notices(email)).hasSize(1);assertThat(orders.datCOD(email,request,maps.chuanHoa(address))).isEqualTo(code);assertThat(notices(email)).hasSize(1);orders.huy(email,code);orders.huy(email,code);assertThat(notices(email)).hasSize(2).allMatch(n->("/don-hang/"+code).equals(n.get("DuongDanDich")));
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED) void failedSecondNotificationRollsBackFirstNotificationAndFollowReference(){
        String email=user();BigDecimal original=price();var stock=jdbc.sql("SELECT t.\"MaBienThe\",t.\"SoLuongTon\" FROM \"TonKho\" t JOIN \"BienTheSanPham\" b USING (\"MaBienThe\") WHERE b.\"MaSanPham\"=1 AND t.\"MaKhoHang\"=1").query().listOfRows();
        try{
            availability(0);following.luu(email,1,true,true);long id=follow(email);var before=following.cuaToi(email,1).orElseThrow();price(original.subtract(new BigDecimal("500")));availability(1);var count=new AtomicInteger();
            doAnswer(call->{if(count.incrementAndGet()==2)throw new IllegalStateException("Injected second notification failure");return call.callRealMethod();}).when(notificationDAO).tao(anyLong(),anyString(),anyString(),anyString(),anyString(),anyString());
            assertThatThrownBy(()->following.kiemTraMot(id)).isInstanceOf(IllegalStateException.class);assertThat(count).hasValue(2);assertThat(notices(email)).isEmpty();assertThat(following.cuaToi(email,1).orElseThrow()).isEqualTo(before);
        }finally{reset(notificationDAO);following.xoa(email,1);price(original);for(var row:stock)jdbc.sql("UPDATE \"TonKho\" SET \"SoLuongTon\"=:quantity WHERE \"MaBienThe\"=:sku AND \"MaKhoHang\"=1").param("quantity",row.get("SoLuongTon")).param("sku",row.get("MaBienThe")).update();}
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED) void accountMutexAndNotificationForeignKeyDoNotDeadlockWithFollowUpdate()throws Exception{
        String email=user();long user=users.hienTai(email).maNguoiDung();following.luu(email,1,false,false);long id=follow(email);var pool=Executors.newFixedThreadPool(2);var userLocked=new CountDownLatch(1);var followLocked=new CountDownLatch(1);
        try{
            var a=pool.submit(()->new TransactionTemplate(manager).executeWithoutResult(status->{accounts.khoaNguoiDung(user);userLocked.countDown();await(followLocked);dao.khoa(id).orElseThrow();}));
            var b=pool.submit(()->{await(userLocked);new TransactionTemplate(manager).executeWithoutResult(status->{dao.khoa(id).orElseThrow();followLocked.countDown();notifications.tao(user,"CO_HANG","Thử đồng thời","Thông báo kiểm tra khóa ngoại","/uu-dai","parallel-key");});});
            a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);assertThat(notices(email)).hasSize(1);
        }finally{pool.shutdownNow();following.xoa(email,1);}
    }
    static void await(CountDownLatch latch){try{if(!latch.await(5,TimeUnit.SECONDS))throw new IllegalStateException("Timed out awaiting lock fixture");}catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IllegalStateException(ex);}}
}
