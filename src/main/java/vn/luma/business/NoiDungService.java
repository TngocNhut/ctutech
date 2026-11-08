package vn.luma.business;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import vn.luma.data.*;
import vn.luma.web.*;

@Service
public class NoiDungService {
    private final NoiDungDAO dao;private final TaiKhoanService users;private final QuanLySanPhamDAO audit;private final KhuyenMaiDAO campaigns;private final KhoAnhSanPham images;
    public NoiDungService(NoiDungDAO dao,TaiKhoanService users,QuanLySanPhamDAO audit,KhuyenMaiDAO campaigns,KhoAnhSanPham images){this.dao=dao;this.users=users;this.audit=audit;this.campaigns=campaigns;this.images=images;}
    private void trang(int page){if(page<1||page>10000)throw new IllegalArgumentException("Trang không hợp lệ.");}
    private void lich(LocalDateTime start,LocalDateTime end){if(start==null||end!=null&&!end.isAfter(start))throw new IllegalArgumentException("Kết thúc phải sau bắt đầu; thời gian dùng giờ Việt Nam.");}
    public List<Map<String,Object>> suKien(String email,int page){users.quanTri(email);trang(page);return dao.suKien(true,page);}
    public List<Map<String,Object>> suKien(int page){trang(page);return dao.suKien(false,page);}
    public Map<String,Object> suKien(String email,long id){users.quanTri(email);return dao.suKien(id,false);}
    public Map<String,Object> suKien(String slug){return dao.suKien(slug).orElseThrow(()->new KhongTimThayException("Sự kiện chưa công bố hoặc đã hết lịch hiển thị."));}
    @Transactional public long luuSuKien(String email,Long id,SuKienYeuCau f){long actor=users.quanTri(email).maNguoiDung();lich(f.batDau(),f.ketThuc());if(id!=null)dao.suKien(id,true);if(f.chuongTrinh()!=null)campaigns.chuongTrinh(f.chuongTrinh(),false);try{long saved=dao.luuSuKien(id,actor,f);audit.log(actor,saved,"LUU_SU_KIEN","SU_KIEN");return saved;}catch(DuplicateKeyException ex){throw new IllegalArgumentException("Đường dẫn sự kiện đã được dùng.");}}
    public List<Map<String,Object>> viTri(String email){users.quanTri(email);return dao.viTri();}
    public List<Map<String,Object>> banner(String email,int page){users.quanTri(email);trang(page);return dao.banner(page);}
    public Map<String,Object> banner(String email,long id){users.quanTri(email);return dao.banner(id,false);}
    public static boolean dichAnToan(String path){return path==null||path.isBlank()||Set.of("/","/san-pham","/uu-dai","/su-kien","/san-pham?loai=dien-thoai","/san-pham?loai=laptop","/san-pham?loai=tai-nghe","/san-pham?loai=chuot-ban-phim","/san-pham?loai=phu-kien").contains(path)||path.matches("/san-pham/[a-z0-9]+(?:-[a-z0-9]+)*(?:\\?sku=[1-9][0-9]{0,18})?");}
    @Transactional public long luuBanner(String email,Long id,BannerYeuCau f,byte[] image){long actor=users.quanTri(email).maNguoiDung();lich(f.batDau(),f.ketThuc());if(!dichAnToan(f.dich())||f.suKien()!=null&&f.dich()!=null&&!f.dich().isBlank())throw new IllegalArgumentException("Chọn sự kiện hoặc một đích nội bộ hợp lệ, không chọn cả hai.");if(dao.viTri().stream().noneMatch(p->((Number)p.get("MaViTriHienThi")).longValue()==f.viTri()))throw new IllegalArgumentException("Chọn vị trí được giao diện hỗ trợ.");if(f.suKien()!=null)dao.suKien(f.suKien(),false);String path=id==null?null:(String)dao.banner(id,true).get("DuongDanAnh");if(image!=null&&image.length>0)path="/anh-banner/"+images.luu(image);if(path==null)throw new IllegalArgumentException("Hãy tải ảnh PNG hoặc JPEG cho banner mới.");long saved=dao.luuBanner(id,actor,f,path);audit.log(actor,saved,"LUU_BANNER","BANNER");return saved;}
    public List<Map<String,Object>> bannerCongKhai(String position){if(!Set.of("TRANG_CHU","UU_DAI").contains(position))throw new IllegalArgumentException("Vị trí không hợp lệ.");return dao.bannerCongKhai(position).stream().map(b->{var row=new HashMap<>(b);row.put("dich",b.get("MaSuKien")==null?Objects.toString(b.get("DuongDanDich"),""):"/su-kien/"+b.get("slug_su_kien"));return (Map<String,Object>)row;}).toList();}
    public byte[] anh(String file){if(!dao.anhCongKhai(file))throw new KhongTimThayException("Ảnh banner chưa được công bố.");return images.doc(file);}
    public byte[] anh(String email,long id){var row=banner(email,id);String path=(String)row.get("DuongDanAnh");return images.doc(path.substring("/anh-banner/".length()));}
}
