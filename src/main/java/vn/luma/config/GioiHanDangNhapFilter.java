package vn.luma.config;

import java.io.IOException;
import java.time.*;
import java.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.luma.business.TaiKhoanService;

public class GioiHanDangNhapFilter extends OncePerRequestFilter {
    private static final Duration WINDOW=Duration.ofMinutes(15);
    private static final int MAX_KEYS=10000;
    private final Map<String,CuaSo> attempts=new HashMap<>();
    private final TaiKhoanService accounts;private final Clock clock;
    private Instant nextCleanup=Instant.MIN;private boolean capacityLogged;
    private static class CuaSo {int count;boolean logged;final Instant end;CuaSo(Instant end){this.end=end;}}
    record KetQua(boolean choPhep,boolean ghiLog,int thuLaiSau) { }
    public GioiHanDangNhapFilter(TaiKhoanService accounts){this(accounts,Clock.systemUTC());}
    GioiHanDangNhapFilter(TaiKhoanService accounts,Clock clock){this.accounts=accounts;this.clock=clock;}
    private String key(String type,String value){return type+TaiKhoanService.bam(value);}
    // ponytail: cửa sổ trong RAM cho một instance localhost; dùng shared store khi chạy nhiều instance.
    synchronized KetQua nhan(String address,String email){
        Instant now=clock.instant();
        if(!now.isBefore(nextCleanup)){attempts.values().removeIf(v->!now.isBefore(v.end));nextCleanup=now.plusSeconds(60);capacityLogged=false;}
        var keys=List.of(key("IP:",address),key("ACCOUNT:",email));
        for(String k:keys){var old=attempts.get(k);if(old!=null&&!now.isBefore(old.end))attempts.remove(k);}
        long needed=keys.stream().filter(k->!attempts.containsKey(k)).count();
        if(attempts.size()+needed>MAX_KEYS){boolean log=!capacityLogged;capacityLogged=true;return new KetQua(false,log,60);}
        var ip=attempts.computeIfAbsent(keys.getFirst(),k->new CuaSo(now.plus(WINDOW)));
        var account=attempts.computeIfAbsent(keys.getLast(),k->new CuaSo(now.plus(WINDOW)));
        var blocked=ip.count>=100?ip:account.count>=10?account:null;
        if(blocked!=null){boolean log=!blocked.logged;blocked.logged=true;return new KetQua(false,log,(int)Math.max(1,Duration.between(now,blocked.end).toSeconds()+1));}
        ip.count++;account.count++;return new KetQua(true,false,0);
    }
    public synchronized void thanhCong(String email){attempts.remove(key("ACCOUNT:",email));}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException {
        if("POST".equals(req.getMethod())&&"/login".equals(req.getServletPath())){
            String email=Objects.toString(req.getParameter("username"),"").strip().toLowerCase(Locale.ROOT);
            if(email.length()>254)email=email.substring(0,254);
            var result=nhan(req.getRemoteAddr(),email);
            if(!result.choPhep()){
                if(result.ghiLog())accounts.ghiDangNhapLoi(email,true);
                res.setHeader("Retry-After",Integer.toString(result.thuLaiSau()));res.sendError(429);return;
            }
        }
        chain.doFilter(req,res);
    }
}
