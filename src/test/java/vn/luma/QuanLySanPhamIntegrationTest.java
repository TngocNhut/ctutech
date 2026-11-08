package vn.luma;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import vn.luma.business.*;
import vn.luma.web.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.mail.local-directory=.local/test-mail","app.scheduling.enabled=false","app.media.product-directory=.local/test-product-media"})
@Transactional
class QuanLySanPhamIntegrationTest {
    static final String ADMIN="admin@luma.local";
    @Autowired QuanLySanPhamService admin;
    @Autowired SanPhamService shop;
    @Autowired TaiKhoanService users;
    @Autowired GioHangService carts;
    @Autowired DonHangService orders;
    @Autowired BanDoService maps;
    @Autowired JdbcClient jdbc;
    @Autowired AnhSanPhamService images;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    NoiDungSanPhamYeuCau model(String slug,boolean visible){return new NoiDungSanPhamYeuCau(1,1,"Model thử "+slug,slug,"Mô tả ngắn dùng cho kiểm thử","Nội dung chi tiết\nGiữ dòng mới",12,"https://example.test/product",visible);}
    NoiDungBienTheYeuCau sku(String code,String name,int stock,String price){return new NoiDungBienTheYeuCau(code,"TEST-PART",name,"Đen","Chip thử","",8,256,new BigDecimal("6.1"),"Hệ điều hành thử",true,new CapNhatBienThe(new BigDecimal(price),new BigDecimal("12000000"),new BigDecimal("7000000"),stock,"Kiểm thử quản trị SKU"));}
    String slug(){return "crud-"+UUID.randomUUID();}
    int count(String table,long product){return jdbc.sql("SELECT count(*) FROM \""+table+"\" WHERE \"MaSanPham\"=:id").param("id",product).query(Integer.class).single();}
    @Test void createsModelSkuAndStockWithPriceHistoryThenUpdatesAndHides(){
        String slug=slug();long product=admin.luuModel(ADMIN,null,model(slug,true));assertThatThrownBy(()->shop.chiTiet(slug)).isInstanceOf(KhongTimThayException.class);
        String code="T-"+UUID.randomUUID();long sku=admin.luuSku(ADMIN,product,null,sku(code,"8 GB / 256 GB · Đen",3,"10000000"));
        assertThat(shop.chiTiet(slug).maSkuGoiY()).isEqualTo(sku);assertThat(shop.chiTiet(slug).tonKhaDung()).isEqualTo(3);
        assertThat(jdbc.sql("SELECT sum(\"ThayDoiTon\") FROM \"BienDongKho\" WHERE \"MaBienThe\"=:id").param("id",sku).query(Integer.class).single()).isEqualTo(3);
        assertThat(jdbc.sql("SELECT count(*) FROM \"LichSuGiaBienThe\" WHERE \"MaBienThe\"=:id").param("id",sku).query(Integer.class).single()).isEqualTo(1);
        admin.luuSku(ADMIN,product,sku,sku(code,"Tên cấu hình đã sửa",4,"11000000"));assertThat(shop.bienThe(slug).getFirst().tenBienThe()).isEqualTo("Tên cấu hình đã sửa");assertThat(shop.bienThe(slug).getFirst().soLuongTon()).isEqualTo(4);
        assertThat(jdbc.sql("SELECT count(*) FROM \"LichSuGiaBienThe\" WHERE \"MaBienThe\"=:id").param("id",sku).query(Integer.class).single()).isEqualTo(2);
        admin.ngungSku(ADMIN,sku);assertThatThrownBy(()->shop.chiTiet(slug)).isInstanceOf(KhongTimThayException.class);assertThat(admin.bienThe(ADMIN,product)).hasSize(1);
        admin.luuSku(ADMIN,product,sku,sku(code,"Tên cấu hình đã sửa",4,"11000000"));admin.anModel(ADMIN,product);assertThatThrownBy(()->shop.chiTiet(slug)).isInstanceOf(KhongTimThayException.class);assertThat(admin.model(ADMIN,product)).containsEntry("DangHienThi",false);
    }
    @Test void categoryAndBrandCrudAreRestrictedToAdministratorAndKeepHiddenRecords(){
        String slug=slug();var form=new MucDanhMucYeuCau("Danh mục "+slug,slug,"Mô tả",5,true);
        assertThatThrownBy(()->admin.luuMuc("khach@luma.local","phan-loai",null,form)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        for(String type:List.of("phan-loai","thuong-hieu")){long id=admin.luuMuc(ADMIN,type,null,form);assertThat(admin.muc(ADMIN,type,id)).containsEntry("Ten",form.ten());admin.anMuc(ADMIN,type,id);assertThat(admin.muc(ADMIN,type,id)).containsEntry("DangHienThi",false);admin.luuMuc(ADMIN,type,id,new MucDanhMucYeuCau("Tên đã sửa "+slug,slug,"Mô tả mới",10,true));assertThat(admin.muc(ADMIN,type,id)).containsEntry("DangHienThi",true);}
        assertThatThrownBy(()->admin.danhMuc(ADMIN,"NguoiDung")).isInstanceOf(KhongTimThayException.class);
        assertThatThrownBy(()->admin.luuModel("khach@luma.local",null,model(slug,true))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void existingOrderKeepsProductPriceAndNameSnapshotsAfterAdminEdit(){
        String slug=slug();long product=admin.luuModel(ADMIN,null,model(slug,true));String skuCode="T-"+UUID.randomUUID();long sku=admin.luuSku(ADMIN,product,null,sku(skuCode,"Cấu hình cũ",3,"10000000"));
        String email="crud-"+UUID.randomUUID()+"@example.test";users.seedDemo(email,"Khách CRUD","KhachHang","StrongDemo!2026");carts.sua(email,sku,1,true);
        var address=new DiaChiYeuCau("Khách CRUD","0901234567","TP.HCM","Phường Bến Nghé","123 Nguyễn Huệ","","",null,null,false);var request=new DatHangYeuCau(UUID.randomUUID().toString(),orders.noiDungGio(email),"",address);
        String code=orders.datCOD(email,request,maps.chuanHoa(address));String name=(String)orders.dong(email,code).getFirst().get("TenSanPhamLucDat");
        admin.luuModel(ADMIN,product,new NoiDungSanPhamYeuCau(1,1,"Tên model mới",slug,"Mô tả đã đổi","Đã sửa",24,"https://example.test/new",true));admin.luuSku(ADMIN,product,sku,sku(skuCode,"Cấu hình mới",3,"11000000"));
        assertThat(orders.dong(email,code).getFirst()).containsEntry("TenSanPhamLucDat",name);assertThat((BigDecimal)orders.dong(email,code).getFirst().get("DonGia")).isEqualByComparingTo("10000000");assertThat(shop.chiTiet(slug).tenSanPham()).isEqualTo("Tên model mới");
    }
    @Test void cannotAttachExistingSkuToAnotherModelOrUseUnsafeSource(){
        String slug=slug();long product=admin.luuModel(ADMIN,null,model(slug,true));
        assertThatThrownBy(()->admin.luuSku(ADMIN,product,1L,sku("OTHER","Không được đổi model",0,"10000000"))).isInstanceOf(KhongTimThayException.class);
        var form=new NoiDungSanPhamYeuCau(1,1,"Unsafe",slug+"-bad","Mô tả","",12,"javascript:alert(1)",true);assertThatThrownBy(()->admin.luuModel(ADMIN,null,form)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED) void duplicateSkuRollsBackProductCreation(){
        long[] product={0};String slug=slug();assertThatThrownBy(()->new TransactionTemplate(transactionManager).execute(status->{product[0]=admin.luuModel(ADMIN,null,model(slug,true));admin.luuSku(ADMIN,product[0],null,sku("DEMO-001-01","Trùng mã gốc",1,"10000000"));return null;})).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Mã SKU đã tồn tại");
        assertThat(count("BienTheSanPham",product[0])).isZero();assertThat(jdbc.sql("SELECT count(*) FROM \"SanPham\" WHERE \"MaSanPham\"=:id").param("id",product[0]).query(Integer.class).single()).isZero();
    }
    byte[] png(int width,int height)throws Exception{var image=new java.awt.image.BufferedImage(width,height,java.awt.image.BufferedImage.TYPE_INT_RGB);try(var out=new java.io.ByteArrayOutputStream()){javax.imageio.ImageIO.write(image,"png",out);return out.toByteArray();}}
    @Test void uploadedDraftImageRequiresAdminThenFollowsModelAndImageVisibility()throws Exception{
        String slug=slug();long product=admin.luuModel(ADMIN,null,model(slug,false));String code="IMG-"+UUID.randomUUID();long sku=admin.luuSku(ADMIN,product,null,sku(code,"SKU có ảnh",1,"10000000"));
        byte[] bytes=png(8,8);var form=new AnhSanPhamYeuCau(null,"Ảnh kiểm thử tự tạo",0,"","Tự tạo cho kiểm thử",true);long image=images.tao(ADMIN,product,form,bytes);
        String route=(String)images.chiTiet(ADMIN,product,image).get("DuongDanTep");String file=route.substring(route.lastIndexOf('/')+1);
        assertThat(images.doc(ADMIN,file)).isEqualTo(bytes);assertThatThrownBy(()->images.doc(null,file)).isInstanceOf(KhongTimThayException.class);
        admin.luuModel(ADMIN,product,model(slug,true));assertThat(images.doc(null,file)).isEqualTo(bytes);assertThat(shop.anh(slug,sku)).hasSize(1);
        long specific=images.tao(ADMIN,product,new AnhSanPhamYeuCau(sku,"Ảnh đúng SKU",100,"https://example.test/sku-photo","Ảnh tự tạo",true),png(9,9));
        assertThat(((Number)shop.anh(slug,sku).getFirst().get("MaTep")).longValue()).isEqualTo(specific);assertThat(shop.anh(slug,sku)).hasSize(2);
        assertThatThrownBy(()->images.sua(ADMIN,product,image,new AnhSanPhamYeuCau(1L,"SKU khác model",0,"","",true))).isInstanceOf(IllegalArgumentException.class);
        images.an(ADMIN,product,image);assertThat(shop.anh(slug,sku)).hasSize(1);images.an(ADMIN,product,specific);assertThat(shop.anh(slug,sku)).isEmpty();assertThatThrownBy(()->images.doc("khach@luma.local",file)).isInstanceOf(KhongTimThayException.class);assertThat(images.doc(ADMIN,file)).isEqualTo(bytes);
        assertThatThrownBy(()->images.doc(null,"../db-password.txt")).isInstanceOf(KhongTimThayException.class);
    }
    @Test void productUploadRejectsNonImageOversizeAndOversizedDimensions()throws Exception{
        long product=admin.luuModel(ADMIN,null,model(slug(),false));var form=new AnhSanPhamYeuCau(null,"Ảnh thử",0,"","",true);
        assertThatThrownBy(()->images.tao("khach@luma.local",product,form,png(8,8))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        for(byte[] bytes:List.of("%PDF-not-an-image".getBytes(),new byte[6*1024*1024],new byte[]{(byte)137,80,78,71,13,10,26,10},png(6001,1)))assertThatThrownBy(()->images.tao(ADMIN,product,form,bytes)).isInstanceOf(IllegalArgumentException.class);
        assertThat(images.danhSach(ADMIN,product)).isEmpty();
    }
}
