package vn.luma.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

// Multipart parsing may fail before a controller is selected, so this advice has no controller-type selector.
@ControllerAdvice
public class XuLyTepQuaLon {
    @ExceptionHandler(MaxUploadSizeExceededException.class) @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String hienThi(){return "error/413";}
}
