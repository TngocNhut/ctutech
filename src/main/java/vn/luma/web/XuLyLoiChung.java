package vn.luma.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.luma.business.KhongTimThayException;

@ControllerAdvice(annotations=Controller.class)
public class XuLyLoiChung {
    @ExceptionHandler(KhongTimThayException.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    String khongTimThay(KhongTimThayException ex,Model model){model.addAttribute("loi",ex.getMessage());return "error/404";}
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    String khongHopLe(IllegalArgumentException ex,jakarta.servlet.http.HttpServletRequest request){
        var origin=java.util.Arrays.stream(ex.getStackTrace()).filter(frame->frame.getClassName().startsWith("vn.luma.")).findFirst().orElse(null);
        org.slf4j.LoggerFactory.getLogger(XuLyLoiChung.class).warn("Yêu cầu không hợp lệ: path={}, origin={}",request.getRequestURI(),origin==null?"unknown":origin.getClassName()+":"+origin.getLineNumber());
        return "error/400";
    }
}
