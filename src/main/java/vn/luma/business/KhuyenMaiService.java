package vn.luma.business;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import vn.luma.data.*;
import vn.luma.web.*;

@Service
public class KhuyenMaiService {
    private final KhuyenMaiDAO dao;private final TaiKhoanService users;private final QuanLySanPhamDAO log;private final DonHangDAO orders;private final ThongBaoService notifications;
    public KhuyenMaiService(KhuyenMaiDAO dao,TaiKhoanService users,QuanLySanPhamDAO log,DonHangDAO orders,ThongBaoService notifications){this.dao=dao;this.users=users;this.log=log;this.orders=orders;this.notifications=notifications;}
    public List<Map<String,Object>> danhSach(String email){return danhSach(email,1);}
    public List<Map<String,Object>> danhSach(String email,int page){users.quanTri(email);trang(page);return dao.chuongTrinh(page);}
    public Map<String,Object> chiTiet(String email,long id){users.quanTri(email);return dao.chuongTrinh(id,false);}
    public List<Map<String,Object>> quyTac(String email,long id){chiTiet(email,id);return dao.quyTac(id);}
    public List<Map<String,Object>> vouchers(String email){return vouchers(email,1);}
    public List<Map<String,Object>> vouchers(String email,int page){users.quanTri(email);trang(page);return dao.vouchers(false,page);}
    private void trang(int page){if(page<1||page>10000)throw new IllegalArgumentException("Trang không hợp lệ.");}
    public List<Map<String,Object>> congKhai(){return congKhai(1);}
    public List<Map<String,Object>> congKhai(int page){trang(page);return dao.vouchers(true,page);}
    public Map<String,Object> voucher(String email,long id){users.quanTri(email);return dao.voucher(id,false);}
    @Transactional public void luuThongBao(String email,long id){long user=users.hienTai(email).maNguoiDung();var offer=dao.voucherCongKhai(id).orElseThrow(()->new IllegalArgumentException("Ưu đãi không còn công khai hoặc đã hết hiệu lực."));notifications.tao(user,"KHUYEN_MAI","Ưu đãi đã lưu: "+offer.get("KyHieu"),offer.get("MoTa")+". Xem điều kiện hiện tại và nhập mã khi đặt đơn. Lưu ưu đãi không giữ lượt sử dụng.","/uu-dai","saved-offer:"+id);}
    private void thoiGian(java.time.LocalDateTime start,java.time.LocalDateTime end){if(start==null||end==null||!end.isAfter(start))throw new IllegalArgumentException("Thời điểm kết thúc phải sau thời điểm bắt đầu (giờ Việt Nam).");}
    private void giaTri(String type,BigDecimal value){if(value==null||value.signum()<=0||!("PHAN_TRAM".equals(type)||"SO_TIEN".equals(type))||("PHAN_TRAM".equals(type)?value.compareTo(BigDecimal.valueOf(100))>=0:value.stripTrailingZeros().scale()>0))throw new IllegalArgumentException("Phần trăm phải lớn hơn 0 và nhỏ hơn 100; mức giảm VND phải là số nguyên dương.");}
    @Transactional public long luu(String email,Long id,ChuongTrinhYeuCau f){
        long actor=users.quanTri(email).maNguoiDung();thoiGian(f.batDau(),f.ketThuc());
        if(id!=null){var old=dao.chuongTrinh(id,true);if(!old.get("KyHieu").equals(f.kyHieu()))throw new IllegalArgumentException("Ký hiệu chiến dịch được giữ cố định; hãy tạo chiến dịch mới khi cần đổi.");}
        try{long saved=dao.luu(id,f);log.log(actor,saved,"LUU_CHIEN_DICH","CHIEN_DICH_KHUYEN_MAI");return saved;}catch(DuplicateKeyException ex){throw new IllegalArgumentException("Ký hiệu chiến dịch đã được dùng.");}
    }
    @Transactional public void luuQuyTac(String email,long id,QuyTacKhuyenMaiYeuCau f){
        long actor=users.quanTri(email).maNguoiDung();dao.chuongTrinh(id,true);giaTri(f.kieu(),f.giaTri());
        var base=dao.giaGoc(f.sku());
        if(f.hoatDong()&&"SO_TIEN".equals(f.kieu())&&f.giaTri().compareTo(base)>=0)throw new IllegalArgumentException("Mức giảm phải nhỏ hơn giá bán gốc SKU.");
        dao.quyTac(id,f);log.log(actor,id,"LUU_QUY_TAC_KHUYEN_MAI","QUY_TAC_KHUYEN_MAI");
    }
    @Transactional public long luuVoucher(String email,Long id,VoucherYeuCau f){
        long actor=users.quanTri(email).maNguoiDung();thoiGian(f.batDau(),f.ketThuc());giaTri(f.kieu(),f.giaTri());
        if(f.tongLuot()<1||f.moiKhach()<1||f.moiKhach()>f.tongLuot()||f.donToiThieu()==null||f.donToiThieu().signum()<0)throw new IllegalArgumentException("Giới hạn lượt và đơn tối thiểu không hợp lệ.");
        if("PHAN_TRAM".equals(f.kieu())?(f.toiDa()==null||f.toiDa().signum()<=0):f.toiDa()!=null)throw new IllegalArgumentException("Mã phần trăm cần trần giảm dương; mã VND để trống trần giảm.");
        if(id!=null){var old=dao.voucher(id,true);if(!old.get("KyHieu").equals(f.kyHieu()))throw new IllegalArgumentException("Mã giảm giá được giữ cố định; hãy tạo mã mới khi cần đổi.");if(orders.luotVoucher(id,null)>f.tongLuot())throw new IllegalArgumentException("Tổng lượt mới không được thấp hơn số lượt đang giữ hoặc đã dùng.");}
        try{long saved=dao.luuVoucher(id,f);log.log(actor,saved,"LUU_MA_GIAM_GIA","MA_GIAM_GIA");return saved;}catch(DuplicateKeyException ex){throw new IllegalArgumentException("Mã giảm giá đã được dùng.");}
    }
}
