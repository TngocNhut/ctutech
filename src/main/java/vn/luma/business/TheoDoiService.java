package vn.luma.business;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.data.*;
import static vn.luma.data.ThanhToanDAO.id;

@Service
public class TheoDoiService {
    private final TheoDoiDAO dao;private final TaiKhoanService users;private final TaiKhoanDAO accounts;private final ThongBaoService notifications;
    public TheoDoiService(TheoDoiDAO dao,TaiKhoanService users,TaiKhoanDAO accounts,ThongBaoService notifications){this.dao=dao;this.users=users;this.accounts=accounts;this.notifications=notifications;}
    public Optional<Map<String,Object>> cuaToi(String email,long product){return dao.cuaToi(users.hienTai(email).maNguoiDung(),product);}
    public List<Map<String,Object>> danhSach(String email,int page){if(page<1||page>10000)throw new IllegalArgumentException("Trang từ 1 đến 10000.");long user=users.hienTai(email).maNguoiDung();return dao.danhSach(user,page).stream().map(r->{var row=new HashMap<>(r);row.put("hienTai",dao.tinhTrang(id(r,"MaSanPham")));return (Map<String,Object>)row;}).toList();}
    @Transactional public void luu(String email,long product,boolean price,boolean stock){luu(email,product,price,stock,null);}
    @Transactional public void luu(String email,long product,boolean price,boolean stock,Long selected){
        long user=users.hienTai(email).maNguoiDung();accounts.khoaNguoiDung(user);var current=dao.tinhTrang(product);var previous=dao.cuaToi(user,product);
        if(previous.isEmpty()&&current.get("gia")==null)throw new IllegalArgumentException("Sản phẩm chưa có cấu hình công khai để theo dõi.");
        if(previous.isEmpty()&&dao.dem(user)>=100)throw new IllegalArgumentException("Danh sách tối đa100 sản phẩm. Hãy bỏ sản phẩm không còn cần theo dõi.");
        if(selected!=null&&!dao.phienBanHopLe(product,selected))throw new IllegalArgumentException("Phiên bản đã chọn không thuộc sản phẩm hoặc không còn kinh doanh.");
        dao.luu(user,product,price,stock,current,selected);
    }
    @Transactional public void xoa(String email,long product){long user=users.hienTai(email).maNguoiDung();accounts.khoaNguoiDung(user);dao.xoa(user,product);}
    @Transactional public void kiemTraCuaToi(String email){long user=users.hienTai(email).maNguoiDung();accounts.khoaNguoiDung(user);for(long id:dao.cuaNguoiDung(user))kiemTraMot(id);}
    private String tien(BigDecimal value){return java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value);}
    public long mocCuoi(){return dao.mocCuoi();}
    public List<Long> tiepTheo(long after,long until){return dao.tiepTheo(after,until);}
    // ponytail: một transaction ngắn/dòng cho bản local; gom snapshot theo model nếu danh sách theo dõi lớn.
    @Transactional public void kiemTraMot(long follow){
        var optional=dao.khoa(follow);if(optional.isEmpty())return;var row=optional.get();if(!"HOAT_DONG".equals(row.get("tai_khoan")))return;
        var current=dao.tinhTrang(id(row,"MaSanPham"));var old=(BigDecimal)row.get("GiaThamChieu");var price=(BigDecimal)current.get("gia");
        boolean available=Boolean.TRUE.equals(current.get("co_hang")),before=Boolean.TRUE.equals(row.get("CoHangThamChieu"));
        boolean priceChanged=price!=null&&(old==null||old.compareTo(price)!=0);if(!priceChanged&&before==available)return;
        long phase=id(row,"PhienThongBao")+1,user=id(row,"MaNguoiDung");String title=(String)current.get("TenSanPham"),slug=(String)current.get("DuongDan");
        if(Boolean.TRUE.equals(row.get("NhanBaoGiamGia"))&&price!=null&&old!=null&&price.compareTo(old)<0)notifications.tao(user,"GIAM_GIA","Giá sản phẩm theo dõi đã giảm",title+": giá niêm yết cấu hình thấp nhất từ "+tien(old)+" xuống "+tien(price)+" VND tại lúc kiểm tra. Giá/tồn được kiểm tra lại khi đặt hàng.","/san-pham/"+slug+"?sku="+current.get("sku"),"follow-price:"+follow+":"+phase);
        if(Boolean.TRUE.equals(row.get("NhanBaoCoHang"))&&!before&&available)notifications.tao(user,"CO_HANG","Sản phẩm theo dõi đã có hàng",title+" có ít nhất một cấu hình còn số lượng khả dụng tại lúc kiểm tra. Thông báo không giữ hàng.","/san-pham/"+slug+"?sku="+current.get("sku_con"),"follow-stock:"+follow+":"+phase);
        dao.capNhat(follow,price==null?old:price,available,phase);
    }
}
