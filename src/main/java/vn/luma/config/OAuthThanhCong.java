package vn.luma.config;
import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import vn.luma.business.*;

public class OAuthThanhCong implements AuthenticationSuccessHandler {
    private final LienKetOAuthService identities;private final TaiKhoanService users;private final DangNhapThanhCong local;private final OAuth2AuthorizedClientService clients;
    public OAuthThanhCong(LienKetOAuthService identities,TaiKhoanService users,DangNhapThanhCong local,OAuth2AuthorizedClientService clients){this.identities=identities;this.users=users;this.local=local;this.clients=clients;}
    @Override public void onAuthenticationSuccess(HttpServletRequest req,HttpServletResponse res,Authentication authentication)throws IOException,ServletException{
        var oauth=(OAuth2AuthenticationToken)authentication;String provider=oauth.getAuthorizedClientRegistrationId();var context=SecurityContextHolder.createEmptyContext();var repository=new HttpSessionSecurityContextRepository();
        try{
            if(req.getRequestedSessionId()!=null)users.thuHoiPhien(req.getRequestedSessionId());
            String subject=oauth.getPrincipal().getAttribute(provider.equals("google")?"sub":"id");String email=oauth.getPrincipal().getAttribute("email");var identity=new LienKetOAuthService.DanhTinh(provider.toUpperCase(Locale.ROOT),subject,email,Instant.now().plusSeconds(300));var account=identities.dangNhap(identity);
            if(account.isEmpty() && provider.equals("google")) account=identities.taoGoogleMoi(identity,oauth.getPrincipal().getAttribute("name"),Boolean.TRUE.equals(oauth.getPrincipal().getAttribute("email_verified")));
            req.getSession().removeAttribute(LienKetOAuthService.PENDING);
            if(account.isEmpty()){req.getSession().setAttribute(LienKetOAuthService.PENDING,identity);SecurityContextHolder.setContext(context);repository.saveContext(context,req,res);new org.springframework.security.web.savedrequest.HttpSessionRequestCache().removeRequest(req,res);res.sendRedirect("/login?oauthPending");return;}
            var u=account.get();var principal=User.withUsername(u.thuDienTu()!=null?u.thuDienTu():u.soDienThoai()).password("").roles(u.vaiTro()).build();var auth=UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities());context.setAuthentication(auth);SecurityContextHolder.setContext(context);repository.saveContext(context,req,res);local.onAuthenticationSuccess(req,res,auth);
        }catch(IllegalArgumentException|org.springframework.security.access.AccessDeniedException ex){req.getSession().removeAttribute(LienKetOAuthService.PENDING);SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());repository.saveContext(SecurityContextHolder.getContext(),req,res);res.sendRedirect("/login?oauthError");}
        finally{if(clients!=null)clients.removeAuthorizedClient(provider,oauth.getName());}
    }
}
