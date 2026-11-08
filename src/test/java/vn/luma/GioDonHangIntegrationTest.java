package vn.luma;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import vn.luma.business.*;
import vn.luma.data.DonHangDAO;
import vn.luma.model.DiaChiChot;
import vn.luma.web.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test",
    "app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false"})
class GioDonHangIntegrationTest {
    @Autowired DatHangController checkout;
    @Autowired GioHangService carts;
    @Autowired DonHangService orders;
    @Autowired TaiKhoanService users;
    @Autowired BanDoService maps;
    @Autowired DiaChiService addresses;
    @Autowired JdbcClient jdbc;
    @Autowired vn.luma.data.GioHangDAO cartDAO;
    @MockitoSpyBean DonHangDAO dao;
    String user(){String email="order-"+UUID.randomUUID()+"@example.test";users.seedDemo(email,"Người nhận thử","KhachHang","StrongDemo!2026");return email;}
    DiaChiYeuCau address(){return new DiaChiYeuCau("Người nhận thử","0901234567","Thành phố Hồ Chí Minh","Phường Bến Nghé","123 Đường thử nghiệm","Gọi trước khi giao","",null,null,false);}
    DatHangYeuCau request(String email,String coupon){return new DatHangYeuCau(UUID.randomUUID().toString(),orders.noiDungGio(email),coupon,address());}
    int held(long sku){return jdbc.sql("SELECT \"SoLuongGiu\" FROM \"TonKho\" WHERE \"MaKhoHang\"=1 AND \"MaBienThe\"=:s").param("s",sku).query(Integer.class).single();}
    @Test @org.springframework.transaction.annotation.Transactional void orderListContainsPurchasedVariantAndIsOwnerScoped(){
        String email=user(),other=user();carts.sua(email,1,1,true);var req=request(email,"");String code=orders.datCOD(email,req,maps.chuanHoa(req.diaChi()));
        var listed=orders.danhSach(email,1);assertThat(listed).hasSize(1);assertThat(listed.getFirst()).containsEntry("MaDonCongKhai",code);
        @SuppressWarnings("unchecked") var items=(List<Map<String,Object>>)listed.getFirst().get("sanPham");assertThat(items).hasSize(1);
        assertThat(((Number)items.getFirst().get("MaBienThe")).longValue()).isEqualTo(1);assertThat(items.getFirst().get("TenSanPhamLucDat")).isNotNull();assertThat(items.getFirst().get("mau")).isNotNull();
        assertThat(orders.danhSach(other,1)).isEmpty();
    }
    @Test @org.springframework.transaction.annotation.Transactional void mergingRetainsDiscontinuedSkuAndShowsItAsBlocked(){
        String email=user();var guest=carts.suaKhach(Map.of(),1,1,true);int before=held(1);
        jdbc.sql("UPDATE \"BienTheSanPham\" SET \"DangKinhDoanh\"=false WHERE \"MaBienThe\"=1").update();
        var result=carts.gop(email,guest);assertThat(result.daGop()).isEqualTo(1);assertThat(result.canKiemTra()).isEqualTo(1);assertThat(result.conLai()).isEmpty();
        var cart=carts.xem(email,Map.of());assertThat(cart.dong()).hasSize(1);assertThat(cart.coTheDat()).isFalse();assertThat(held(1)).isEqualTo(before);
        carts.sua(email,1,0,false);assertThat(carts.xem(email,Map.of()).dong()).isEmpty();
    }
    @Test @org.springframework.transaction.annotation.Transactional void mergeOverflowRemainsAvailableForExplicitRetry(){
        String email=user();jdbc.sql("UPDATE \"TonKho\" SET \"SoLuongTon\"=200 WHERE \"MaKhoHang\"=1 AND \"MaBienThe\"=1").update();carts.sua(email,1,99,false);
        var first=carts.gop(email,Map.of(1L,30));assertThat(first.daGop()).isZero();assertThat(first.conLai()).containsEntry(1L,30);
        carts.sua(email,1,80,false);var second=carts.gop(email,first.conLai());assertThat(second.daGop()).isEqualTo(19);assertThat(second.conLai()).containsEntry(1L,11);assertThat(carts.xem(email,Map.of()).dong().getFirst().soLuong()).isEqualTo(99);
        carts.sua(email,1,88,false);var third=carts.gop(email,second.conLai());assertThat(third.daGop()).isEqualTo(11);assertThat(third.conLai()).isEmpty();
        var unknown=carts.gop(email,Map.of(999999999L,2));assertThat(unknown.conLai()).containsEntry(999999999L,2);
    }
    @Test void pendingGuestChoicesCannotBeMergedOrChangedByAnotherAccount(){
        var session=new org.springframework.mock.web.MockHttpSession();GioHangController.luuConLai(session,"owner@example.test",Map.of(1L,7));var controller=new GioHangController(carts,org.mockito.Mockito.mock(vn.luma.web.HanhViController.class));var flash=new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();
        assertThatThrownBy(()->controller.gopLai(()->"other@example.test",session,flash)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->controller.suaChoGop(1,0,()->"other@example.test",session,flash)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(GioHangController.guest(session)).containsEntry(1L,7);controller.suaChoGop(1,3,()->"owner@example.test",session,flash);assertThat(GioHangController.guest(session)).containsEntry(1L,3);
    }
    @Test @org.springframework.transaction.annotation.Transactional void cartLineLimitKeepsWholePendingLineUntilSpaceIsFreed(){
        String email=user();var ids=jdbc.sql("""
            INSERT INTO "BienTheSanPham" ("MaSanPham","MaHang","TenBienThe","MauSac","BoXuLy","DungLuongRam","DungLuongLuuTru","KichThuocManHinh","HeDieuHanh","ThongSoBoSung","GiaNiemYet","GiaBan","GiaVon","DangKinhDoanh","NgayCapNhat")
            SELECT b."MaSanPham",:prefix||g.n,b."TenBienThe",b."MauSac",b."BoXuLy",b."DungLuongRam",b."DungLuongLuuTru",b."KichThuocManHinh",b."HeDieuHanh",'{}',b."GiaNiemYet",b."GiaBan",b."GiaVon",false,now()
            FROM "BienTheSanPham" b CROSS JOIN generate_series(1,51) g(n) WHERE b."MaBienThe"=1 RETURNING "MaBienThe"
            """).param("prefix","merge-"+UUID.randomUUID()+"-").query(Long.class).list();
        long cart=cartDAO.khoaGio(users.hienTai(email).maNguoiDung());for(long id:ids.subList(0,50))cartDAO.ghi(cart,id,1);
        var result=carts.gop(email,Map.of(ids.getLast(),2));assertThat(result.conLai()).containsEntry(ids.getLast(),2);assertThat(carts.xem(email,Map.of()).dong()).hasSize(50);
        carts.sua(email,ids.getFirst(),0,false);var retry=carts.gop(email,result.conLai());assertThat(retry.conLai()).isEmpty();assertThat(retry.daGop()).isEqualTo(2);assertThat(carts.xem(email,Map.of()).dong()).hasSize(50);
    }
    @Test void guestMergeDoesNotReserveStockAndQuantityCannotExceedAvailability(){
        String email=user();int before=held(1);var guest=carts.suaKhach(Map.of(),1,1,true);carts.gop(email,guest);
        assertThat(carts.xem(email,Map.of()).dong()).hasSize(1);assertThat(held(1)).isEqualTo(before);
        assertThatThrownBy(()->carts.sua(email,1,99,false)).isInstanceOf(IllegalArgumentException.class);
        carts.sua(email,1,0,false);assertThat(carts.xem(email,Map.of()).dong()).isEmpty();
    }
    @Test @org.springframework.transaction.annotation.Transactional void addressEditKeepsOrderSnapshotAndUnexpiredPinWithoutExtendingRetention(){
        String email=user(),other=user();var original=maps.chuanHoa(address());addresses.tao(email,original,true);long id=((Number)addresses.danhSach(email).getFirst().get("MaDiaChi")).longValue();
        assertThatThrownBy(()->addresses.chiTiet(other,id)).isInstanceOf(KhongTimThayException.class);
        assertThatThrownBy(()->addresses.sua(other,id,original,false,false)).isInstanceOf(KhongTimThayException.class);
        carts.sua(email,1,1,true);var request=request(email,"");String code=orders.datCOD(email,request,original);
        jdbc.sql("""
            UPDATE "DiaChi" SET "MaDiaDiemGoogle"='test-place',"ViDo"=10.7769,"KinhDo"=106.7009,"NguonToaDo"='GOOGLE_GEOCODING',
              "NgayLayToaDo"=now(),"NgayXacNhanViTri"=now(),"HanLuuToaDo"=now()+interval '30 days' WHERE "MaDiaChi"=:id
            """).param("id",id).update();
        var before=addresses.chiTiet(email,id);addresses.sua(email,id,original,true,true);var after=addresses.chiTiet(email,id);
        assertThat(after.get("ViDo")).isEqualTo(before.get("ViDo"));assertThat(after.get("HanLuuToaDo")).isEqualTo(before.get("HanLuuToaDo"));
        var changed=new DiaChiChot(original.tenNguoiNhan(),original.soDienThoai(),original.tinhThanh(),original.phuongXa(),"456 Địa chỉ mới",original.ghiChu(),null,null,null,null,null);
        assertThatThrownBy(()->addresses.sua(email,id,changed,true,true)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Địa chỉ đã thay đổi");
        addresses.sua(email,id,changed,true,false);assertThat(addresses.chiTiet(email,id).get("ViDo")).isNull();assertThat(addresses.chiTiet(email,id).get("DiaChiCuThe")).isEqualTo("456 Địa chỉ mới");
        assertThat(orders.chiTiet(email,code).get("DiaChiNhanCuThe")).isEqualTo(original.diaChiCuThe());
        addresses.tao(email,original,true);assertThat(addresses.danhSach(email).stream().filter(a->Boolean.TRUE.equals(a.get("LaMacDinh"))).count()).isEqualTo(1);orders.huy(email,code);
    }
    @Test @org.springframework.transaction.annotation.Transactional void checkoutPrefillsOnlyCurrentUsersSavedDefaultAddress(){
        String email=user(),other=user();var saved=maps.chuanHoa(address());addresses.tao(email,saved,true);carts.sua(email,1,1,true);carts.sua(other,1,1,true);
        var model=new org.springframework.ui.ExtendedModelMap();assertThat(checkout.xem(()->email,model)).isEqualTo("dat-hang");var form=(DatHangYeuCau)model.get("form");
        assertThat(form.diaChi().tenNguoiNhan()).isEqualTo(saved.tenNguoiNhan());assertThat(form.diaChi().soDienThoai()).isEqualTo(saved.soDienThoai());assertThat(form.diaChi().tinhThanh()).isEqualTo(saved.tinhThanh());assertThat(form.diaChi().phuongXa()).isEqualTo(saved.phuongXa());assertThat(form.diaChi().diaChiCuThe()).isEqualTo(saved.diaChiCuThe());
        var otherModel=new org.springframework.ui.ExtendedModelMap();checkout.xem(()->other,otherModel);assertThat(((DatHangYeuCau)otherModel.get("form")).diaChi().diaChiCuThe()).isEmpty();
    }
    @Test @org.springframework.transaction.annotation.Transactional void firstCheckoutSavesAddressAndRetryDoesNotDuplicateIt(){
        String email=user();assertThat(addresses.danhSach(email)).isEmpty();carts.sua(email,1,1,true);var request=request(email,"");
        String code=orders.datCOD(email,request,maps.chuanHoa(request.diaChi()));
        var saved=addresses.danhSach(email);assertThat(saved).hasSize(1);assertThat(saved.getFirst().get("DiaChiCuThe")).isEqualTo("123 Đường thử nghiệm");assertThat(saved.getFirst().get("LaMacDinh")).isEqualTo(true);
        assertThat(orders.datCOD(email,request,maps.chuanHoa(request.diaChi()))).isEqualTo(code);assertThat(addresses.danhSach(email)).hasSize(1);
    }
    @Test void checkoutIsIdempotentAndCancellationReturnsStockAndVoucher(){
        String email=user();carts.sua(email,1,1,true);var request=request(email,"LUMA100");int before=held(1);
        var quote=orders.baoGia(email,request);assertThat(quote.giamGia()).isEqualByComparingTo("100000");assertThat(held(1)).isEqualTo(before);
        String code=orders.datCOD(email,request,maps.chuanHoa(request.diaChi()));
        try{
            assertThat(held(1)).isEqualTo(before+1);assertThat(carts.xem(email,Map.of()).dong()).isEmpty();
            assertThat(orders.datCOD(email,request,maps.chuanHoa(request.diaChi()))).isEqualTo(code);
            assertThat(held(1)).isEqualTo(before+1);
            var d=orders.chiTiet(email,code);assertThat((BigDecimal)d.get("TienGiamGia")).isEqualByComparingTo("100000");
            assertThat((BigDecimal)d.get("TongThanhToan")).isEqualByComparingTo(((BigDecimal)d.get("TongTienHang")).subtract(new BigDecimal("100000")).add(orders.phiGiao()));
            assertThatThrownBy(()->orders.chiTiet(user(),code)).isInstanceOf(KhongTimThayException.class);
            var changed=new DatHangYeuCau(request.khoaChongLap(),request.noiDungGio(),"",address());
            assertThatThrownBy(()->orders.datCOD(email,changed,maps.chuanHoa(changed.diaChi()))).isInstanceOf(IllegalArgumentException.class);
        }finally{orders.huy(email,code);}
        orders.huy(email,code);assertThat(held(1)).isEqualTo(before);assertThat(orders.chiTiet(email,code)).containsEntry("TrangThaiMaGiamGia","DA_TRA_LUOT");
    }
    @Test void failureAfterOrderAndStockWritesRollsBackEverything(){
        String email=user();carts.sua(email,1,1,true);var request=request(email,"");int before=held(1);
        doThrow(new IllegalStateException("injected failure after stock writes")).when(dao).lichSu(anyLong(),anyLong(),isNull(),eq("CHO_XAC_NHAN"),anyString());
        try{assertThatThrownBy(()->orders.datCOD(email,request,maps.chuanHoa(request.diaChi()))).isInstanceOf(IllegalStateException.class);}finally{reset(dao);}
        assertThat(held(1)).isEqualTo(before);assertThat(carts.xem(email,Map.of()).dong()).hasSize(1);
        assertThat(jdbc.sql("SELECT count(*) FROM \"DonHang\" WHERE \"KhoaChongLap\"=:k").param("k",request.khoaChongLap()).query(Integer.class).single()).isZero();
    }
    @Test void onlyOneBuyerGetsTheLastUnit()throws Exception{
        String a=user(),b=user();carts.sua(a,23,1,true);carts.sua(b,23,1,true);
        var ra=request(a,"");var rb=request(b,"");CountDownLatch go=new CountDownLatch(1);ExecutorService pool=Executors.newFixedThreadPool(2);
        List<Map.Entry<String,String>> successes=new ArrayList<>();
        try{
            var fa=pool.submit(()->attempt(a,ra,go));var fb=pool.submit(()->attempt(b,rb,go));go.countDown();
            String ca=fa.get(15,TimeUnit.SECONDS),cb=fb.get(15,TimeUnit.SECONDS);
            if(ca!=null)successes.add(Map.entry(a,ca));if(cb!=null)successes.add(Map.entry(b,cb));
            assertThat(successes).hasSize(1);assertThat(held(23)).isEqualTo(1);
        }finally{pool.shutdownNow();for(var s:successes)orders.huy(s.getKey(),s.getValue());}
    }
    String attempt(String email,DatHangYeuCau request,CountDownLatch go)throws Exception{go.await();try{return orders.datCOD(email,request,maps.chuanHoa(request.diaChi()));}catch(IllegalArgumentException ex){return null;}}
    @Test void addressOwnershipAndManualFallbackAreEnforced(){
        String email=user(),other=user();DiaChiChot address=maps.chuanHoa(address());addresses.tao(email,address,true);
        var saved=addresses.danhSach(email);assertThat(saved).hasSize(1);long id=((Number)saved.get(0).get("MaDiaChi")).longValue();
        assertThatThrownBy(()->addresses.xoa(other,id)).isInstanceOf(KhongTimThayException.class);
        var pin=new DiaChiYeuCau("Người nhận thử","0901234567","TP.HCM","Phường thử","123 Đường thử","","",new BigDecimal("10.7"),new BigDecimal("106.7"),true);
        assertThatThrownBy(()->maps.chuanHoa(pin)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Chưa thể xác minh");
        addresses.xoa(email,id);assertThat(addresses.danhSach(email)).isEmpty();
    }
    @Test void expiredCodReleasesOnlyOnce(){
        String email=user();carts.sua(email,1,1,true);var request=request(email,"");int before=held(1);String code=orders.datCOD(email,request,maps.chuanHoa(request.diaChi()));
        jdbc.sql("UPDATE \"DonHang\" SET \"HanXuLy\"=now()-interval '1 second' WHERE \"MaDonCongKhai\"=:c").param("c",code).update();
        orders.hetHanCOD(code);orders.hetHanCOD(code);assertThat(held(1)).isEqualTo(before);assertThat(orders.chiTiet(email,code)).containsEntry("TrangThaiDon","HET_HAN");
    }
    String coupon(int limit,int discount){
        String code="T"+UUID.randomUUID().toString().replace("-","").toUpperCase(Locale.ROOT);
        jdbc.sql("""
            INSERT INTO "MaGiamGia" ("KyHieu","MoTa","KieuGiam","GiaTriGiam","DonToiThieu","NgayBatDau","NgayKetThuc","TongLuotToiDa","LuotMoiKhach","CongKhai","DangHoatDong","NgayCapNhat")
            VALUES (:code,'Integration test','SO_TIEN',:discount,0,now()-interval '1 day',now()+interval '1 day',:lim,1,false,true,now())
            """).param("code",code).param("discount",discount).param("lim",limit).update();return code;
    }
    @Test void allocationAcrossMultipleLinesMatchesDiscountExactly(){
        String email=user();carts.sua(email,1,1,true);carts.sua(email,3,1,true);var request=request(email,coupon(1,99991));
        String code=orders.datCOD(email,request,maps.chuanHoa(request.diaChi()));
        try{
            var lines=orders.dong(email,code);assertThat(lines).hasSize(2);
            BigDecimal sum=lines.stream().map(l->(BigDecimal)l.get("GiamGiaPhanBo")).reduce(BigDecimal.ZERO,BigDecimal::add);
            assertThat(sum).isEqualByComparingTo("99991");
            for(var line:lines)assertThat((BigDecimal)line.get("GiamGiaPhanBo")).isBetween(BigDecimal.ZERO,((BigDecimal)line.get("DonGia")).multiply(BigDecimal.valueOf(((Number)line.get("SoLuong")).intValue())));
        }finally{orders.huy(email,code);}
    }
    @Test void onlyOneBuyerGetsTheLastVoucher()throws Exception{
        String a=user(),b=user(),coupon=coupon(1,1000);carts.sua(a,1,1,true);carts.sua(b,1,1,true);
        var ra=request(a,coupon);var rb=request(b,coupon);CountDownLatch go=new CountDownLatch(1);ExecutorService pool=Executors.newFixedThreadPool(2);List<Map.Entry<String,String>> successes=new ArrayList<>();
        try{
            var fa=pool.submit(()->attempt(a,ra,go));var fb=pool.submit(()->attempt(b,rb,go));go.countDown();
            String ca=fa.get(15,TimeUnit.SECONDS),cb=fb.get(15,TimeUnit.SECONDS);
            if(ca!=null)successes.add(Map.entry(a,ca));if(cb!=null)successes.add(Map.entry(b,cb));assertThat(successes).hasSize(1);
        }finally{pool.shutdownNow();for(var s:successes)orders.huy(s.getKey(),s.getValue());}
    }
}
