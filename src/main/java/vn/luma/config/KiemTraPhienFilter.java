package vn.luma.config;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.luma.business.TaiKhoanService;

public class KiemTraPhienFilter extends OncePerRequestFilter {
    private final TaiKhoanService service;
    public KiemTraPhienFilter(TaiKhoanService service) {this.service=service;}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            var session=req.getSession(false);
            boolean valid=session!=null && service.phienHopLe(auth.getName(),session.getId());
            if(valid) {
                String role="ROLE_"+service.hienTai(auth.getName()).vaiTro();
                valid=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals(role));
            }
            if(!valid) {
                if(session!=null) session.invalidate();
                SecurityContextHolder.clearContext();
                res.sendRedirect("/login?expired");return;
            }
        }
        chain.doFilter(req,res);
    }
}
