package vn.luma.business;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.data.*;
import vn.luma.model.SanPham;
import vn.luma.web.*;

@Service
public class DanhGiaService {
    private final ThongBaoService notifications;private final DanhGiaDAO dao;private final TaiKhoanService users;private final TaiKhoanDAO accounts;private final SanPhamService products;private final QuanLySanPhamDAO audit;
    public DanhGiaService(DanhGiaDAO dao,TaiKhoanService users,TaiKhoanDAO accounts,SanPhamService products,QuanLySanPhamDAO audit,ThongBaoService notifications){this.notifications=notifications;this.dao=dao;this.users=users;this.accounts=accounts;this.products=products;this.audit=audit;}
    private void trang(int page){if(page<1||page>10000)throw new IllegalArgumentException("Trang từ 1 đến 10000.");}
    public List<Map<String,Object>> daMua(String email,int page){trang(page);return dao.daMua(users.hienTai(email).maNguoiDung(),page);}
    public boolean coTheDanhGia(String email,long product){return dao.bangChung(users.hienTai(email).maNguoiDung(),product).isPresent();}
    public record BanSoan(String tenSanPham,Map<String,Object> danhGia) { }
    public BanSoan banSoan(String email,long product){long user=users.hienTai(email).maNguoiDung();var proof=dao.bangChung(user,product).orElseThrow(()->new KhongTimThayException("Chỉ đánh giá sản phẩm từ đơn đã hoàn thành và đã ghi thu."));return new BanSoan((String)proof.get("TenSanPhamLucDat"),dao.cuaToi(user,product).orElse(null));}
    @Transactional public void luu(String email,long product,DanhGiaYeuCau f){
        var user=users.hienTai(email);accounts.khoaNguoiDung(user.maNguoiDung());user=users.hienTai(email);
        if(!"HOAT_DONG".equals(user.trangThai()))throw new IllegalArgumentException("Cần tài khoản hoạt động để đánh giá.");
        if(f.soSao()<1||f.soSao()>5||f.noiDung()==null||f.noiDung().strip().length()<10||f.noiDung().strip().length()>2000||f.tieuDe()!=null&&f.tieuDe().length()>150)throw new IllegalArgumentException("Đánh giá cần 1–5 sao, nội dung 10–2000 ký tự, tiêu đề tối đa150 ký tự.");
        var proof=dao.bangChung(user.maNguoiDung(),product).orElseThrow(()->new IllegalArgumentException("Chỉ đánh giá sản phẩm từ đơn đã hoàn thành và đã ghi thu."));
        dao.luu(user.maNguoiDung(),product,ThanhToanDAO.id(proof,"MaChiTietDonHang"),f);
    }
    public record CongKhai(SanPham sanPham,Map<String,Object> tongHop,List<Map<String,Object>> danhGia,int trang) { }
    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public CongKhai congKhai(String slug,int page){trang(page);var product=products.chiTiet(slug);return new CongKhai(product,dao.tongHop(product.maSanPham()),dao.congKhai(product.maSanPham(),page,10),page);}
    private void boLoc(String state,String q,int page){trang(page);if(q==null||q.length()>100||!Set.of("TAT_CA","HIEN_THI","BI_AN").contains(state))throw new IllegalArgumentException("Bộ lọc đánh giá không hợp lệ.");}
    public List<Map<String,Object>> quanTri(String email,String state,String q,int page){users.quanTri(email);boLoc(state,q,page);return dao.quanTri(state,q.strip(),page);}
    public long dem(String email,String state,String q){users.quanTri(email);boLoc(state,q,1);return dao.dem(state,q.strip());}
    public Map<String,Object> chiTiet(String email,long id){users.quanTri(email);return dao.tim(id,false);}
    @Transactional public void duyet(String email,long id,KiemDuyetDanhGiaYeuCau f){
        long actor=users.quanTri(email).maNguoiDung();var previous=dao.tim(id,true);if(f.lyDo()==null||f.lyDo().isBlank()||f.lyDo().length()>255||f.phanHoi()!=null&&f.phanHoi().length()>2000)throw new IllegalArgumentException("Cần lý do 1–255 ký tự và phản hồi tối đa2000 ký tự.");
        dao.duyet(id,actor,f);
        String reply=f.phanHoi()==null?"":f.phanHoi().strip();
        if(!reply.isBlank()&&!reply.equals(previous.get("PhanHoiQuanTri"))){String excerpt=reply.length()>500?reply.substring(0,500)+"…":reply;notifications.tao(ThanhToanDAO.id(previous,"MaNguoiDung"),"PHAN_HOI_DANH_GIA","Bạn có phản hồi từ cửa hàng",previous.get("TenSanPham")+": "+excerpt,"/tai-khoan/danh-gia/"+previous.get("MaSanPham"),"review-reply:"+id+":"+TaiKhoanService.bam(reply));}
        audit.log(actor,id,f.hienThi()?"HIEN_THI_DANH_GIA":"AN_DANH_GIA","DANH_GIA",f.lyDo().strip());
    }
}
