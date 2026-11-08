package vn.luma;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.business.*;
import vn.luma.web.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false"})
@Transactional
class CauHinhSanPhamIntegrationTest {
    static final String ADMIN="admin@luma.local";
    @Autowired CauHinhSanPhamService config;
    @Autowired QuanLySanPhamService products;
    @Autowired TaiKhoanService users;
    @Autowired GioHangService carts;
    @Autowired DonHangService orders;
    @Autowired BanDoService maps;
    @Autowired SanPhamService shop;
    @Autowired JdbcClient jdbc;
    String key(){return "QA_"+UUID.randomUUID().toString().replace("-","").substring(0,16).toUpperCase(Locale.ROOT);}
    String user(){String e="spec-"+UUID.randomUUID()+"@example.test";users.seedDemo(e,"Khách cấu hình thử","KhachHang","StrongDemo!2026");return e;}
    long attr(long category,String type,String name,String unit){return config.luuThuocTinh(ADMIN,null,new ThuocTinhYeuCau(category,key(),name,type,unit,true,0));}
    @Test void productLineRequiresSameBrandAndUsedLineIsProtected(){
        long line=config.luuDong(ADMIN,null,new DongSanPhamYeuCau(1,"Dòng thử","qa-line-"+UUID.randomUUID()));config.ganDong(ADMIN,1,line);assertThat(products.model(ADMIN,1)).containsEntry("MaDongSanPham",line);
        long brand=products.luuMuc(ADMIN,"thuong-hieu",null,new MucDanhMucYeuCau("Hãng khác "+key(),"qa-brand-"+UUID.randomUUID(),"",0,true));long other=config.luuDong(ADMIN,null,new DongSanPhamYeuCau(brand,"Dòng hãng khác","qa-other"));
        assertThatThrownBy(()->config.ganDong(ADMIN,1,other)).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->config.xoaDong(ADMIN,line)).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->config.luuDong(ADMIN,line,new DongSanPhamYeuCau(brand,"Đổi hãng","qa-change"))).isInstanceOf(IllegalArgumentException.class);
        var model=products.model(ADMIN,1);assertThatThrownBy(()->products.luuModel(ADMIN,1L,new NoiDungSanPhamYeuCau(1,brand,(String)model.get("TenSanPham"),(String)model.get("DuongDan"),(String)model.get("MoTaNgan"),"",12,"",true))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("bỏ liên kết");
        config.ganDong(ADMIN,1,null);config.xoaDong(ADMIN,line);assertThat(config.dong(ADMIN,1,1)).noneMatch(d->((Number)d.get("MaDongSanPham")).longValue()==line);
    }
    @Test void valuesUseOneCorrectTypeAndCannotCrossModelOrCategory(){
        long number=attr(1,"SO","Camera thử","MP"),text=attr(1,"CHU","Chuẩn mạng thử",""),flag=attr(1,"CO_KHONG","Chống nước thử",null),wrong=attr(2,"CHU","Thuộc tính danh mục khác",null);
        config.luuGiaTri(ADMIN,1,1,number,new GiaTriThuocTinhYeuCau("48.1250"));config.luuGiaTri(ADMIN,1,1,text,new GiaTriThuocTinhYeuCau("Wi-Fi thử <script>test</script>"));config.luuGiaTri(ADMIN,1,1,flag,new GiaTriThuocTinhYeuCau("false"));
        var rows=config.giaTri(ADMIN,1,1);var numeric=rows.stream().filter(r->((Number)r.get("MaThuocTinh")).longValue()==number).findFirst().orElseThrow();assertThat((BigDecimal)numeric.get("GiaTriSo")).isEqualByComparingTo("48.125");assertThat(numeric.get("GiaTriChu")).isNull();assertThat(numeric.get("GiaTriCoKhong")).isNull();assertThat(config.congKhai("iphone-13",1)).anyMatch(r->"Không".equals(r.get("hienThi"))).anyMatch(r->"48.125 MP".equals(r.get("hienThi")));assertThat(config.congKhai("iphone-13",2)).isEmpty();
        assertThatThrownBy(()->config.luuGiaTri(ADMIN,1,1,wrong,new GiaTriThuocTinhYeuCau("Khác danh mục"))).isInstanceOf(IllegalArgumentException.class);long foreign=jdbc.sql("SELECT \"MaBienThe\" FROM \"BienTheSanPham\" WHERE \"MaSanPham\"<>1 ORDER BY \"MaBienThe\" LIMIT 1").query(Long.class).single();assertThatThrownBy(()->config.giaTri(ADMIN,1,foreign)).isInstanceOf(KhongTimThayException.class);
        assertThatThrownBy(()->config.luuGiaTri(user(),1,1,number,new GiaTriThuocTinhYeuCau("50"))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void usedDefinitionCannotChangeTypeCategoryOrBeDeletedAndInputLimitsAreStrict(){
        long id=attr(1,"SO","Thông số thử","mm");config.luuGiaTri(ADMIN,1,1,id,new GiaTriThuocTinhYeuCau("10"));var row=config.thuocTinh(ADMIN,id);String code=(String)row.get("KyHieu");
        assertThatThrownBy(()->config.luuThuocTinh(ADMIN,id,new ThuocTinhYeuCau(1,code,"Đổi kiểu","CHU","",true,0))).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->config.luuThuocTinh(ADMIN,id,new ThuocTinhYeuCau(2,code,"Đổi danh mục","SO","",true,0))).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->config.xoaThuocTinh(ADMIN,id)).isInstanceOf(IllegalArgumentException.class);
        for(String bad:List.of("NaN","1.00001","1000000000000","1e99999","-1e-5"))assertThatThrownBy(()->config.luuGiaTri(ADMIN,1,1,id,new GiaTriThuocTinhYeuCau(bad))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->CauHinhSanPhamService.docGiaTri("CO_KHONG","maybe")).isInstanceOf(IllegalArgumentException.class);assertThat(CauHinhSanPhamService.docGiaTri("SO","-0.1250")).isEqualTo(new BigDecimal("-0.125"));
        config.xoaGiaTri(ADMIN,1,1,id);config.xoaThuocTinh(ADMIN,id);assertThatThrownBy(()->config.thuocTinh(ADMIN,id)).isInstanceOf(KhongTimThayException.class);
    }
    @Test void orderSnapshotsAttributeNamesValuesAndUnitsBeforeLaterEdits(){
        long id=attr(1,"SO","Camera tại lúc đặt","MP");config.luuGiaTri(ADMIN,1,1,id,new GiaTriThuocTinhYeuCau("48"));String email=user();carts.sua(email,1,1,true);var address=new DiaChiYeuCau("Khách thử","0901234567","TP.HCM","Phường thử","123 Đường thử","","",null,null,false);var request=new DatHangYeuCau(UUID.randomUUID().toString(),orders.noiDungGio(email),"",address);String order=orders.datCOD(email,request,maps.chuanHoa(address));
        String snapshot=orders.dong(email,order).getFirst().get("CauHinhLucDat").toString();assertThat(orders.dong(email,order).getFirst()).containsEntry("ThongSoMoRongChot","Camera tại lúc đặt: 48 MP");
        config.luuGiaTri(ADMIN,1,1,id,new GiaTriThuocTinhYeuCau("108"));String code=(String)config.thuocTinh(ADMIN,id).get("KyHieu");config.luuThuocTinh(ADMIN,id,new ThuocTinhYeuCau(1,code,"Camera tên mới","SO","megapixel",true,0));assertThat(config.congKhai("iphone-13",1)).anyMatch(r->"108 megapixel".equals(r.get("hienThi")));assertThat(orders.dong(email,order).getFirst().get("CauHinhLucDat").toString()).isEqualTo(snapshot);assertThat(orders.dong(email,order).getFirst()).containsEntry("ThongSoMoRongChot","Camera tại lúc đặt: 48 MP");orders.huy(email,order);
    }
    @Test void extendedFiltersMustAllMatchSameSkuAndSelectItsPriceAndLink(){
        long a=attr(1,"CHU","Mạng thử",null),b=attr(1,"SO","Camera thử","MP"),flag=attr(1,"CO_KHONG","NFC thử",null);
        var f=new BoLocSanPham("iPhone 13","","",null,null,null,null,"","",false,"mac-dinh",1);
        config.luuGiaTri(ADMIN,1,1,a,new GiaTriThuocTinhYeuCau("Wi-Fi 6+"));config.luuGiaTri(ADMIN,1,2,b,new GiaTriThuocTinhYeuCau("48"));config.luuGiaTri(ADMIN,1,2,flag,new GiaTriThuocTinhYeuCau("false"));
        var extra=new BoLocBoSung(null,Map.of(a,"wi-fi 6+",b,"48.0000",flag,"false"));assertThat(shop.loc(f,extra).tong()).isZero();
        config.luuGiaTri(ADMIN,1,2,a,new GiaTriThuocTinhYeuCau("Wi-Fi 6+"));var result=shop.loc(f,extra);assertThat(result.tong()).isEqualTo(1);assertThat(result.noiDung().getFirst().maSkuGoiY()).isEqualTo(2);assertThat(result.noiDung().getFirst().giaTu()).isEqualByComparingTo(shop.bienThe("iphone-13").stream().filter(v->v.maBienThe()==2).findFirst().orElseThrow().giaBan());
        long line=config.luuDong(ADMIN,null,new DongSanPhamYeuCau(1,"Dòng lọc thử","qa-filter-"+UUID.randomUUID()));assertThat(shop.loc(f,new BoLocBoSung(line,Map.of())).tong()).isZero();config.ganDong(ADMIN,1,line);assertThat(shop.loc(f,new BoLocBoSung(line,extra.thuocTinh())).tong()).isEqualTo(1);
        assertThatThrownBy(()->shop.loc(f,new BoLocBoSung(null,Map.of(b,"48MP")))).isInstanceOf(IllegalArgumentException.class);
        String code=(String)config.thuocTinh(ADMIN,a).get("KyHieu");config.luuThuocTinh(ADMIN,a,new ThuocTinhYeuCau(1,code,"Mạng đã tắt lọc","CHU",null,false,0));assertThatThrownBy(()->shop.loc(f,extra)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("không còn được phép lọc");
    }
}
