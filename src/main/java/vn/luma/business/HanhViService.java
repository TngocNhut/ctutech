package vn.luma.business;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.data.HanhViDAO;

@Service
public class HanhViService {
    private final HanhViDAO dao;private final TaiKhoanService users;private final NoiDungService editorial;
    public HanhViService(HanhViDAO dao,TaiKhoanService users,NoiDungService editorial){this.dao=dao;this.users=users;this.editorial=editorial;}
    public long tao(String key,String email){UUID.fromString(key);return dao.tao(key,email==null?null:users.hienTai(email).maNguoiDung());}
    private void key(String key){if(key==null||!key.matches("[a-f0-9-]{36}"))throw new IllegalArgumentException("Mã sự kiện không hợp lệ.");UUID.fromString(key);}
    @Transactional public void nhip(long id,String sessionKey,boolean active){dao.khoa(id,sessionKey);dao.nhip(id,active);}
    @Transactional public void suKien(long id,String sessionKey,String type,String group,String key,Long sku){suKien(id,sessionKey,type,group,key,sku,null);}
    @Transactional public void suKien(long id,String sessionKey,String type,String group,String key,Long sku,Long banner){key(key);if(group==null||type==null||!Set.of("TRANG_CHU","DANH_SACH","CHI_TIET","UU_DAI","SU_KIEN","GIO_HANG","DAT_HANG","DON_HANG","TAI_KHOAN").contains(group)||!Set.of("XEM_TRANG","TIM_KIEM","LOC_DANH_MUC","BAM_BANNER").contains(type))throw new IllegalArgumentException("Sự kiện không hợp lệ.");if(type.equals("BAM_BANNER")){if(banner==null||!Set.of("TRANG_CHU","DANH_SACH","UU_DAI").contains(group)||editorial.bannerCongKhai(group.equals("UU_DAI")?"UU_DAI":"TRANG_CHU").stream().noneMatch(b->((Number)b.get("MaBanner")).longValue()==banner))throw new IllegalArgumentException("Banner không hiện công khai.");}else if(banner!=null)throw new IllegalArgumentException("Sự kiện này không nhận mã banner.");dao.khoa(id,sessionKey);if(dao.dem(id)>=5000)return;boolean inserted=dao.suKien(id,type,group,key,banner);if(inserted&&sku!=null&&type.equals("XEM_TRANG")&&group.equals("CHI_TIET")){long product=dao.sanPham(sku).orElseThrow(()->new IllegalArgumentException("SKU không công khai."));dao.tuongTac(id,product,sku,"XEM",key);}}
    @Transactional public void theoDoi(long id,String sessionKey,long product){dao.khoa(id,sessionKey);if(dao.dem(id)>=5000)return;dao.tuongTac(id,product,null,"THEO_DOI","follow-"+product);}
    @Transactional public void themGio(long id,String sessionKey,long sku){dao.khoa(id,sessionKey);if(dao.dem(id)>=5000)return;dao.sanPham(sku).ifPresent(product->dao.tuongTac(id,product,sku,"THEM_GIO",UUID.randomUUID().toString()));}
    @Transactional public void donDep(){dao.donDep();}
}
