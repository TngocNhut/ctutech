package vn.luma.business;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.data.HoaDonDAO;
import static vn.luma.data.ThanhToanDAO.id;

@Service
public class HoaDonService {
    private final HoaDonDAO dao;private final DonHangService orders;private final HoaDonPdf pdf;
    public HoaDonService(HoaDonDAO dao,DonHangService orders,HoaDonPdf pdf){this.dao=dao;this.orders=orders;this.pdf=pdf;}
    @Transactional public void phatHanh(long order){
        var row=dao.khoaDon(order);if(dao.doc(order).isPresent())return;
        BigDecimal total=(BigDecimal)row.get("TongThanhToan");
        if(!"DA_THU".equals(row.get("TrangThaiThanhToan"))||row.get("MaPhienThanhToanChapNhan")==null||dao.tongThu(id(row,"MaPhienThanhToanChapNhan")).compareTo(total)<0)
            throw new IllegalStateException("Chỉ phát hành hóa đơn sau khi xác minh đủ tiền.");
        if(dao.tongDongDon(order).add((BigDecimal)row.get("PhiVanChuyen")).compareTo(total)!=0)throw new IllegalStateException("Tổng dòng hóa đơn không khớp đơn hàng.");
        dao.tao(order);
    }
    public byte[] tai(String email,String code){
        var order=orders.chiTiet(email,code);var invoice=dao.doc(id(order,"MaDonHang")).orElseThrow(()->new KhongTimThayException("Đơn chưa được phát hành hóa đơn."));
        return pdf.tao(invoice,dao.dong(id(invoice,"MaHoaDon")));
    }
    public boolean daPhatHanh(String email,String code){return dao.doc(id(orders.chiTiet(email,code),"MaDonHang")).isPresent();}
}
