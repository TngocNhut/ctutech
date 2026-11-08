package vn.luma.config;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import vn.luma.business.*;
import vn.luma.web.GioHangController;

public class DangNhapThanhCong extends SavedRequestAwareAuthenticationSuccessHandler {
    private final TaiKhoanService accounts;private final GioHangService carts;private final GioiHanDangNhapFilter rateLimit;
    public DangNhapThanhCong(TaiKhoanService accounts,GioHangService carts,GioiHanDangNhapFilter rateLimit){this.accounts=accounts;this.carts=carts;this.rateLimit=rateLimit;setDefaultTargetUrl("/tai-khoan");}
    @Override public void onAuthenticationSuccess(HttpServletRequest req,HttpServletResponse res,Authentication auth)throws IOException,ServletException{
        boolean remember="true".equals(req.getParameter("ghiNho"));
        var session=req.getSession();accounts.ghiPhien(auth.getName(),session.getId(),remember);rateLimit.thanhCong(auth.getName());
        if(remember){int duration=30*24*60*60;session.setMaxInactiveInterval(duration);String path=req.getContextPath().isEmpty()?"/":req.getContextPath();res.addHeader("Set-Cookie",org.springframework.http.ResponseCookie.from("JSESSIONID",session.getId()).path(path).maxAge(duration).httpOnly(true).secure(req.isSecure()).sameSite("Lax").build().toString());}
        synchronized(req.getSession()){session=req.getSession();var guest=GioHangController.guest(session);if(!guest.isEmpty()&&GioHangController.dungChu(session,auth.getName()))try{var result=carts.gop(auth.getName(),guest);GioHangController.luuConLai(session,auth.getName(),result.conLai());session.setAttribute(GioHangService.NOTICE_KEY,result.thongBao());}catch(RuntimeException ex){GioHangController.luuConLai(session,auth.getName(),guest);session.setAttribute(GioHangService.NOTICE_KEY,"Chưa gộp được lựa chọn trước đăng nhập. Mở giỏ hàng để kiểm tra và thử lại.");org.slf4j.LoggerFactory.getLogger(getClass()).warn("Giữ lựa chọn giỏ sau lỗi gộp: {}",ex.getClass().getSimpleName());}}
        if(req.getSession().getAttribute(LienKetOAuthService.PENDING) instanceof LienKetOAuthService.DanhTinh p&&p.conHan()&&!"GOOGLE".equals(p.nhaCungCap())){new org.springframework.security.web.savedrequest.HttpSessionRequestCache().removeRequest(req,res);res.sendRedirect("/tai-khoan/lien-ket");return;}super.onAuthenticationSuccess(req,res,auth);
    }
}
