package vn.luma.business;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.luma.data.*;
import vn.luma.model.TaiKhoan;

@Service
public class LienKetOAuthService {
    public static final String PENDING="danhTinhOAuthChoLienKet";
    public record DanhTinh(String nhaCungCap,String chuThe,String email,Instant hetHan){
        public DanhTinh{if(!Set.of("GOOGLE","FACEBOOK").contains(nhaCungCap)||chuThe==null||chuThe.isBlank()||chuThe.length()>255||hetHan==null)throw new IllegalArgumentException("Danh tính OAuth không hợp lệ.");email=email==null?"":email.substring(0,Math.min(email.length(),254));}
        public boolean conHan(){return Instant.now().isBefore(hetHan);}
    }
    private final LienKetOAuthDAO dao;private final TaiKhoanDAO accounts;private final TaiKhoanService users;private final PasswordEncoder encoder;private final QuanLySanPhamDAO audit;
    public LienKetOAuthService(LienKetOAuthDAO dao,TaiKhoanDAO accounts,TaiKhoanService users,PasswordEncoder encoder,QuanLySanPhamDAO audit){this.dao=dao;this.accounts=accounts;this.users=users;this.encoder=encoder;this.audit=audit;}
    public Optional<TaiKhoan> dangNhap(DanhTinh identity){if(!identity.conHan())throw new IllegalArgumentException("Xác thực đã hết hạn.");return dao.email(identity.nhaCungCap(),identity.chuThe()).map(users::hienTai).map(u->{if(!u.trangThai().equals("HOAT_DONG"))throw new org.springframework.security.access.AccessDeniedException("Tài khoản không khả dụng.");return u;});}
    @Transactional public Optional<TaiKhoan> taoGoogleMoi(DanhTinh identity,String ten,boolean emailVerified){
        if(!"GOOGLE".equals(identity.nhaCungCap())||!identity.conHan()||!emailVerified) return Optional.empty();
        String email=TaiKhoanService.emailChuan(identity.email());
        if(!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")||email.length()>254)return Optional.empty();
        dao.khoa();var linked=dao.email("GOOGLE",identity.chuThe());if(linked.isPresent())return Optional.of(users.hienTai(linked.get()));
        if(accounts.tim(email).isPresent())return Optional.empty();
        String name=(ten==null||ten.isBlank()?email.substring(0,email.indexOf('@')):ten.strip());if(name.length()>120)name=name.substring(0,120);
        long id=accounts.tao(name,email,null,"KhachHang",true);dao.gan(id,"GOOGLE",identity.chuThe());audit.log(id,id,"TAO_TAI_KHOAN_GOOGLE","NGUOI_DUNG");return Optional.of(users.hienTai(email));
    }
    public List<Map<String,Object>> cuaToi(String email){return dao.cuaToi(users.hienTai(email).maNguoiDung());}
    private TaiKhoan kiemTra(String email,String password){var u=users.hienTai(email);if(!u.trangThai().equals("HOAT_DONG")||!u.daXacMinh())throw new IllegalArgumentException("Xác minh email tài khoản cửa hàng trước khi thay đổi liên kết.");if(password==null||password.length()>64||!dao.matKhau(u.maNguoiDung(),password,encoder))throw new IllegalArgumentException("Mật khẩu xác nhận chưa đúng.");return u;}
    @Transactional public void gan(String email,String password,DanhTinh identity){if(identity==null||!identity.conHan())throw new IllegalArgumentException("Xác thực nhà cung cấp đã hết hạn. Hãy thực hiện lại.");if("GOOGLE".equals(identity.nhaCungCap()))throw new IllegalArgumentException("Liên kết Google hiện không khả dụng.");dao.khoa();var u=kiemTra(email,password);accounts.khoaNguoiDung(u.maNguoiDung());u=kiemTra(email,password);var owner=dao.email(identity.nhaCungCap(),identity.chuThe());if(owner.isPresent()){if(owner.get().equals(u.thuDienTu()!=null?u.thuDienTu():u.soDienThoai()))return;throw new IllegalArgumentException("Danh tính này đã liên kết với tài khoản khác.");}if(dao.cuaToi(u.maNguoiDung()).stream().anyMatch(l->l.get("NhaCungCap").equals(identity.nhaCungCap())))throw new IllegalArgumentException("Tài khoản đã có liên kết với nhà cung cấp này; bỏ liên kết cũ trước.");dao.gan(u.maNguoiDung(),identity.nhaCungCap(),identity.chuThe());audit.log(u.maNguoiDung(),u.maNguoiDung(),"LIEN_KET_OAUTH","NGUOI_DUNG");}
    @Transactional public void bo(String email,String password,String provider){if(!Set.of("GOOGLE","FACEBOOK").contains(provider))throw new IllegalArgumentException("Nhà cung cấp không hợp lệ.");dao.khoa();var u=kiemTra(email,password);accounts.khoaNguoiDung(u.maNguoiDung());kiemTra(email,password);dao.bo(u.maNguoiDung(),provider);audit.log(u.maNguoiDung(),u.maNguoiDung(),"BO_LIEN_KET_OAUTH","NGUOI_DUNG");}
}
