package vn.luma.business;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.data.TaiKhoanDAO;
import vn.luma.model.TaiKhoan;
import vn.luma.web.DangKyYeuCau;

@Service
public class TaiKhoanService {
    private final TaiKhoanDAO dao;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher events;
    private final vn.luma.data.QuanLyNguoiDungDAO security;
    public TaiKhoanService(TaiKhoanDAO dao,PasswordEncoder encoder,ApplicationEventPublisher events,vn.luma.data.QuanLyNguoiDungDAO security) {
        this.dao=dao;this.encoder=encoder;this.events=events;this.security=security;
    }
    public record ThuThuNghiem(String email,String purpose,String token) { }
    public static String bam(String value) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
    public static String emailChuan(String email) {return email.strip().toLowerCase(Locale.ROOT);}
    public static String danhTinhChuan(String value) {String v=emailChuan(value);if(v.matches("0[0-9]{9}"))return "+84"+v.substring(1);return v;}
    public TaiKhoan hienTai(String email) {return dao.tim(danhTinhChuan(email)).orElseThrow(()->new IllegalArgumentException("Tài khoản không khả dụng"));}
    public TaiKhoan vanHanh(String email){var user=hienTai(email);if(!"HOAT_DONG".equals(user.trangThai())||!("QuanTri".equals(user.vaiTro())||"NhanVien".equals(user.vaiTro())))throw new org.springframework.security.access.AccessDeniedException("Không có quyền vận hành.");return user;}
    public TaiKhoan quanTri(String email){var user=hienTai(email);if(!"QuanTri".equals(user.vaiTro())||!"HOAT_DONG".equals(user.trangThai()))throw new org.springframework.security.access.AccessDeniedException("Không có quyền quản trị.");return user;}
    public void kiemTraMatKhau(String password) {
        if(password==null || password.length()<12 || password.length()>64 || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new IllegalArgumentException("Mật khẩu cần 12–64 ký tự và không quá 72 byte UTF-8.");
    }
    @Transactional
    public long dangKyTrucTiep(vn.luma.web.DangKyMoiYeuCau form) {
        kiemTraMatKhau(form.matKhau());
        if(!form.matKhau().equals(form.xacNhanMatKhau()))throw new IllegalArgumentException("Hai mật khẩu chưa khớp.");
        String email=emailChuan(form.taiKhoan());if(!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))throw new IllegalArgumentException("Nhập email hợp lệ.");String name=(form.ho().strip()+" "+form.ten().strip()).strip();
        if(name.length()>120)throw new IllegalArgumentException("Họ và tên tối đa 120 ký tự.");
        if(dao.tim(email).isPresent())throw new IllegalArgumentException("Không thể đăng ký bằng email này. Hãy đăng nhập hoặc đặt lại mật khẩu.");
        try{return dao.taoKhachChuaXacMinh(name,email,form.ngaySinh(),encoder.encode(form.matKhau()));}
        catch(DuplicateKeyException ex){throw new IllegalArgumentException("Không thể đăng ký bằng email này.");}
    }
    @Transactional
    public long taoNhanVienTrucTiep(DangKyYeuCau form) {
        kiemTraMatKhau(form.matKhau());if(!form.matKhau().equals(form.xacNhanMatKhau()))throw new IllegalArgumentException("Hai mật khẩu chưa khớp.");
        String email=emailChuan(form.email());if(dao.tim(email).isPresent())throw new IllegalArgumentException("Không thể đăng ký bằng email này.");
        try{return dao.tao(form.hoTen().strip(),email,encoder.encode(form.matKhau()),"NhanVien",false);}catch(DuplicateKeyException ex){throw new IllegalArgumentException("Không thể đăng ký bằng email này.");}
    }
    @Transactional
    public long dangKy(DangKyYeuCau form) {
        kiemTraMatKhau(form.matKhau());
        if(!form.matKhau().equals(form.xacNhanMatKhau())) throw new IllegalArgumentException("Hai mật khẩu chưa khớp.");
        String email=emailChuan(form.email());
        if(dao.tim(email).isPresent()) throw new IllegalArgumentException("Không thể đăng ký bằng email này. Bạn có thể đăng nhập hoặc đặt lại mật khẩu.");
        long id;
        try {id=dao.tao(form.hoTen().strip(),email,encoder.encode(form.matKhau()),"KhachHang",false);}
        catch(DuplicateKeyException ex){throw new IllegalArgumentException("Không thể đăng ký bằng email này.");}
        phatMa(id,email,"XAC_MINH_THU");
        return id;
    }
    @Transactional
    public void seedDemo(String email,String ten,String role,String password) {
        if(dao.tim(email).isEmpty()) dao.tao(ten,email,encoder.encode(password),role,true);
    }
    private void phatMa(long id,String email,String purpose) {
        dao.khoaNguoiDung(id);
        if(dao.soMaGanDay(id)>=5) return;
        byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        dao.capMa(id,purpose,bam(token));
        events.publishEvent(new ThuThuNghiem(email,purpose,token));
    }
    @Transactional
    public void yeuCauDatLai(String email) {
        dao.tim(emailChuan(email)).filter(t->t.trangThai().equals("HOAT_DONG"))
            .ifPresent(t->phatMa(t.maNguoiDung(),t.thuDienTu(),"DAT_LAI_MAT_KHAU"));
    }
    @Transactional
    public void guiLaiXacMinh(String email) {
        TaiKhoan user=hienTai(email);
        if(!user.daXacMinh()) phatMa(user.maNguoiDung(),user.thuDienTu(),"XAC_MINH_THU");
    }
    private long nhanMa(String token,String purpose) {
        if(token==null || token.length()!=43) throw new IllegalArgumentException("Liên kết không hợp lệ hoặc đã hết hạn.");
        String hash=bam(token);
        long id=dao.chuMa(hash,purpose).orElseThrow(()->new IllegalArgumentException("Liên kết không hợp lệ hoặc đã hết hạn."));
        dao.khoaNguoiDung(id);
        if(!dao.dungMa(hash,purpose)) throw new IllegalArgumentException("Liên kết đã dùng hoặc hết hạn. Vui lòng yêu cầu liên kết mới.");
        return id;
    }
    @Transactional
    public void xacMinh(String token) {long id=nhanMa(token,"XAC_MINH_THU");dao.xacMinh(id);security.baoMat(id,null,"XAC_THUC_TOKEN","THANH_CONG","XAC_MINH_THU");}
    @Transactional
    public void datLai(String token,String password,String confirm) {
        kiemTraMatKhau(password);
        if(!password.equals(confirm)) throw new IllegalArgumentException("Hai mật khẩu chưa khớp.");
        long id=nhanMa(token,"DAT_LAI_MAT_KHAU");dao.doiMatKhau(id,encoder.encode(password));security.baoMat(id,null,"XAC_THUC_TOKEN","THANH_CONG","DAT_LAI_MAT_KHAU");security.baoMat(id,null,"THU_HOI_PHIEN","THANH_CONG","DAT_LAI_MAT_KHAU");
    }
    @Transactional
    public void suaHoSo(String email,String ten,String phone) {
        var u=hienTai(email);if(u.thuDienTu()==null && !danhTinhChuan(phone).equals(u.soDienThoai()))throw new IllegalArgumentException("Không thể thay đổi số điện thoại đăng nhập tại đây.");dao.suaHoSo(u.maNguoiDung(),ten.strip(),u.thuDienTu()==null?u.soDienThoai():phone.strip());
    }
    @Transactional
    public void ghiPhien(String email,String sessionId) { ghiPhien(email,sessionId,false); }
    @Transactional
    public void ghiPhien(String email,String sessionId,boolean remember) {
        TaiKhoan user=hienTai(email);
        dao.khoaNguoiDung(user.maNguoiDung());
        user=hienTai(email);
        if(!user.trangThai().equals("HOAT_DONG")) throw new IllegalArgumentException("Tài khoản không khả dụng.");
        long session=dao.ghiPhien(user.maNguoiDung(),bam(sessionId),remember);security.baoMat(user.maNguoiDung(),session,"DANG_NHAP","THANH_CONG",null);
    }
    public boolean phienHopLe(String email,String sessionId) {return dao.phienHopLe(danhTinhChuan(email),bam(sessionId));}
    public void thuHoiPhien(String sessionId) {dao.thuHoiPhien(bam(sessionId));}
    public void ghiDangNhapLoi(String email,boolean blocked){Long id=email.length()>254?null:dao.tim(danhTinhChuan(email)).map(TaiKhoan::maNguoiDung).orElse(null);security.baoMat(id,null,"DANG_NHAP",blocked?"BI_CHAN":"THAT_BAI",blocked?"GIOI_HAN_TAN_SUAT":"THONG_TIN_KHONG_HOP_LE");}
}
