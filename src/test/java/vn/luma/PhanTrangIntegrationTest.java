package vn.luma;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.business.*;
import vn.luma.web.*;
import static org.assertj.core.api.Assertions.*;
import static vn.luma.data.ThanhToanDAO.id;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false"})
class PhanTrangIntegrationTest {
    static final String ADMIN="admin@luma.local";
    @Autowired QuanLySanPhamService products;
    @Autowired KhoService stock;
    @Autowired KhuyenMaiService offers;
    @Autowired DonHangService orders;
    @Autowired GiaoNhanService shipping;
    @Autowired TraHangService returns;
    @Autowired HoanTienService refunds;
    @Autowired TaiKhoanService users;
    @Autowired GioHangService carts;
    @Autowired BanDoService maps;
    @LocalServerPort int port;
    @Test @Transactional void moreThanOnePageOfHiddenModelsAndSkusRemainEditableAndSearchIsLiteral(){
        String key="qa-page-"+UUID.randomUUID();var ids=new ArrayList<Long>();var skus=new ArrayList<Long>();
        for(int i=0;i<31;i++){long model=products.luuModel(ADMIN,null,new NoiDungSanPhamYeuCau(1,1,key+" "+i,key+"-"+i,"Model kiểm thử phân trang","",12,"",false));ids.add(model);skus.add(products.luuSku(ADMIN,model,null,new NoiDungBienTheYeuCau(key+"-"+i,"","SKU kiểm thử","Đen","Chip thử","",8,128,new BigDecimal("6.1"),"Hệ thử",false,new CapNhatBienThe(new BigDecimal("100000"),new BigDecimal("120000"),new BigDecimal("80000"),0,"Tạo SKU thử phân trang"))));}
        var first=products.danhSach(ADMIN,key,1);var second=products.danhSach(ADMIN,key,2);assertThat(first).hasSize(30);assertThat(second).hasSize(1);assertThat(id(second.getFirst(),"MaSanPham")).isEqualTo(ids.getFirst());assertThat(first).noneMatch(p->id(p,"MaSanPham")==ids.getFirst());assertThat(products.model(ADMIN,ids.getFirst())).containsEntry("MaSanPham",ids.getFirst());
        assertThat(stock.danhSach(ADMIN,key,1)).hasSize(30);assertThat(stock.danhSach(ADMIN,key,2)).singleElement().satisfies(v->assertThat(v.maBienThe()).isEqualTo(skus.getLast()));assertThat(stock.sku(ADMIN,skus.getLast()).giaBan()).isEqualByComparingTo("100000");assertThat(products.danhSach(ADMIN,"%",1)).isEmpty();assertThat(stock.danhSach(ADMIN,"%",1)).isEmpty();
        assertThatThrownBy(()->stock.danhSach("khach@luma.local",key,2)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);assertThatThrownBy(()->stock.danhSach(ADMIN,key,0)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test @Transactional void oldOrdersStayReachableOnLaterPagesAndOwnerScopeSurvivesPagination(){
        String email="page-orders-"+UUID.randomUUID()+"@example.test";users.seedDemo(email,"Khách thử phân trang","KhachHang","OrderPage!2026");var codes=new ArrayList<String>();var address=new DiaChiYeuCau("Khách thử","0901234567","TP.HCM","Phường thử","123 Đường thử","","",null,null,false);
        for(int i=0;i<31;i++){carts.sua(email,1,1,true);String code=orders.datCOD(email,new DatHangYeuCau(UUID.randomUUID().toString(),orders.noiDungGio(email),"",address),maps.chuanHoa(address));codes.add(code);orders.huy(email,code);}
        assertThat(orders.danhSach(email,1)).hasSize(30);assertThat(orders.danhSach(email,2)).singleElement().satisfies(d->assertThat(d).containsEntry("MaDonCongKhai",codes.getFirst()));assertThat(orders.chiTiet(email,codes.getFirst())).containsEntry("TrangThaiDon","DA_HUY");assertThat(shipping.danhSach(ADMIN,"DA_HUY",1)).hasSize(30);assertThat(shipping.danhSach(ADMIN,"DA_HUY",2).getFirst()).containsEntry("MaDonCongKhai",codes.getFirst());assertThat(orders.danhSach("khach@luma.local",2)).noneMatch(d->codes.contains(d.get("MaDonCongKhai")));assertThatThrownBy(()->orders.chiTiet("khach@luma.local",codes.getFirst())).isInstanceOf(KhongTimThayException.class);assertThatThrownBy(()->shipping.danhSach(ADMIN,"INVALID",1)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test @Transactional void campaignsAndPublicVouchersBeyondThirtyCanBeTraversedWithoutOverlap(){
        String key="P-"+UUID.randomUUID().toString().substring(0,18).toUpperCase(Locale.ROOT);var now=LocalDateTime.now();var campaigns=new ArrayList<Long>();var vouchers=new ArrayList<Long>();
        for(int i=0;i<31;i++){campaigns.add(offers.luu(ADMIN,null,new ChuongTrinhYeuCau(key+i,"Chiến dịch trang "+i,"",now.minusDays(1),now.plusDays(1),0,"NHAP",false)));vouchers.add(offers.luuVoucher(ADMIN,null,new VoucherYeuCau(key+i,"Mã thử phân trang","SO_TIEN",new BigDecimal("100"),null,BigDecimal.ZERO,now.minusDays(1),now.plusDays(1),5,1,true,true)));}
        assertThat(offers.danhSach(ADMIN,1)).hasSize(30);assertThat(offers.danhSach(ADMIN,2).getFirst()).containsEntry("MaChuongTrinh",campaigns.getFirst());assertThat(offers.vouchers(ADMIN,2).getFirst()).containsEntry("MaGiamGia",vouchers.getFirst());assertThat(offers.congKhai(1)).hasSize(30);assertThat(offers.congKhai(2).getFirst()).containsEntry("MaGiamGia",vouchers.getFirst());assertThat(returns.danhSach(ADMIN,10000)).isEmpty();assertThat(refunds.danhSach(ADMIN,10000)).isEmpty();
    }
    @Test void invalidPublicPageGetsUseful400AndOffersPaginationRenders()throws Exception{
        var client=HttpClient.newHttpClient();var valid=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/uu-dai?trang=2")).GET().build(),HttpResponse.BodyHandlers.ofString());assertThat(valid.statusCode()).isEqualTo(200);assertThat(valid.body()).contains("Trang trước","trang=1");var bad=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/uu-dai?trang=0")).GET().build(),HttpResponse.BodyHandlers.ofString());assertThat(bad.statusCode()).isEqualTo(400);assertThat(bad.body()).contains("Yêu cầu chưa hợp lệ.");
    }
}
