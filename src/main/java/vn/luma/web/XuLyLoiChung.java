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
    String khongHopLe(){return "error/400";}
}
