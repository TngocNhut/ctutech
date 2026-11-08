package vn.luma.business;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import vn.luma.data.*;

@Service
public class DoiSoatService {
    private final DoiSoatDAO dao;private final TaiKhoanService users;private final ThanhToanService payments;private final QuanLySanPhamDAO audit;
    public DoiSoatService(DoiSoatDAO dao,TaiKhoanService users,ThanhToanService payments,QuanLySanPhamDAO audit){this.dao=dao;this.users=users;this.payments=payments;this.audit=audit;}
    private void trang(int page){if(page<1||page>10000)throw new IllegalArgumentException("Trang từ 1 đến 10000.");}
    private void loc(String state,String q,int page){trang(page);if(q==null||q.length()>100||!Set.of("CAN_KIEM_TRA","TAT_CA","MOI_TAO","CHO_THANH_TOAN","THU_THIEU","THU_DU","CAN_DOI_SOAT","DU_TIEN","HET_HAN","DA_HUY").contains(state))throw new IllegalArgumentException("Bộ lọc đối soát không hợp lệ.");}
    public List<Map<String,Object>> danhSach(String email,String state,String q,int page){users.quanTri(email);loc(state,q,page);return dao.danhSach(state,q.strip(),page);}
    public long dem(String email,String state,String q){users.quanTri(email);loc(state,q,1);return dao.dem(state,q.strip());}
    public record ChiTiet(Map<String,Object> phien,List<Map<String,Object>> khoanThu,List<Map<String,Object>> suKien,List<Map<String,Object>> lichSu,BigDecimal daThu,BigDecimal giuHoan,BigDecimal daHoan) { }
    @org.springframework.transaction.annotation.Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public ChiTiet chiTiet(String email,long id,int eventPage,int historyPage){
        users.quanTri(email);trang(eventPage);trang(historyPage);var session=dao.phien(id);var receipts=dao.thu(id);
        return new ChiTiet(session,receipts,dao.suKien(id,eventPage),dao.lichSu(id,historyPage),sum(receipts,"SoTien"),sum(receipts,"han_muc_hoan"),sum(receipts,"da_hoan"));
    }
    private BigDecimal sum(List<Map<String,Object>> rows,String key){return rows.stream().map(r->(BigDecimal)r.get(key)).reduce(BigDecimal.ZERO,BigDecimal::add);}
    public List<Map<String,Object>> chuaGan(String email,int page){users.quanTri(email);trang(page);return dao.chuaGan(page);}
    public void kiemTra(String email,long id){
        var actor=users.quanTri(email);var session=dao.phien(id);if(!"PAYOS".equals(session.get("NhaCungCap")))throw new IllegalArgumentException("COD được đối soát bằng chứng từ thu nội bộ tại chi tiết đơn.");
        payments.doiSoat(email,(String)session.get("MaDonCongKhai"));audit.log(actor.maNguoiDung(),id,"TRA_CUU_DOI_SOAT","PHIEN_THANH_TOAN");
    }
    @org.springframework.transaction.annotation.Transactional
    public void ghiChu(String email,long id,String note){var actor=users.quanTri(email);dao.phien(id);if(note==null||note.isBlank()||note.length()>500)throw new IllegalArgumentException("Ghi chú cần 1–500 ký tự.");dao.ghiChu(id,actor.maNguoiDung(),note.strip());audit.log(actor.maNguoiDung(),id,"GHI_CHU_DOI_SOAT","PHIEN_THANH_TOAN");}
}
