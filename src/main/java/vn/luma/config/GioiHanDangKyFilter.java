package vn.luma.config;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
/** Local single-instance limit, complementing the provider's verification limits. */
public class GioiHanDangKyFilter extends OncePerRequestFilter {
 private record Window(Instant end,int count){}
 private final Map<String,Window> windows=new HashMap<>();
 private synchronized boolean allow(String ip,String path){
  Instant now=Instant.now();windows.entrySet().removeIf(e->!now.isBefore(e.getValue().end()));
  String key=ip+"|"+path;Window current=windows.get(key);if(current==null){if(windows.size()>=10000)return false;current=new Window(now.plusSeconds(900),0);}
  int max=path.equals("/xac-nhan-dang-ky")?30:10;if(current.count()>=max)return false;
  windows.put(key,new Window(current.end(),current.count()+1));return true;
 }
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  String path=req.getServletPath();if("POST".equals(req.getMethod())&&(path.equals("/tao-tai-khoan")||path.equals("/xac-nhan-dang-ky")||path.equals("/xac-nhan-dang-ky/gui-lai"))&&!allow(req.getRemoteAddr(),path)){res.setStatus(429);res.setContentType("text/plain;charset=UTF-8");res.getWriter().write("Quá nhiều yêu cầu xác minh. Vui lòng thử lại sau.");return;}chain.doFilter(req,res);
 }
}
