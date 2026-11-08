package vn.luma.business;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.data.ThongBaoDAO;

@Service
public class ThongBaoService {
    private final ThongBaoDAO dao;private final TaiKhoanService users;
    public ThongBaoService(ThongBaoDAO dao,TaiKhoanService users){this.dao=dao;this.users=users;}
    public static boolean dichAnToan(String target){return target!=null&&(target.equals("/uu-dai")||target.equals("/tai-khoan/thong-bao")||target.matches("/san-pham/[a-z0-9]+(?:-[a-z0-9]+)*(?:\\?sku=[1-9][0-9]{0,18})?")||target.matches("/don-hang/LM-[A-Z0-9]{32}")||target.matches("/tai-khoan/(?:danh-gia|ho-tro)/[1-9][0-9]{0,18}"));}
    @Transactional public boolean tao(long user,String type,String title,String body,String target,String key){
        if(type==null||!Set.of("DON_HANG","KHUYEN_MAI","GIAM_GIA","CO_HANG","IT_TUONG_TAC","PHAN_HOI_DANH_GIA").contains(type)||title==null||title.isBlank()||title.length()>160||body==null||body.isBlank()||body.length()>1000||key==null||key.isBlank()||key.length()>160||target!=null&&(target.length()>255||!dichAnToan(target)))throw new IllegalArgumentException("Thông báo không hợp lệ.");
        return dao.tao(user,type,title,body,target,key);
    }
    public List<Map<String,Object>> danhSach(String email,boolean unread,int page){if(page<1||page>10000)throw new IllegalArgumentException("Trang từ 1 đến 10000.");return dao.danhSach(users.hienTai(email).maNguoiDung(),unread,page);}
    public long chuaDoc(String email){return dao.chuaDoc(users.hienTai(email).maNguoiDung());}
    @Transactional public String doc(String email,long id){String path=dao.doc(users.hienTai(email).maNguoiDung(),id);return dichAnToan(path)?path:"/tai-khoan/thong-bao";}
    @Transactional public void docTatCa(String email){dao.docTatCa(users.hienTai(email).maNguoiDung());}
}
