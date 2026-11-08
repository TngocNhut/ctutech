package vn.luma.business;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.data.*;
import vn.luma.web.XacNhanHoanYeuCau;
import static vn.luma.data.ThanhToanDAO.id;

@Service
public class HoanTienService {
    private final HoanTienDAO dao;private final DonHangDAO orders;private final DonHangService orderService;private final TaiKhoanService users;private final ThanhToanDAO payments;private final HoaDonService invoices;private final KhoChungTu proofs;
    public HoanTienService(HoanTienDAO dao,DonHangDAO orders,DonHangService orderService,TaiKhoanService users,ThanhToanDAO payments,HoaDonService invoices,KhoChungTu proofs){this.dao=dao;this.orders=orders;this.orderService=orderService;this.users=users;this.payments=payments;this.invoices=invoices;this.proofs=proofs;}
    public List<Map<String,Object>> danhSach(String email){return danhSach(email,1);}
    public List<Map<String,Object>> danhSach(String email,int page){users.quanTri(email);if(page<1||page>10000)throw new IllegalArgumentException("Trang không hợp lệ.");return dao.danhSach(page);}
    public Map<String,Object> chiTiet(String email,long refund){users.quanTri(email);return dao.chiTiet(refund);}
    public List<Map<String,Object>> theoDon(String email,String code){return dao.theoDon(id(orderService.chiTiet(email,code),"MaDonHang"));}
    public byte[] chungTu(String email,long refund){var row=dao.chiTiet(refund);orderService.chiTiet(email,(String)row.get("MaDonCongKhai"));return proofs.doc((String)row.get("DuongDanChungTu"));}
    public String tenChungTu(String email,long refund){var row=dao.chiTiet(refund);orderService.chiTiet(email,(String)row.get("MaDonCongKhai"));String file=(String)row.get("DuongDanChungTu");if(file==null||!file.matches("[0-9a-f-]{36}\\.(pdf|png|jpg)"))throw new KhongTimThayException("Không có chứng từ.");return "ChungTuHoan-"+refund+file.substring(file.lastIndexOf('.'));}
    private Map<String,Object> khoaDon(String code){var row=orders.theoMa(code,false);if(row.get("MaGiamGia")!=null)orders.khoaVoucherId(id(row,"MaGiamGia"));return orders.theoMa(code,true);}
    private Map<String,Object> khoaHoan(Map<String,Object> initial){khoaDon((String)initial.get("MaDonCongKhai"));payments.khoaPhien(id(initial,"MaPhienThanhToan"));dao.khoaKhoanThu(id(initial,"MaGiaoDich"));return dao.khoa(id(initial,"MaHoanTien"));}
    @Transactional public void xacNhan(String email,long refund,XacNhanHoanYeuCau form,byte[] proof){
        long actor=users.quanTri(email).maNguoiDung();var initial=dao.chiTiet(refund);var current=khoaHoan(initial);
        if(form.soTien()==null||form.soTien().compareTo((BigDecimal)current.get("SoTienHoan"))!=0||!form.daKiemTra())throw new IllegalArgumentException("Số tiền hoàn phải khớp và cần xác nhận đã kiểm tra chứng từ.");
        if("THANH_CONG".equals(current.get("TrangThai"))){
            if(form.thamChieu().strip().equals(current.get("MaThamChieuHoan"))&&form.taiKhoan().strip().equals(current.get("TaiKhoanNhanHoan"))&&form.nganHang().strip().equals(current.get("NganHangNhanHoan"))&&form.nguoiNhan().strip().equals(current.get("TenNguoiNhanHoan")))return;
            throw new IllegalArgumentException("Khoản hoàn đã ghi thành công với chứng từ khác.");
        }
        if(!Set.of("CHO_XU_LY","DANG_XU_LY").contains((String)current.get("TrangThai")))throw new IllegalArgumentException("Cần đưa khoản hoàn về hàng chờ trước khi ghi lại.");
        if(payments.daGiuHoan(id(initial,"MaGiaoDich")).compareTo((BigDecimal)initial.get("TienDaThu"))>0)throw new IllegalStateException("Hạn mức hoàn vượt khoản thu; cần đối soát.");
        if(dao.trungThamChieu(form.thamChieu(),refund))throw new IllegalArgumentException("Mã chứng từ đã dùng cho một khoản hoàn khác.");
        String file=proofs.luu(proof);dao.hoanThanh(refund,actor,form,file);dao.chungTu(refund);
        long order=id(initial,"MaDonHang");BigDecimal total=dao.tongThuDon(order),returned=dao.tongHoanDon(order);
        var row=orders.theoMa((String)initial.get("MaDonCongKhai"),false);
        if(returned.compareTo(total)>=0)dao.trangThaiDon(order,"DA_HOAN");
        else if(Set.of("DA_HUY","HET_HAN").contains((String)row.get("TrangThaiDon"))&&dao.conChoHoan(order))dao.trangThaiDon(order,"CHO_HOAN");
        else if(total.subtract(returned).compareTo((BigDecimal)row.get("TongThanhToan"))<0)dao.trangThaiDon(order,"HOAN_MOT_PHAN");
        if(current.get("MaPhieuTraHang")!=null)dao.dongPhieuNeuDu(id(current,"MaPhieuTraHang"));
        orderService.ghiLichSu(order,actor,(String)row.get("TrangThaiDon"),(String)row.get("TrangThaiDon"),"Khoản hoàn #"+refund+" đã được ghi thành công theo chứng từ. Xem đơn để kiểm tra bằng chứng đối soát.");
        dao.log(actor,refund,"XAC_NHAN_HOAN_TIEN");
    }
    @Transactional public void danhDauLoi(String email,long refund,String reason){long actor=users.quanTri(email).maNguoiDung();var row=khoaHoan(dao.chiTiet(refund));if(reason==null||reason.isBlank()||reason.length()>500)throw new IllegalArgumentException("Cần nêu lý do, tối đa 500 ký tự.");if("THANH_CONG".equals(row.get("TrangThai")))throw new IllegalArgumentException("Không được đổi khoản đã hoàn thành sang thất bại.");dao.loi(refund,actor,reason.strip());}
    @Transactional public void thuLai(String email,long refund){long actor=users.quanTri(email).maNguoiDung();var row=khoaHoan(dao.chiTiet(refund));if(!"THAT_BAI".equals(row.get("TrangThai")))throw new IllegalArgumentException("Chỉ đưa khoản thất bại về hàng chờ.");dao.thuLai(refund,actor);}
    @Transactional public void chapNhanThuDu(String email,String code,boolean checked,String note){
        long actor=users.quanTri(email).maNguoiDung();var order=khoaDon(code);long oid=id(order,"MaDonHang");var session=payments.phienDon(oid,true).orElseThrow(()->new IllegalArgumentException("Không có phiên PayOS."));long sid=id(session,"MaPhienThanhToan");
        if(order.get("MaPhienThanhToanChapNhan")!=null)return;
        if(!checked||note==null||note.isBlank()||note.length()>500)throw new IllegalArgumentException("Cần kiểm tra khoản thu dư và nêu kết quả đối soát.");
        if(!Set.of("CHO_XAC_NHAN","DA_XAC_NHAN").contains((String)order.get("TrangThaiDon"))||!"DANG_GIU".equals(order.get("TrangThaiKho"))||orders.quaHan(oid))throw new IllegalArgumentException("Đơn không còn đủ điều kiện nhận thanh toán. Hãy xử lý khoản hoàn.");
        if(!"THU_DU".equals(session.get("TrangThaiXuLy"))||!"CAN_DOI_SOAT_THU_DU".equals(session.get("MaLoi")))throw new IllegalArgumentException("Chỉ duyệt khoản thu dư đã khớp tham chiếu. Xung đột cần kiểm tra riêng.");
        var receipts=payments.khoanThu(sid);BigDecimal total=payments.tongThu(sid),due=(BigDecimal)session.get("SoTienYeuCau");
        BigDecimal reserved=receipts.stream().map(r->payments.daGiuHoan(id(r,"MaGiaoDich"))).reduce(BigDecimal.ZERO,BigDecimal::add);
        if(total.compareTo(due)<=0||total.subtract(reserved).compareTo(due)!=0)throw new IllegalArgumentException("Cần giữ đúng phần dư để hoàn và bảo toàn đủ tiền hàng.");
        payments.chapNhan(oid,sid);invoices.phatHanh(oid);dao.duyetThuDu(sid,total,note.strip());
        orderService.ghiLichSu(oid,actor,(String)order.get("TrangThaiDon"),(String)order.get("TrangThaiDon"),"Cửa hàng đã đối soát và nhận đủ tiền hàng; phần dư được xử lý hoàn riêng");dao.log(actor,oid,"DUYET_THU_DU");
    }
}
