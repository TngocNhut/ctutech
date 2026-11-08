package vn.luma.config;
import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
/** Temporarily disable Google before Spring Security's OAuth redirect filter runs. */
public class GoogleTamTatFilter extends OncePerRequestFilter {
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  String path=request.getServletPath();
  if(path.equals("/oauth2/authorization/google")||path.equals("/login/oauth2/code/google")){response.sendError(404);return;}
  chain.doFilter(request,response);
 }
}
