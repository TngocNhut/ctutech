package vn.luma.business;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import vn.luma.data.*;
import vn.luma.model.BienThe;
import vn.luma.web.CapNhatBienThe;

@Service
public class KhoService {
    private final KhoDAO dao;private final SanPhamDAO products;private final TaiKhoanService users;
    public KhoService(KhoDAO dao,SanPhamDAO products,TaiKhoanService users){this.dao=dao;this.products=products;this.users=users;}
    private long quanTri(String email){return users.vanHanh(email).maNguoiDung();}
    public List<BienThe> danhSach(String email){return danhSach(email,"",1);}
    public List<BienThe> danhSach(String email,String q,int page){quanTri(email);if(page<1||page>10000||q==null||q.length()>100)throw new IllegalArgumentException("Bộ lọc tồn kho không hợp lệ.");return products.bienTheGiaGoc(page,q.strip());}
    public BienThe sku(String email,long id){quanTri(email);return products.bienTheGiaGoc(id).orElseThrow(()->new KhongTimThayException("SKU không tồn tại."));}
    @Transactional public void capNhat(String email,long id,CapNhatBienThe form){
        long actor=quanTri(email);
        if(form.giaBan()==null||form.giaNiemYet()==null||form.giaVon()==null||form.giaBan().signum()<=0||form.giaVon().signum()<0||form.giaNiemYet().compareTo(form.giaBan())<0||form.soLuongTon()<0)
            throw new IllegalArgumentException("Giá niêm yết phải từ giá bán trở lên; giá bán dương, tồn và giá vốn không âm.");
        var old=dao.khoa(id);
        if(form.soLuongTon()<((Number)old.get("SoLuongGiu")).intValue())throw new IllegalArgumentException("Tồn vật lý không được thấp hơn số đang giữ cho đơn.");
        boolean changed=((BigDecimal)old.get("GiaBan")).compareTo(form.giaBan())!=0||((BigDecimal)old.get("GiaNiemYet")).compareTo(form.giaNiemYet())!=0;
        dao.ghi(id,form,actor,form.soLuongTon()-((Number)old.get("SoLuongTon")).intValue(),changed,"admin:"+UUID.randomUUID());
    }
}
