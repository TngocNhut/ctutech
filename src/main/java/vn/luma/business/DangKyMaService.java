package vn.luma.business;
import java.time.*;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.luma.data.TaiKhoanDAO;
import vn.luma.web.DangKyMoiYeuCau;
@Service
public class DangKyMaService {
 public record ChoXacMinh(String hoTen,LocalDate ngaySinh,String destination,String channel,String passwordHash,Instant hetHan,int attempts,Instant guiLuc,int guiSoLan) implements java.io.Serializable {}
 private final TwilioVerifyClient verify;private final TaiKhoanDAO dao;private final TaiKhoanService accounts;private final PasswordEncoder encoder;
 public DangKyMaService(TwilioVerifyClient verify,TaiKhoanDAO dao,TaiKhoanService accounts,PasswordEncoder encoder){this.verify=verify;this.dao=dao;this.accounts=accounts;this.encoder=encoder;}
 public ChoXacMinh batDau(DangKyMoiYeuCau form){
  accounts.kiemTraMatKhau(form.matKhau());if(!form.matKhau().equals(form.xacNhanMatKhau()))throw new IllegalArgumentException("Hai mật khẩu chưa khớp.");
  String id=TaiKhoanService.danhTinhChuan(form.taiKhoan());String channel;
  if(id.matches("\\+84[0-9]{9}"))channel="sms";
  else if(id.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")&&id.length()<=254)channel="email";
  else throw new IllegalArgumentException("Nhập email hợp lệ hoặc số điện thoại Việt Nam gồm 10 số.");
  if(dao.tim(id).isPresent())throw new IllegalArgumentException("Không thể đăng ký bằng thông tin này. Hãy đăng nhập hoặc đặt lại mật khẩu.");
  if(!verify.available())throw new IllegalStateException("Dịch vụ gửi mã chưa được cấu hình. Vui lòng thử lại sau.");
  String name=(form.ho().strip()+" "+form.ten().strip()).strip();if(name.length()>120)throw new IllegalArgumentException("Họ và tên không được quá 120 ký tự.");String hash=encoder.encode(form.matKhau());
  verify.send(id,channel);var now=Instant.now();return new ChoXacMinh(name,form.ngaySinh(),id,channel,hash,now.plusSeconds(600),0,now,1);
 }
 public ChoXacMinh guiLai(ChoXacMinh pending){
  if(pending==null||Instant.now().isAfter(pending.hetHan()))throw new IllegalArgumentException("Phiên đăng ký đã hết hạn. Hãy đăng ký lại.");
  if(pending.guiSoLan()>=3||Instant.now().isBefore(pending.guiLuc().plusSeconds(60)))throw new IllegalArgumentException("Hãy chờ 60 giây giữa các lần gửi; tối đa 3 lần mỗi phiên.");
  verify.send(pending.destination(),pending.channel());return new ChoXacMinh(pending.hoTen(),pending.ngaySinh(),pending.destination(),pending.channel(),pending.passwordHash(),pending.hetHan(),pending.attempts(),Instant.now(),pending.guiSoLan()+1);
 }
 public ChoXacMinh thuSai(ChoXacMinh p){return new ChoXacMinh(p.hoTen(),p.ngaySinh(),p.destination(),p.channel(),p.passwordHash(),p.hetHan(),p.attempts()+1,p.guiLuc(),p.guiSoLan());}
 @Transactional public void xacNhan(ChoXacMinh p,String code){
  if(p==null||Instant.now().isAfter(p.hetHan())||p.attempts()>=5)throw new IllegalArgumentException("Phiên xác minh đã hết hạn. Hãy đăng ký lại.");
  if(!verify.check(p.destination(),code))throw new IllegalArgumentException("Mã xác nhận chưa đúng hoặc đã hết hạn.");
  if(dao.tim(p.destination()).isPresent())throw new IllegalArgumentException("Tài khoản đã tồn tại. Vui lòng đăng nhập.");
  try{dao.taoDaXacMinh(p.hoTen(),"email".equals(p.channel())?p.destination():null,"sms".equals(p.channel())?p.destination():null,p.ngaySinh(),p.passwordHash());}
  catch(DuplicateKeyException ex){throw new IllegalArgumentException("Tài khoản đã tồn tại. Vui lòng đăng nhập.");}
 }
}
