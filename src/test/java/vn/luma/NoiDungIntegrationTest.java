package vn.luma;

import java.time.*;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.business.*;
import vn.luma.web.*;
import static org.assertj.core.api.Assertions.*;
import static vn.luma.data.ThanhToanDAO.id;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_test","app.mail.local-directory=.local/test-mail","app.media.product-directory=.local/test-product-media","app.scheduling.enabled=false"})
@Transactional
class NoiDungIntegrationTest {
    static final String ADMIN="admin@luma.local";
    @Autowired NoiDungService content;
    @Autowired SanPhamService products;
    LocalDateTime now(){return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));}
    String slug(){return "qa-event-"+UUID.randomUUID();}
    long position(){return id(content.viTri(ADMIN).stream().filter(p->p.get("KyHieu").equals("TRANG_CHU")).findFirst().orElseThrow(),"MaViTriHienThi");}
    byte[] png()throws Exception{var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(32,16,BufferedImage.TYPE_INT_RGB),"png",out);return out.toByteArray();}
    SuKienYeuCau event(String slug,String state,LocalDateTime start,LocalDateTime end){return new SuKienYeuCau(null,"Sự kiện QA <script>test</script>",slug,"Nội dung nguyên văn <b>không thành HTML</b>",state,start,end);}
    BannerYeuCau banner(Long event,String target,boolean show,LocalDateTime start,LocalDateTime end){return new BannerYeuCau(position(),event,"Banner QA","Ảnh thử banner",target,0,show,start,end);}
    @Test void eventsOnlyAppearDuringPublishedScheduleAndNeverChangeProductPrices(){
        String slug=slug();var price=products.chiTiet("iphone-13").giaTu();long id=content.luuSuKien(ADMIN,null,event(slug,"NHAP",now().minusHours(1),null));assertThatThrownBy(()->content.suKien(slug)).isInstanceOf(KhongTimThayException.class);
        content.luuSuKien(ADMIN,id,event(slug,"CONG_BO",now().plusDays(1),null));assertThatThrownBy(()->content.suKien(slug)).isInstanceOf(KhongTimThayException.class);content.luuSuKien(ADMIN,id,event(slug,"CONG_BO",now().minusHours(1),now().plusHours(1)));assertThat(content.suKien(slug)).containsEntry("NoiDung","Nội dung nguyên văn <b>không thành HTML</b>");assertThat(content.suKien(1)).anyMatch(e->id(e,"MaSuKien")==id);assertThat(products.chiTiet("iphone-13").giaTu()).isEqualByComparingTo(price);
        content.luuSuKien(ADMIN,id,event(slug,"CONG_BO",now().minusDays(2),now().minusDays(1)));assertThatThrownBy(()->content.suKien(slug)).isInstanceOf(KhongTimThayException.class);assertThatThrownBy(()->content.luuSuKien(ADMIN,id,event(slug,"CONG_BO",now(),now().minusHours(1)))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void linkedBannerAndItsImageStayPrivateUntilBothSchedulesAllowPublication()throws Exception{
        String slug=slug();long event=content.luuSuKien(ADMIN,null,event(slug,"NHAP",now().minusHours(1),null));byte[] image=png();long banner=content.luuBanner(ADMIN,null,banner(event,"",true,now().minusHours(1),null),image);String path=(String)content.banner(ADMIN,banner).get("DuongDanAnh"),file=path.substring("/anh-banner/".length());assertThat(content.anh(ADMIN,banner)).isEqualTo(image);assertThat(content.bannerCongKhai("TRANG_CHU")).noneMatch(b->id(b,"MaBanner")==banner);assertThatThrownBy(()->content.anh(file)).isInstanceOf(KhongTimThayException.class);
        content.luuSuKien(ADMIN,event,event(slug,"CONG_BO",now().minusHours(1),null));assertThat(content.bannerCongKhai("TRANG_CHU")).anyMatch(b->id(b,"MaBanner")==banner&&b.get("dich").equals("/su-kien/"+slug));assertThat(content.anh(file)).isEqualTo(image);content.luuSuKien(ADMIN,event,event(slug,"AN",now().minusHours(1),null));assertThat(content.bannerCongKhai("TRANG_CHU")).noneMatch(b->id(b,"MaBanner")==banner);assertThatThrownBy(()->content.anh(file)).isInstanceOf(KhongTimThayException.class);assertThat(content.anh(ADMIN,banner)).isEqualTo(image);
    }
    @Test void bannerEditCanKeepImageAndUnsafeTargetsBadFilesAndWrongRolesAreRejected()throws Exception{
        byte[] image=png();long banner=content.luuBanner(ADMIN,null,banner(null,"/uu-dai",false,now().minusDays(1),null),image);String path=(String)content.banner(ADMIN,banner).get("DuongDanAnh");content.luuBanner(ADMIN,banner,banner(null,"/san-pham?loai=laptop",true,now().minusDays(1),null),null);assertThat(content.banner(ADMIN,banner)).containsEntry("DuongDanAnh",path);assertThat(content.bannerCongKhai("TRANG_CHU")).anyMatch(b->id(b,"MaBanner")==banner&&b.get("dich").equals("/san-pham?loai=laptop"));
        for(String bad:List.of("https://example.test","//example.test","/\\evil","/san-pham?next=//evil","/quan-tri","/%2f%2fevil","/uu-dai\r\nLocation:evil"))assertThatThrownBy(()->content.luuBanner(ADMIN,banner,banner(null,bad,true,now().minusDays(1),null),null)).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->content.luuBanner(ADMIN,null,banner(null,"",true,now(),null),new byte[]{1,2,3})).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->content.luuBanner("khach@luma.local",banner,banner(null,"",false,now(),null),null)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);assertThatThrownBy(()->content.anh("../private.txt")).isInstanceOf(KhongTimThayException.class);
        assertThatThrownBy(()->content.luuBanner(ADMIN,null,banner(null,"",true,now(),null),null)).isInstanceOf(IllegalArgumentException.class);assertThat(content.banner(ADMIN,banner)).containsEntry("DuongDanDich","/san-pham?loai=laptop");
    }
}
